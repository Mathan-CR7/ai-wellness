import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { InactivitySuggestionMessage, ActivityUpdateMessage } from '../types';

type MessageCallback<T> = (data: T) => void;

interface PendingSubscription {
  topic: string;
  callback: MessageCallback<any>;
}

class WebSocketManager {
  private client: Client | null = null;
  private isConnected = false;
  private subscriptions: Map<string, StompSubscription> = new Map();
  // Queue subscriptions requested before connection is established
  private pendingSubscriptions: PendingSubscription[] = [];

  private getSocketUrl(): string {
    if (import.meta.env.VITE_WS_URL) {
      return import.meta.env.VITE_WS_URL;
    }
    if (typeof window !== 'undefined') {
      const loc = window.location;
      if (loc.hostname === 'localhost' || loc.hostname === '127.0.0.1') {
        // In development Vite proxies /ws → Render backend
        return `${loc.protocol}//${loc.host}/ws`;
      }
    }
    return 'https://ai-wellness-jt1d.onrender.com/ws';
  }

  public connect(onConnected?: () => void, onError?: (err: any) => void) {
    if (this.client && this.client.active) {
      // Already connecting/connected — if already connected, flush pending subscriptions
      if (this.isConnected) {
        this.flushPendingSubscriptions();
        if (onConnected) onConnected();
      }
      return;
    }

    const socketUrl = this.getSocketUrl();

    this.client = new Client({
      webSocketFactory: () => new SockJS(socketUrl),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.isConnected = true;
        console.log('[STOMP] Connected to WebSocket broker at', socketUrl);
        // Flush all subscriptions that were requested before connection was ready
        this.flushPendingSubscriptions();
        if (onConnected) onConnected();
      },
      onStompError: (frame) => {
        console.error('[STOMP] Error:', frame.headers['message']);
        if (onError) onError(frame);
      },
      onWebSocketClose: () => {
        this.isConnected = false;
        // Clear active subscriptions — they will be re-created on reconnect via pendingSubscriptions
        this.subscriptions.clear();
        console.log('[STOMP] Connection closed');
      },
      onDisconnect: () => {
        this.isConnected = false;
        this.subscriptions.clear();
      },
    });

    this.client.activate();
  }

  /** Flush all pending subscriptions once connected */
  private flushPendingSubscriptions() {
    if (!this.client || !this.client.connected) return;
    const pending = [...this.pendingSubscriptions];
    this.pendingSubscriptions = [];
    for (const { topic, callback } of pending) {
      this.doSubscribe(topic, callback);
    }
  }

  /** Actually register a STOMP subscription — must be called when connected */
  private doSubscribe<T>(topic: string, callback: MessageCallback<T>) {
    if (!this.client || !this.client.connected) return;
    // Avoid duplicate subscriptions
    if (this.subscriptions.has(topic)) return;
    const sub = this.client.subscribe(topic, (message) => {
      try {
        const data: T = JSON.parse(message.body);
        callback(data);
      } catch (e) {
        console.error('[STOMP] Failed to parse message body for topic', topic, e);
      }
    });
    this.subscriptions.set(topic, sub);
    console.log('[STOMP] Subscribed to topic:', topic);
  }

  public subscribeToUserActivity(userId: number, callback: MessageCallback<ActivityUpdateMessage>): () => void {
    return this.subscribe<ActivityUpdateMessage>(`/topic/users/${userId}/activity`, callback);
  }

  public subscribeToGlobalActivity(callback: MessageCallback<ActivityUpdateMessage>): () => void {
    return this.subscribe<ActivityUpdateMessage>('/topic/activity', callback);
  }

  public subscribeToInactivitySuggestions(callback: MessageCallback<InactivitySuggestionMessage>): () => void {
    return this.subscribe<InactivitySuggestionMessage>('/topic/inactivity-suggestions', callback);
  }

  public subscribeToUserInactivity(userId: number, callback: MessageCallback<InactivitySuggestionMessage>): () => void {
    return this.subscribe<InactivitySuggestionMessage>(`/topic/users/${userId}/inactivity-suggestion`, callback);
  }

  public subscribe<T>(topic: string, callback: MessageCallback<T>): () => void {
    if (!this.client) {
      this.connect();
    }

    if (this.isConnected && this.client?.connected) {
      // Already connected — subscribe immediately
      this.doSubscribe(topic, callback);
    } else {
      // Queue for when connection is established / re-established
      // Remove any existing pending entry for same topic first
      this.pendingSubscriptions = this.pendingSubscriptions.filter(p => p.topic !== topic);
      this.pendingSubscriptions.push({ topic, callback });
      console.log('[STOMP] Queued subscription for topic (waiting for connection):', topic);
    }

    // Return unsubscribe function
    return () => {
      const sub = this.subscriptions.get(topic);
      if (sub) {
        sub.unsubscribe();
        this.subscriptions.delete(topic);
      }
      this.pendingSubscriptions = this.pendingSubscriptions.filter(p => p.topic !== topic);
    };
  }

  public disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.isConnected = false;
      this.subscriptions.clear();
      this.pendingSubscriptions = [];
      console.log('[STOMP] Client deactivated');
    }
  }

  public getIsConnected(): boolean {
    return this.isConnected;
  }
}

export const wsManager = new WebSocketManager();

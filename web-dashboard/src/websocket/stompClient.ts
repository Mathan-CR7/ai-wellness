import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { InactivitySuggestionMessage, ActivityUpdateMessage } from '../types';

type MessageCallback<T> = (data: T) => void;

class WebSocketManager {
  private client: Client | null = null;
  private isConnected = false;
  private subscriptions: Map<string, StompSubscription> = new Map();

  private getSocketUrl(): string {
    if (import.meta.env.VITE_WS_URL) {
      return import.meta.env.VITE_WS_URL;
    }
    if (typeof window !== 'undefined') {
      const loc = window.location;
      if (loc.hostname === 'localhost' || loc.hostname === '127.0.0.1') {
        return `${loc.protocol}//${loc.host}/ws`;
      }
    }
    return 'https://ai-wellness-jt1d.onrender.com/ws';
  }

  public connect(onConnected?: () => void, onError?: (err: any) => void) {
    if (this.client && this.client.active) {
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
        if (onConnected) onConnected();
      },
      onStompError: (frame) => {
        console.error('[STOMP] Error:', frame.headers['message']);
        if (onError) onError(frame);
      },
      onWebSocketClose: () => {
        this.isConnected = false;
        console.log('[STOMP] Connection closed');
      },
    });

    this.client.activate();
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

    const doSubscribe = () => {
      if (this.client && this.client.connected) {
        const sub = this.client.subscribe(topic, (message) => {
          try {
            const data: T = JSON.parse(message.body);
            callback(data);
          } catch (e) {
            console.error('[STOMP] Failed to parse message body', e);
          }
        });
        this.subscriptions.set(topic, sub);
      }
    };

    if (this.isConnected && this.client?.connected) {
      doSubscribe();
    } else {
      setTimeout(() => {
        if (this.client?.connected) {
          doSubscribe();
        }
      }, 1000);
    }

    return () => {
      const sub = this.subscriptions.get(topic);
      if (sub) {
        sub.unsubscribe();
        this.subscriptions.delete(topic);
      }
    };
  }

  public disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.isConnected = false;
      this.subscriptions.clear();
      console.log('[STOMP] Client deactivated');
    }
  }

  public getIsConnected(): boolean {
    return this.isConnected;
  }
}

export const wsManager = new WebSocketManager();

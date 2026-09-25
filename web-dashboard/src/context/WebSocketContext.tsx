import React, { createContext, useContext, useEffect, useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { wsManager } from '../websocket/stompClient';
import { InactivitySuggestionMessage, ActivityUpdateMessage, NotificationItem } from '../types';
import { useAuth } from './AuthContext';

interface WebSocketContextType {
  isConnected: boolean;
  notifications: NotificationItem[];
  unreadCount: number;
  latestAiSuggestion: InactivitySuggestionMessage | null;
  latestActivityUpdate: ActivityUpdateMessage | null;
  markAsRead: (id: string) => void;
  clearAll: () => void;
  dismissAiSuggestion: () => void;
  simulateInactivityDemo: () => void;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

export const WebSocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  const [isConnected, setIsConnected] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    const saved = localStorage.getItem('wellness_notifications');
    return saved ? JSON.parse(saved) : [];
  });
  const [latestAiSuggestion, setLatestAiSuggestion] = useState<InactivitySuggestionMessage | null>(null);
  const [latestActivityUpdate, setLatestActivityUpdate] = useState<ActivityUpdateMessage | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !user) return;

    wsManager.connect(
      () => setIsConnected(true),
      () => setIsConnected(false)
    );

    const handleActivityUpdate = (data: ActivityUpdateMessage) => {
      console.log('[STOMP] Received real-time activity update:', data);
      // Only accept updates for the authenticated user
      if (data.userId !== undefined && data.userId !== user.id) return;
      setLatestActivityUpdate(data);
      // Invalidate react-query cache so REST queries refetch immediately
      queryClient.invalidateQueries({ queryKey: ['todaySteps'] });
      queryClient.invalidateQueries({ queryKey: ['activitySummaryToday'] });
      queryClient.invalidateQueries({ queryKey: ['activeChallenges'] });
      queryClient.invalidateQueries({ queryKey: ['teamLeaderboard'] });
    };

    // Real-time User Activity STOMP updates (user-specific topic)
    const unsubscribeUserActivity = wsManager.subscribeToUserActivity(user.id, handleActivityUpdate);

    // Global activity topic fallback (covers cases where user-specific topic is missed)
    const unsubscribeGlobalActivity = wsManager.subscribeToGlobalActivity(handleActivityUpdate);

    // Global Inactivity Suggestions
    const unsubscribeGlobal = wsManager.subscribeToInactivitySuggestions((data) => {
      setLatestAiSuggestion(data);
      const newNotif: NotificationItem = {
        id: `inactivity-${Date.now()}`,
        type: 'inactivity',
        title: 'Time for a movement break 🌱',
        message: data.suggestion,
        timestamp: new Date().toISOString(),
        read: false,
      };
      setNotifications((prev) => [newNotif, ...prev]);
    });

    // User-specific Inactivity Suggestions
    const unsubscribeUserInactivity = wsManager.subscribeToUserInactivity(user.id, (data) => {
      setLatestAiSuggestion(data);
      const newNotif: NotificationItem = {
        id: `inactivity-user-${Date.now()}`,
        type: 'inactivity',
        title: 'Personalized Inactivity Alert ⚡',
        message: data.suggestion,
        timestamp: new Date().toISOString(),
        read: false,
      };
      setNotifications((prev) => [newNotif, ...prev]);
    });

    return () => {
      unsubscribeUserActivity();
      unsubscribeGlobalActivity();
      unsubscribeGlobal();
      unsubscribeUserInactivity();
      wsManager.disconnect();
    };
  }, [isAuthenticated, user?.id, queryClient]);

  useEffect(() => {
    localStorage.setItem('wellness_notifications', JSON.stringify(notifications));
  }, [notifications]);

  const markAsRead = (id: string) => {
    setNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
  };

  const clearAll = () => setNotifications([]);

  const dismissAiSuggestion = () => setLatestAiSuggestion(null);

  const simulateInactivityDemo = () => {
    const demoData: InactivitySuggestionMessage = {
      userId: user?.id || 1,
      userEmail: user?.email || 'user@wellness.app',
      suggestion: "Spring AI Alert: You've been sitting inactive for 2 hours and 15 minutes! Stand up, walk for 5 minutes, and perform light shoulder and neck stretches.",
      inactivityMinutes: 135,
      currentSteps: 3902,
      timestamp: new Date().toISOString(),
    };
    setLatestAiSuggestion(demoData);

    const newNotif: NotificationItem = {
      id: `inactivity-demo-${Date.now()}`,
      type: 'inactivity',
      title: 'Spring AI Movement Alert (Demo)',
      message: demoData.suggestion,
      timestamp: new Date().toISOString(),
      read: false,
    };
    setNotifications((prev) => [newNotif, ...prev]);
  };

  const unreadCount = notifications.filter((n) => !n.read).length;

  return (
    <WebSocketContext.Provider
      value={{
        isConnected,
        notifications,
        unreadCount,
        latestAiSuggestion,
        latestActivityUpdate,
        markAsRead,
        clearAll,
        dismissAiSuggestion,
        simulateInactivityDemo,
      }}
    >
      {children}
    </WebSocketContext.Provider>
  );
};

export const useWebSocket = () => {
  const context = useContext(WebSocketContext);
  if (!context) {
    throw new Error('useWebSocket must be used within a WebSocketProvider');
  }
  return context;
};

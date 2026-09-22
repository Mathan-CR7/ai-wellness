import React, { createContext, useContext, useEffect, useState } from 'react';
import { wsManager } from '../websocket/stompClient';
import { InactivitySuggestionMessage, NotificationItem } from '../types';
import { useAuth } from './AuthContext';

interface WebSocketContextType {
  isConnected: boolean;
  notifications: NotificationItem[];
  unreadCount: number;
  latestAiSuggestion: InactivitySuggestionMessage | null;
  markAsRead: (id: string) => void;
  clearAll: () => void;
  dismissAiSuggestion: () => void;
  simulateInactivityDemo: () => void;
}

const WebSocketContext = createContext<WebSocketContextType | undefined>(undefined);

export const WebSocketProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, isAuthenticated } = useAuth();
  const [isConnected, setIsConnected] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => {
    const saved = localStorage.getItem('wellness_notifications');
    return saved ? JSON.parse(saved) : [];
  });
  const [latestAiSuggestion, setLatestAiSuggestion] = useState<InactivitySuggestionMessage | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !user) return;

    wsManager.connect(() => setIsConnected(true), () => setIsConnected(false));

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
    const unsubscribeUser = wsManager.subscribeToUserInactivity(user.id, (data) => {
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
      unsubscribeGlobal();
      unsubscribeUser();
      wsManager.disconnect();
    };
  }, [isAuthenticated, user?.id]);

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

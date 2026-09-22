import React from 'react';
import { X, Bell, CheckCircle2, Sparkles, AlertTriangle, Trophy, Flame } from 'lucide-react';
import { useWebSocket } from '../../context/WebSocketContext';
import { Card } from '../ui/Card';

interface NotificationDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

export const NotificationDrawer: React.FC<NotificationDrawerProps> = ({ isOpen, onClose }) => {
  const { notifications, markAsRead, clearAll } = useWebSocket();

  if (!isOpen) return null;

  const getIcon = (type: string) => {
    switch (type) {
      case 'inactivity':
        return <Sparkles className="w-5 h-5 text-amber-500" />;
      case 'ai_recommendation':
        return <Sparkles className="w-5 h-5 text-purple-500" />;
      case 'challenge':
        return <Trophy className="w-5 h-5 text-emerald-500" />;
      case 'leaderboard':
        return <Flame className="w-5 h-5 text-orange-500" />;
      default:
        return <Bell className="w-5 h-5 text-brand-500" />;
    }
  };

  return (
    <div className="fixed inset-0 z-50 overflow-hidden bg-slate-950/40 backdrop-blur-xs">
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-md bg-white dark:bg-slate-900 border-l border-slate-200 dark:border-slate-800 shadow-2xl flex flex-col">
          {/* Header */}
          <div className="p-4 border-b border-slate-100 dark:border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <Bell className="w-5 h-5 text-brand-600 dark:text-brand-400" />
              <h3 className="font-bold text-base text-slate-900 dark:text-slate-100">Notifications</h3>
            </div>
            <div className="flex items-center space-x-2">
              {notifications.length > 0 && (
                <button
                  onClick={clearAll}
                  className="text-xs font-semibold text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                >
                  Clear All
                </button>
              )}
              <button
                onClick={onClose}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-3">
            {notifications.length === 0 ? (
              <div className="text-center py-16">
                <Bell className="w-12 h-12 text-slate-300 dark:text-slate-700 mx-auto mb-3 opacity-60" />
                <p className="font-semibold text-slate-600 dark:text-slate-400">You're all caught up!</p>
                <p className="text-xs text-slate-400 dark:text-slate-500 mt-1">
                  Real-time alerts and AI suggestions will appear here.
                </p>
              </div>
            ) : (
              notifications.map((n) => (
                <Card
                  key={n.id}
                  onClick={() => markAsRead(n.id)}
                  className={`p-3.5 cursor-pointer border transition-all ${
                    n.read
                      ? 'bg-slate-50/50 dark:bg-slate-900/50 border-slate-100 dark:border-slate-800/60 opacity-75'
                      : 'bg-white dark:bg-slate-800/90 border-brand-500/30 shadow-sm'
                  }`}
                >
                  <div className="flex items-start space-x-3">
                    <div className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800">{getIcon(n.type)}</div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <p className="font-bold text-xs text-slate-900 dark:text-slate-100 truncate">{n.title}</p>
                        {!n.read && <span className="w-2 h-2 rounded-full bg-brand-500 flex-shrink-0" />}
                      </div>
                      <p className="text-xs text-slate-600 dark:text-slate-300 mt-1 line-clamp-2">{n.message}</p>
                      <span className="text-[10px] text-slate-400 mt-1 block">
                        {new Date(n.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  </div>
                </Card>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

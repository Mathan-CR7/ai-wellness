import React, { useState } from 'react';
import { Bell, Sun, Moon, LogOut, User, Sparkles, Wifi } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { useWebSocket } from '../../context/WebSocketContext';
import { NotificationDrawer } from './NotificationDrawer';

export const TopNav: React.FC = () => {
  const { user, logout } = useAuth();
  const { theme, setTheme, isDark } = useTheme();
  const { isConnected, unreadCount } = useWebSocket();
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const toggleTheme = () => {
    setTheme(isDark ? 'light' : 'dark');
  };

  return (
    <header className="sticky top-0 z-20 h-16 border-b border-slate-200/80 dark:border-slate-800/80 bg-white/80 dark:bg-slate-900/80 backdrop-blur-md px-4 md:px-6 flex items-center justify-between">
      {/* Left: WebSocket / Real-time Status Badge */}
      <div className="flex items-center space-x-3">
        <div className="flex items-center space-x-2 px-3 py-1 rounded-full bg-slate-100 dark:bg-slate-800 text-xs font-medium text-slate-600 dark:text-slate-300">
          <span className={`w-2 h-2 rounded-full ${isConnected ? 'bg-emerald-500 animate-pulse' : 'bg-amber-500'}`} />
          <Wifi className="w-3.5 h-3.5" />
          <span className="hidden sm:inline">{isConnected ? 'Live Sync Active' : 'Connecting Broker...'}</span>
        </div>
      </div>

      {/* Right Actions */}
      <div className="flex items-center space-x-2 md:space-x-3">
        {/* Theme Toggle */}
        <button
          onClick={toggleTheme}
          className="p-2 rounded-xl text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          title="Toggle Light/Dark Theme"
        >
          {isDark ? <Sun className="w-5 h-5 text-amber-400" /> : <Moon className="w-5 h-5 text-slate-600" />}
        </button>

        {/* Notifications Button */}
        <button
          onClick={() => setIsNotifOpen(true)}
          className="relative p-2 rounded-xl text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          title="Notifications"
        >
          <Bell className="w-5 h-5" />
          {unreadCount > 0 && (
            <span className="absolute top-1.5 right-1.5 w-4 h-4 rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center animate-bounce">
              {unreadCount}
            </span>
          )}
        </button>

        {/* User Menu Dropdown */}
        <div className="relative">
          <button
            onClick={() => setIsMenuOpen(!isMenuOpen)}
            className="flex items-center space-x-2 p-1.5 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-brand-500 to-emerald-600 text-white font-bold text-xs flex items-center justify-center shadow-md shadow-brand-500/20">
              {user?.fullName?.charAt(0).toUpperCase() || 'U'}
            </div>
            <span className="hidden md:inline font-semibold text-sm text-slate-800 dark:text-slate-200">
              {user?.fullName || user?.email}
            </span>
          </button>

          {isMenuOpen && (
            <div className="absolute right-0 mt-2 w-56 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xl py-2 z-50 animate-fade-in">
              <div className="px-4 py-2 border-b border-slate-100 dark:border-slate-800">
                <p className="text-sm font-bold text-slate-900 dark:text-slate-100 truncate">{user?.fullName}</p>
                <p className="text-xs text-slate-500 dark:text-slate-400 truncate">{user?.email}</p>
              </div>
              <a
                href="/profile"
                className="flex items-center space-x-2 px-4 py-2.5 text-sm text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
              >
                <User className="w-4 h-4" />
                <span>My Profile</span>
              </a>
              <button
                onClick={logout}
                className="w-full flex items-center space-x-2 px-4 py-2.5 text-sm text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30 transition-colors text-left"
              >
                <LogOut className="w-4 h-4" />
                <span>Sign Out</span>
              </button>
            </div>
          )}
        </div>
      </div>

      <NotificationDrawer isOpen={isNotifOpen} onClose={() => setIsNotifOpen(false)} />
    </header>
  );
};

import React from 'react';
import { useTheme } from '../context/ThemeContext';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Moon, Sun, Monitor, Bell, Shield, Smartphone } from 'lucide-react';

export const SettingsPage: React.FC = () => {
  const { theme, setTheme } = useTheme();

  return (
    <div className="space-y-8 max-w-2xl mx-auto animate-fade-in">
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
          Settings & Preferences
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          Customize UI appearance, notification alerts, and health sensor settings
        </p>
      </div>

      {/* Theme Preference */}
      <Card className="p-6">
        <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100 mb-1">
          Appearance Theme
        </h3>
        <p className="text-xs text-slate-500 mb-4">Choose your preferred visual theme</p>

        <div className="grid grid-cols-3 gap-3">
          <button
            onClick={() => setTheme('light')}
            className={`p-4 rounded-2xl border flex flex-col items-center justify-center space-y-2 transition-all ${
              theme === 'light'
                ? 'bg-brand-50/80 border-brand-500 text-brand-700 font-bold shadow-md'
                : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 text-slate-600'
            }`}
          >
            <Sun className="w-6 h-6 text-amber-500" />
            <span className="text-xs">Light</span>
          </button>

          <button
            onClick={() => setTheme('dark')}
            className={`p-4 rounded-2xl border flex flex-col items-center justify-center space-y-2 transition-all ${
              theme === 'dark'
                ? 'bg-brand-950/80 border-brand-500 text-brand-300 font-bold shadow-md'
                : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 text-slate-600'
            }`}
          >
            <Moon className="w-6 h-6 text-indigo-400" />
            <span className="text-xs">Dark</span>
          </button>

          <button
            onClick={() => setTheme('system')}
            className={`p-4 rounded-2xl border flex flex-col items-center justify-center space-y-2 transition-all ${
              theme === 'system'
                ? 'bg-brand-50/80 dark:bg-brand-950/80 border-brand-500 text-brand-600 font-bold shadow-md'
                : 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800 text-slate-600'
            }`}
          >
            <Monitor className="w-6 h-6 text-sky-500" />
            <span className="text-xs">System</span>
          </button>
        </div>
      </Card>

      {/* Notifications Settings */}
      <Card className="p-6">
        <div className="flex items-center space-x-2 mb-3">
          <Bell className="w-5 h-5 text-brand-500" />
          <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100">
            Real-Time Notifications
          </h3>
        </div>

        <div className="space-y-3">
          <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800">
            <div>
              <p className="font-bold text-xs text-slate-900 dark:text-slate-100">AI Inactivity Alerts</p>
              <p className="text-[11px] text-slate-400">Receive STOMP notifications when inactive for 2+ hours</p>
            </div>
            <Badge variant="success">Enabled</Badge>
          </div>

          <div className="flex items-center justify-between p-3.5 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800">
            <div>
              <p className="font-bold text-xs text-slate-900 dark:text-slate-100">Team Leaderboard Updates</p>
              <p className="text-[11px] text-slate-400">Notify when rank changes on team squad leaderboard</p>
            </div>
            <Badge variant="success">Enabled</Badge>
          </div>
        </div>
      </Card>
    </div>
  );
};

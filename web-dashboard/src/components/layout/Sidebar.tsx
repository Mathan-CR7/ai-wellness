import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Activity,
  Trophy,
  Users,
  Bot,
  Dumbbell,
  User,
  Settings,
  Flame,
  Sparkles,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { clsx } from 'clsx';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ collapsed, onToggle }) => {
  const navItems = [
    { label: 'Dashboard', icon: LayoutDashboard, to: '/' },
    { label: 'Activity Analytics', icon: Activity, to: '/activity' },
    { label: 'Challenges', icon: Trophy, to: '/challenges' },
    { label: 'Team Leaderboard', icon: Flame, to: '/leaderboard' },
    { label: 'Teams', icon: Users, to: '/teams' },
    { label: 'AI Coach', icon: Bot, to: '/ai-coach', badge: 'AI' },
    { label: 'Workouts', icon: Dumbbell, to: '/exercises' },
    { label: 'Profile', icon: User, to: '/profile' },
    { label: 'Settings', icon: Settings, to: '/settings' },
  ];

  return (
    <aside
      className={clsx(
        'hidden md:flex flex-col border-r border-slate-200/80 dark:border-slate-800/80 bg-white/90 dark:bg-slate-900/90 backdrop-blur-md transition-all duration-300 z-30 sticky top-0 h-screen',
        collapsed ? 'w-20' : 'w-64'
      )}
    >
      {/* Brand Header */}
      <div className="flex items-center justify-between p-4 h-16 border-b border-slate-100 dark:border-slate-800">
        <div className="flex items-center space-x-3 overflow-hidden">
          <div className="flex-shrink-0 w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-600 to-emerald-400 flex items-center justify-center text-white shadow-md shadow-brand-500/20">
            <Sparkles className="w-5 h-5" />
          </div>
          {!collapsed && (
            <div className="flex flex-col">
              <span className="font-extrabold text-base tracking-tight text-slate-900 dark:text-white">
                AURA WELLNESS
              </span>
              <span className="text-[10px] uppercase font-bold tracking-widest text-brand-600 dark:text-brand-400">
                AI Fitness Tracker
              </span>
            </div>
          )}
        </div>
        <button
          onClick={onToggle}
          className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
        >
          {collapsed ? <ChevronRight className="w-4 h-4" /> : <ChevronLeft className="w-4 h-4" />}
        </button>
      </div>

      {/* Navigation Links */}
      <div className="flex-1 py-4 px-3 space-y-1 overflow-y-auto">
        {navItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              clsx(
                'flex items-center space-x-3 px-3.5 py-3 rounded-xl font-medium text-sm transition-all duration-200 group',
                isActive
                  ? 'bg-brand-500 text-white shadow-md shadow-brand-500/25 font-semibold'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800/60 hover:text-slate-900 dark:hover:text-slate-100'
              )
            }
          >
            <item.icon className="w-5 h-5 flex-shrink-0 transition-transform group-hover:scale-110" />
            {!collapsed && <span className="truncate">{item.label}</span>}
            {!collapsed && item.badge && (
              <span className="ml-auto px-2 py-0.5 text-[10px] font-extrabold uppercase rounded-full bg-ai-500 text-white shadow-sm">
                {item.badge}
              </span>
            )}
          </NavLink>
        ))}
      </div>

      {/* Footer Branding */}
      {!collapsed && (
        <div className="p-4 border-t border-slate-100 dark:border-slate-800">
          <div className="p-3.5 rounded-2xl bg-gradient-to-br from-brand-900/10 via-emerald-950/5 to-purple-900/10 border border-brand-500/20 text-xs">
            <p className="font-bold text-slate-900 dark:text-slate-200">Sync Status</p>
            <p className="text-slate-500 dark:text-slate-400 text-[11px] mt-0.5">
              Health Connect Sensor Active
            </p>
          </div>
        </div>
      )}
    </aside>
  );
};

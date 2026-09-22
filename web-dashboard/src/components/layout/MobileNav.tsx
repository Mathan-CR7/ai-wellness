import React from 'react';
import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Activity, Trophy, Flame, Bot, Dumbbell, User } from 'lucide-react';
import { clsx } from 'clsx';

export const MobileNav: React.FC = () => {
  const items = [
    { label: 'Home', icon: LayoutDashboard, to: '/' },
    { label: 'Activity', icon: Activity, to: '/activity' },
    { label: 'AI Coach', icon: Bot, to: '/ai-coach' },
    { label: 'Leaderboard', icon: Flame, to: '/leaderboard' },
    { label: 'Challenges', icon: Trophy, to: '/challenges' },
    { label: 'Profile', icon: User, to: '/profile' },
  ];

  return (
    <nav className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 dark:bg-slate-900/95 backdrop-blur-lg border-t border-slate-200/80 dark:border-slate-800/80 px-2 py-2">
      <div className="flex items-center justify-around">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              clsx(
                'flex flex-col items-center py-1 px-2 rounded-xl text-[11px] font-semibold transition-all',
                isActive
                  ? 'text-brand-600 dark:text-brand-400 font-extrabold scale-105'
                  : 'text-slate-500 dark:text-slate-400 hover:text-slate-700'
              )
            }
          >
            <item.icon className="w-5 h-5 mb-0.5" />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  );
};

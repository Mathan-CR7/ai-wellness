import React from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: 'default' | 'glass' | 'ai' | 'gradient';
}

export const Card: React.FC<CardProps> = ({
  children,
  className,
  variant = 'default',
  ...props
}) => {
  const base = 'rounded-2xl transition-all duration-200';
  
  const variants = {
    default: 'bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800/80 shadow-sm hover:shadow-md',
    glass: 'glass-panel shadow-sm hover:shadow-md',
    ai: 'bg-gradient-to-br from-ai-900/10 via-slate-900/5 to-purple-900/10 border border-ai-500/20 dark:border-ai-500/30 shadow-md',
    gradient: 'bg-gradient-to-br from-brand-600 to-emerald-700 text-white shadow-xl shadow-brand-600/20',
  };

  return (
    <div className={twMerge(clsx(base, variants[variant], className))} {...props}>
      {children}
    </div>
  );
};

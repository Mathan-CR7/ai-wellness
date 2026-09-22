import React from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: 'brand' | 'ai' | 'warning' | 'info' | 'success' | 'outline';
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  className,
  variant = 'brand',
  ...props
}) => {
  const base = 'inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold tracking-wide transition-colors';
  const variants = {
    brand: 'bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300 border border-brand-200/60 dark:border-brand-800/60',
    ai: 'bg-ai-50 text-ai-700 dark:bg-ai-950/50 dark:text-ai-300 border border-ai-200/60 dark:border-ai-800/60',
    warning: 'bg-amber-50 text-amber-700 dark:bg-amber-950/50 dark:text-amber-300 border border-amber-200/60 dark:border-amber-800/60',
    info: 'bg-sky-50 text-sky-700 dark:bg-sky-950/50 dark:text-sky-300 border border-sky-200/60 dark:border-sky-800/60',
    success: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/50 dark:text-emerald-300 border border-emerald-200/60 dark:border-emerald-800/60',
    outline: 'border border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400',
  };

  return (
    <span className={twMerge(clsx(base, variants[variant], className))} {...props}>
      {children}
    </span>
  );
};

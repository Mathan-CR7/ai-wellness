import React from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export interface ProgressProps extends React.HTMLAttributes<HTMLDivElement> {
  value: number;
  max?: number;
  variant?: 'brand' | 'ai' | 'warning';
}

export const Progress: React.FC<ProgressProps> = ({
  value,
  max = 100,
  variant = 'brand',
  className,
  ...props
}) => {
  const percent = Math.min(Math.max((value / max) * 100, 0), 100);

  const gradients = {
    brand: 'bg-gradient-to-r from-brand-500 to-emerald-400',
    ai: 'bg-gradient-to-r from-ai-500 to-indigo-500',
    warning: 'bg-gradient-to-r from-amber-500 to-orange-400',
  };

  return (
    <div
      className={twMerge(
        clsx('w-full bg-slate-100 dark:bg-slate-800 rounded-full overflow-hidden h-2.5', className)
      )}
      {...props}
    >
      <div
        className={twMerge(clsx('h-full transition-all duration-500 rounded-full', gradients[variant]))}
        style={{ width: `${percent}%` }}
      />
    </div>
  );
};

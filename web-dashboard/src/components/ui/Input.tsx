import React from 'react';
import { clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  leftIcon?: React.ReactNode;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, label, error, leftIcon, ...props }, ref) => {
    return (
      <div className="w-full">
        {label && (
          <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
            {label}
          </label>
        )}
        <div className="relative">
          {leftIcon && (
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
              {leftIcon}
            </div>
          )}
          <input
            ref={ref}
            className={twMerge(
              clsx(
                'w-full rounded-xl border bg-white dark:bg-slate-900/90 text-slate-900 dark:text-slate-100 text-sm px-4 py-2.5 transition-all focus:outline-none focus:ring-2 placeholder:text-slate-400 dark:placeholder:text-slate-600',
                leftIcon ? 'pl-10' : '',
                error
                  ? 'border-red-500 focus:ring-red-500/20'
                  : 'border-slate-200 dark:border-slate-800 focus:border-brand-500 focus:ring-brand-500/20',
                className
              )
            )}
            {...props}
          />
        </div>
        {error && <p className="mt-1 text-xs text-red-500 font-medium">{error}</p>}
      </div>
    );
  }
);

Input.displayName = 'Input';

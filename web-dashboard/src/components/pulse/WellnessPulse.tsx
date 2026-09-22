import React, { useState } from 'react';
import { Activity, Flame, ShieldCheck, TrendingUp, Zap } from 'lucide-react';
import { Card } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { InactivityWellnessModal } from '../wellness/InactivityWellnessModal';

interface WellnessPulseProps {
  currentSteps: number;
  dailyGoal: number;
  activeStreakDays?: number;
}

export const WellnessPulse: React.FC<WellnessPulseProps> = ({
  currentSteps,
  dailyGoal,
  activeStreakDays = 1,
}) => {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const percent = dailyGoal > 0 ? Math.min(Math.round((currentSteps / dailyGoal) * 100), 100) : 0;

  const getStatus = () => {
    if (percent >= 100) return { label: 'Goal Reached 🎉', color: 'emerald', badge: 'success', desc: 'Fantastic effort! You hit your daily activity goal.' };
    if (percent >= 75) return { label: 'Goal Progressing 🔥', color: 'brand', badge: 'brand', desc: 'You are right on track! Keep going strong.' };
    if (percent >= 40) return { label: 'Moderately Active 🏃', color: 'sky', badge: 'info', desc: 'Steady activity recorded. A short walk will boost your goal.' };
    return { label: 'Low Activity 🧘', color: 'amber', badge: 'warning', desc: 'Low movement recorded. Stand up and stretch for 5 minutes!' };
  };

  const status = getStatus();

  return (
    <>
      <Card className="p-5 relative overflow-hidden bg-gradient-to-br from-white via-slate-50/50 to-brand-50/20 dark:from-slate-900 dark:via-slate-900/90 dark:to-brand-950/20 border-slate-200/80 dark:border-slate-800">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center space-x-2">
            <div className="p-2 rounded-xl bg-brand-500/10 text-brand-600 dark:text-brand-400">
              <Activity className="w-5 h-5 animate-pulse" />
            </div>
            <div>
              <h3 className="font-extrabold text-sm uppercase tracking-wider text-slate-800 dark:text-slate-200">
                Wellness Pulse
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">Real-time movement evaluation</p>
            </div>
          </div>
          <Badge variant={status.badge as any}>{status.label}</Badge>
        </div>

        <div className="grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-slate-100 dark:border-slate-800/80">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-amber-500/10 text-amber-500">
              <Flame className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">Streak</span>
              <span className="font-extrabold text-base text-slate-900 dark:text-slate-100">
                {activeStreakDays} {activeStreakDays === 1 ? 'Day' : 'Days'}
              </span>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            <div className="p-2.5 rounded-xl bg-brand-500/10 text-brand-500">
              <TrendingUp className="w-5 h-5" />
            </div>
            <div>
              <span className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">Goal Target</span>
              <span className="font-extrabold text-base text-slate-900 dark:text-slate-100">{percent}%</span>
            </div>
          </div>
        </div>

        <div className="mt-4 pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between gap-3">
          <p className="text-xs text-slate-600 dark:text-slate-300 leading-relaxed">
            💡 {status.desc}
          </p>
          <Button
            variant="outline"
            size="sm"
            onClick={() => setIsModalOpen(true)}
            className="whitespace-nowrap text-xs shadow-xs"
          >
            Start Break 🌿
          </Button>
        </div>
      </Card>

      <InactivityWellnessModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        inactivityMinutes={135}
        recommendation="Low movement recorded. Stand up, walk for 5 minutes, and perform simple shoulder and neck stretches!"
      />
    </>
  );
};

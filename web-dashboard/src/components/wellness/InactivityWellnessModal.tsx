import React, { useState, useEffect } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { StretchAnimation, StretchType } from './StretchAnimation';
import { Footprints, Play, X, Sparkles, CheckCircle2, RotateCcw, Clock, Bell, ChevronRight } from 'lucide-react';

interface InactivityWellnessModalProps {
  isOpen: boolean;
  onClose: () => void;
  inactivityMinutes?: number;
  recommendation?: string;
}

export interface BreakStage {
  stageIndex: number;
  id: StretchType;
  title: string;
  durationSeconds: number;
  instruction: string;
  completionNotice: string;
}

const STAGES: BreakStage[] = [
  {
    stageIndex: 0,
    id: 'walking',
    title: 'Stage 1: 5-Minute Brisk Walk',
    durationSeconds: 300,
    instruction: 'Walk 400+ steps to reactivate lower body circulation.',
    completionNotice: '🎉 5-Minute Walk Completed! Next Stage: 3-Minute Shoulder Rotations.',
  },
  {
    stageIndex: 1,
    id: 'shoulder',
    title: 'Stage 2: 3-Minute Shoulder Rotations',
    durationSeconds: 180,
    instruction: 'Roll shoulders backward & forward 15 reps to unlock desk stiffness.',
    completionNotice: '✅ 3-Minute Shoulder Rotations Completed! Next Stage: 3-Minute Neck Stretches.',
  },
  {
    stageIndex: 2,
    id: 'neck',
    title: 'Stage 3: 3-Minute Neck Release Stretches',
    durationSeconds: 180,
    instruction: 'Gently tilt ear to shoulder holding 30 seconds for each side.',
    completionNotice: '✅ 3-Minute Neck Stretches Completed! Final Stage: 4-Minute Standing Side Torso Stretch.',
  },
  {
    stageIndex: 3,
    id: 'side',
    title: 'Stage 4: 4-Minute Standing Side Torso Stretch',
    durationSeconds: 240,
    instruction: 'Reach overhead with clasped hands & flex lateral torso for core mobility.',
    completionNotice: '🏆 15-Minute Movement Break Fully Completed! You are refreshed & energized.',
  },
];

export const InactivityWellnessModal: React.FC<InactivityWellnessModalProps> = ({
  isOpen,
  onClose,
  inactivityMinutes = 135,
  recommendation = "You've been sitting for over 2 hours! Take a short movement break to stretch your neck and shoulders.",
}) => {
  const [currentStageIdx, setCurrentStageIdx] = useState(0);
  const [activeStretch, setActiveStretch] = useState<StretchType>('walking');
  const [isSessionActive, setIsSessionActive] = useState(false);
  const [timerSeconds, setTimerSeconds] = useState(STAGES[0].durationSeconds);
  const [isCompleted, setIsCompleted] = useState(false);
  const [notificationMsg, setNotificationMsg] = useState<string | null>(null);

  const currentStage = STAGES[currentStageIdx];

  useEffect(() => {
    let interval: any = null;
    if (isSessionActive && timerSeconds > 0) {
      interval = setInterval(() => {
        setTimerSeconds((prev) => prev - 1);
      }, 1000);
    } else if (timerSeconds === 0 && isSessionActive) {
      // Current stage completed!
      const notice = currentStage.completionNotice;
      setNotificationMsg(notice);

      // Trigger browser notification if supported
      if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('🌿 Wellness Break Notification', { body: notice });
      }

      if (currentStageIdx < STAGES.length - 1) {
        // Advance to next stage!
        const nextIdx = currentStageIdx + 1;
        setCurrentStageIdx(nextIdx);
        setActiveStretch(STAGES[nextIdx].id);
        setTimerSeconds(STAGES[nextIdx].durationSeconds);
      } else {
        // Complete full 15-minute break
        setIsSessionActive(false);
        setIsCompleted(true);
      }
    }
    return () => clearInterval(interval);
  }, [isSessionActive, timerSeconds, currentStageIdx]);

  const handleStartBreak = () => {
    setCurrentStageIdx(0);
    setActiveStretch(STAGES[0].id);
    setIsSessionActive(true);
    setIsCompleted(false);
    setTimerSeconds(STAGES[0].durationSeconds);
    setNotificationMsg(null);

    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }
  };

  const handleResetSession = () => {
    setIsSessionActive(false);
    setIsCompleted(false);
    setCurrentStageIdx(0);
    setActiveStretch(STAGES[0].id);
    setTimerSeconds(STAGES[0].durationSeconds);
    setNotificationMsg(null);
  };

  const formatTimer = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const secs = sec % 60;
    return `${mins}:${secs < 10 ? '0' : ''}${secs}`;
  };

  const hours = Math.floor(inactivityMinutes / 60);
  const mins = inactivityMinutes % 60;
  const durationText = hours > 0 ? `${hours}h ${mins}m` : `${mins} min`;

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="🌿 15-Minute Multi-Stage Movement Break">
      <div className="space-y-5 text-center">
        {/* Banner */}
        <div className="p-4 rounded-2xl bg-gradient-to-r from-emerald-900/10 via-brand-900/10 to-teal-900/10 border border-brand-500/20 text-left">
          <div className="flex items-center justify-between gap-2 mb-2">
            <Badge variant="brand" className="bg-brand-500/10 text-brand-600 dark:text-brand-400 font-bold">
              ⚡ Sedentary for {durationText}
            </Badge>
            <Badge variant="warning">Total Break: 15 Mins</Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-700 dark:text-slate-300 font-medium leading-relaxed">
            "{recommendation}"
          </p>
        </div>

        {/* Live Stage Notification Toast Popup */}
        {notificationMsg && (
          <div className="p-3.5 rounded-2xl bg-gradient-to-r from-amber-500/20 to-brand-500/20 border border-amber-500/40 text-amber-900 dark:text-amber-200 font-extrabold text-xs flex items-center justify-between animate-bounce">
            <div className="flex items-center space-x-2 text-left">
              <Bell className="w-5 h-5 text-amber-500 flex-shrink-0" />
              <span>{notificationMsg}</span>
            </div>
            <button onClick={() => setNotificationMsg(null)} className="text-amber-500 hover:text-amber-700">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Active Stage & Vector Character Animation */}
        <div className="space-y-3">
          <div className="p-3 rounded-2xl bg-slate-100 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-800 flex items-center justify-between">
            <div className="text-left">
              <span className="text-[10px] font-extrabold uppercase text-brand-600 dark:text-brand-400 tracking-wider">
                Current Active Stage ({currentStageIdx + 1}/4)
              </span>
              <h4 className="text-sm font-extrabold text-slate-900 dark:text-white">
                {currentStage.title}
              </h4>
            </div>
            <Badge variant="brand">{currentStage.durationMinutes} Mins</Badge>
          </div>

          <StretchAnimation type={activeStretch} />

          <p className="text-xs text-slate-600 dark:text-slate-400 italic">
            "{currentStage.instruction}"
          </p>

          {isSessionActive && (
            <div className="p-3.5 rounded-2xl bg-brand-500/10 border border-brand-500/30">
              <span className="text-[11px] uppercase font-extrabold text-brand-600 dark:text-brand-400 block mb-0.5">
                Stage {currentStageIdx + 1} Timer Remaining
              </span>
              <span className="text-4xl font-black text-slate-900 dark:text-white tracking-tight font-mono">
                {formatTimer(timerSeconds)}
              </span>
            </div>
          )}

          {isCompleted && (
            <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-600 dark:text-emerald-400 font-extrabold text-sm flex items-center justify-center space-x-2">
              <CheckCircle2 className="w-6 h-6 text-emerald-500" />
              <span>15-Minute Guided Movement Break Complete! You're ready to focus. 🎉</span>
            </div>
          )}
        </div>

        {/* 4-Stage Progress Stepper Bar */}
        <div className="grid grid-cols-4 gap-2 pt-1">
          {STAGES.map((stg) => {
            const isDone = currentStageIdx > stg.stageIndex || isCompleted;
            const isCurrent = currentStageIdx === stg.stageIndex && isSessionActive;

            return (
              <div
                key={stg.stageIndex}
                className={`p-2 rounded-xl border text-center transition-all ${
                  isCurrent
                    ? 'bg-brand-500 text-white border-brand-600 shadow-md scale-105'
                    : isDone
                    ? 'bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border-emerald-500/30'
                    : 'bg-slate-50 dark:bg-slate-900 text-slate-400 border-slate-200 dark:border-slate-800'
                }`}
              >
                <div className="text-[10px] font-black uppercase">
                  {stg.stageIndex === 0 ? '5m Walk' : stg.stageIndex === 1 ? '3m Shoulder' : stg.stageIndex === 2 ? '3m Neck' : '4m Side'}
                </div>
                <div className="text-[9px] mt-0.5 opacity-80">
                  {isDone ? '✅ Done' : isCurrent ? '⏳ Running' : 'Wait'}
                </div>
              </div>
            );
          })}
        </div>

        {/* Modal Footer Actions */}
        <div className="flex items-center space-x-3 pt-2">
          {!isSessionActive ? (
            <Button
              variant="primary"
              size="lg"
              className="flex-1 shadow-lg shadow-brand-500/20"
              onClick={handleStartBreak}
            >
              <Play className="w-4 h-4 mr-2 fill-current" /> Start 15-Min Break
            </Button>
          ) : (
            <Button
              variant="outline"
              size="lg"
              className="flex-1"
              onClick={handleResetSession}
            >
              <RotateCcw className="w-4 h-4 mr-2" /> Restart Routine
            </Button>
          )}

          <Button
            variant="ghost"
            size="lg"
            className="text-slate-400 hover:text-slate-600"
            onClick={onClose}
          >
            Close
          </Button>
        </div>
      </div>
    </Modal>
  );
};

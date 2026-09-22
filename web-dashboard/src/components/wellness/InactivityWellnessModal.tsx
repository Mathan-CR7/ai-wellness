import React, { useState, useEffect } from 'react';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { StretchAnimation, StretchType } from './StretchAnimation';
import { Footprints, Play, X, Sparkles, CheckCircle2, RotateCcw, Clock } from 'lucide-react';

interface InactivityWellnessModalProps {
  isOpen: boolean;
  onClose: () => void;
  inactivityMinutes?: number;
  recommendation?: string;
}

export const InactivityWellnessModal: React.FC<InactivityWellnessModalProps> = ({
  isOpen,
  onClose,
  inactivityMinutes = 135,
  recommendation = "You've been sitting for over 2 hours! Take a short movement break to stretch your neck and shoulders.",
}) => {
  const [activeStretch, setActiveStretch] = useState<StretchType>('walking');
  const [isSessionActive, setIsSessionActive] = useState(false);
  const [timerSeconds, setTimerSeconds] = useState(300); // 5 min guided break
  const [isCompleted, setIsCompleted] = useState(false);

  useEffect(() => {
    let interval: any = null;
    if (isSessionActive && timerSeconds > 0) {
      interval = setInterval(() => {
        setTimerSeconds((prev) => prev - 1);
      }, 1000);
    } else if (timerSeconds === 0 && isSessionActive) {
      setIsSessionActive(false);
      setIsCompleted(true);
    }
    return () => clearInterval(interval);
  }, [isSessionActive, timerSeconds]);

  const handleStartBreak = () => {
    setIsSessionActive(true);
    setIsCompleted(false);
    setTimerSeconds(300);
  };

  const handleResetSession = () => {
    setIsSessionActive(false);
    setIsCompleted(false);
    setTimerSeconds(300);
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
    <Modal isOpen={isOpen} onClose={onClose} title="🌿 Time for a Movement Break">
      <div className="space-y-6 text-center">
        {/* Banner */}
        <div className="p-4 rounded-2xl bg-gradient-to-r from-emerald-900/10 via-brand-900/10 to-teal-900/10 border border-brand-500/20 text-left">
          <div className="flex items-center justify-between gap-2 mb-2">
            <Badge variant="brand" className="bg-brand-500/10 text-brand-600 dark:text-brand-400">
              ⚡ Inactive for {durationText}
            </Badge>
            <Badge variant="warning">Spring AI Guidance</Badge>
          </div>
          <p className="text-xs sm:text-sm text-slate-700 dark:text-slate-300 font-medium leading-relaxed">
            "{recommendation}"
          </p>
        </div>

        {/* Stretch Visualizer & Timer */}
        <div className="space-y-3">
          <StretchAnimation type={activeStretch} />

          {isSessionActive && (
            <div className="p-3 rounded-2xl bg-brand-500/10 border border-brand-500/30 animate-pulse">
              <span className="text-xs uppercase font-extrabold text-brand-600 dark:text-brand-400 block mb-0.5">
                Guided Movement Session Active
              </span>
              <span className="text-3xl font-black text-slate-900 dark:text-white tracking-tight">
                {formatTimer(timerSeconds)}
              </span>
            </div>
          )}

          {isCompleted && (
            <div className="p-3 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-600 dark:text-emerald-400 font-bold text-sm flex items-center justify-center space-x-2">
              <CheckCircle2 className="w-5 h-5" />
              <span>Great job completing your movement break! 🎉</span>
            </div>
          )}
        </div>

        {/* Stretch Movement Selector Tabs */}
        <div className="grid grid-cols-5 gap-1.5 p-1 rounded-2xl bg-slate-100 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-800">
          {[
            { id: 'walking', label: '5m Walk' },
            { id: 'neck', label: 'Neck Stretch' },
            { id: 'shoulder', label: 'Shoulder Rotation' },
            { id: 'side', label: 'Side Flex' },
            { id: 'arm', label: 'Arm Reach' },
          ].map((item) => (
            <button
              key={item.id}
              onClick={() => setActiveStretch(item.id as StretchType)}
              className={`py-2 px-1 rounded-xl text-[11px] font-extrabold transition-all ${
                activeStretch === item.id
                  ? 'bg-white dark:bg-slate-900 text-brand-600 dark:text-brand-400 shadow-xs'
                  : 'text-slate-500 hover:text-slate-700 dark:text-slate-400'
              }`}
            >
              {item.label}
            </button>
          ))}
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
              <Play className="w-4 h-4 mr-2 fill-current" /> Start Break
            </Button>
          ) : (
            <Button
              variant="outline"
              size="lg"
              className="flex-1"
              onClick={handleResetSession}
            >
              <RotateCcw className="w-4 h-4 mr-2" /> Reset Timer
            </Button>
          )}

          <Button
            variant="ghost"
            size="lg"
            className="text-slate-400 hover:text-slate-600"
            onClick={onClose}
          >
            Later
          </Button>
        </div>
      </div>
    </Modal>
  );
};

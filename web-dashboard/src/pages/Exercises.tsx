import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { exerciseService } from '../api/exerciseService';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { Dumbbell, Plus, Flame, Clock, Calendar, Play, Pause, CheckCircle, Trash2, BellRing, Timer, Lock } from 'lucide-react';

export interface ScheduledTask {
  id: string;
  exerciseType: string;
  durationMinutes: number;
  caloriesBurned: number;
  scheduledDateTime: string; // ISO or YYYY-MM-DDTHH:mm
  notes?: string;
  status: 'SCHEDULED' | 'RUNNING' | 'PAUSED' | 'COMPLETED';
  secondsLeft: number;
  startedAt?: string;
  completedAt?: string;
}

const STORAGE_KEY = 'aura_scheduled_workouts_v1';

export const ExercisesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<'schedule' | 'instant'>('schedule');
  const [, setTick] = useState(0);

  // Form States
  const [exerciseType, setExerciseType] = useState('WALKING');
  const [durationMinutes, setDurationMinutes] = useState(30);
  const [caloriesBurned, setCaloriesBurned] = useState(150);
  const [notes, setNotes] = useState('');
  const [scheduledDateTime, setScheduledDateTime] = useState(() => {
    const now = new Date();
    now.setMinutes(now.getMinutes() + 5);
    return now.toISOString().slice(0, 16);
  });
  const [errorMessage, setErrorMessage] = useState('');
  const [notificationToast, setNotificationToast] = useState<string | null>(null);

  // Scheduled Tasks State
  const [scheduledTasks, setScheduledTasks] = useState<ScheduledTask[]>(() => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY);
      return saved ? JSON.parse(saved) : [];
    } catch {
      return [];
    }
  });

  // Request browser notification permission on mount
  useEffect(() => {
    if (typeof window !== 'undefined' && 'Notification' in window) {
      if (Notification.permission === 'default') {
        Notification.requestPermission();
      }
    }
  }, []);

  // Sync scheduled tasks to LocalStorage
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(scheduledTasks));
  }, [scheduledTasks]);

  // Fetch logged exercises from backend
  const { data: exercises, isLoading } = useQuery({
    queryKey: ['myExercises'],
    queryFn: () => exerciseService.getMyExercises(),
  });

  const logMutation = useMutation({
    mutationFn: exerciseService.logExercise,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myExercises'] });
      setIsModalOpen(false);
      setNotes('');
      setErrorMessage('');
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Failed to log exercise session.';
      setErrorMessage(msg);
    },
  });

  // 1-second ticker for running countdowns & unlocking scheduled tasks dynamically
  useEffect(() => {
    const interval = setInterval(() => {
      setTick((t) => t + 1);

      setScheduledTasks((prevTasks) =>
        prevTasks.map((task) => {
          if (task.status !== 'RUNNING') return task;

          if (task.secondsLeft > 1) {
            return { ...task, secondsLeft: task.secondsLeft - 1 };
          }

          // Task just completed!
          triggerCompletionNotification(task);
          
          // Log automatically to backend
          exerciseService.logExercise({
            exerciseType: task.exerciseType,
            durationMinutes: task.durationMinutes,
            caloriesBurned: task.caloriesBurned,
            notes: `[Scheduled Task Completed] ${task.notes || ''}`.trim(),
            loggedAt: new Date().toISOString(),
          }).then(() => {
            queryClient.invalidateQueries({ queryKey: ['myExercises'] });
          }).catch((err) => {
            console.error('Failed to auto-log completed scheduled task:', err);
          });

          return {
            ...task,
            status: 'COMPLETED',
            secondsLeft: 0,
            completedAt: new Date().toISOString(),
          };
        })
      );
    }, 1000);

    return () => clearInterval(interval);
  }, [queryClient]);

  const triggerCompletionNotification = (task: ScheduledTask) => {
    const title = `🎉 Scheduled Workout Completed!`;
    const message = `Great job! You finished your ${task.exerciseType.replace('_', ' ')} session (${task.durationMinutes} mins, ${task.caloriesBurned} kcal).`;
    
    setNotificationToast(message);
    setTimeout(() => setNotificationToast(null), 8000);

    if (typeof window !== 'undefined' && 'Notification' in window && Notification.permission === 'granted') {
      new Notification(title, {
        body: message,
        icon: '/favicon.ico',
      });
    }
  };

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');

    if (activeTab === 'instant') {
      logMutation.mutate({
        exerciseType,
        durationMinutes: Number(durationMinutes),
        caloriesBurned: Number(caloriesBurned),
        notes,
      });
      return;
    }

    // Schedule Task
    if (!scheduledDateTime) {
      setErrorMessage('Please select a valid scheduled date and time.');
      return;
    }

    const newTask: ScheduledTask = {
      id: Date.now().toString(),
      exerciseType,
      durationMinutes: Number(durationMinutes),
      caloriesBurned: Number(caloriesBurned),
      scheduledDateTime,
      notes,
      status: 'SCHEDULED',
      secondsLeft: Number(durationMinutes) * 60,
    };

    setScheduledTasks((prev) => [newTask, ...prev]);
    setIsModalOpen(false);
    setNotes('');
    setNotificationToast(`📅 Scheduled ${exerciseType} for ${formatDate(scheduledDateTime)}`);
    setTimeout(() => setNotificationToast(null), 5000);
  };

  const handleStartTask = (id: string) => {
    const task = scheduledTasks.find((t) => t.id === id);
    if (task && task.scheduledDateTime) {
      const targetTime = new Date(task.scheduledDateTime).getTime();
      const now = Date.now();
      if (targetTime > now) {
        setNotificationToast(`🔒 Cannot start yet! Workout is scheduled for ${formatDate(task.scheduledDateTime)}`);
        setTimeout(() => setNotificationToast(null), 4000);
        return;
      }
    }

    setScheduledTasks((prev) =>
      prev.map((t) => (t.id === id ? { ...t, status: 'RUNNING', startedAt: new Date().toISOString() } : t))
    );
  };

  const getScheduleTimeUntilText = (scheduledDateTime: string) => {
    if (!scheduledDateTime) return 'Ready to Start';
    const target = new Date(scheduledDateTime).getTime();
    const now = Date.now();
    const diffMs = target - now;

    if (diffMs <= 0) return 'Scheduled Time Reached! Ready to Start';

    const diffMins = Math.ceil(diffMs / (1000 * 60));
    if (diffMins < 60) {
      return `Starts in ${diffMins} min${diffMins > 1 ? 's' : ''}`;
    }
    const hours = Math.floor(diffMins / 60);
    const remMins = diffMins % 60;
    return `Starts in ${hours}h ${remMins}m`;
  };

  const handlePauseTask = (id: string) => {
    setScheduledTasks((prev) =>
      prev.map((t) => (t.id === id ? { ...t, status: 'PAUSED' } : t))
    );
  };

  const handleCompleteEarly = (id: string) => {
    setScheduledTasks((prev) =>
      prev.map((t) => {
        if (t.id === id) {
          triggerCompletionNotification(t);
          exerciseService.logExercise({
            exerciseType: t.exerciseType,
            durationMinutes: t.durationMinutes,
            caloriesBurned: t.caloriesBurned,
            notes: `[Manually Completed Scheduled Task] ${t.notes || ''}`.trim(),
            loggedAt: new Date().toISOString(),
          }).then(() => {
            queryClient.invalidateQueries({ queryKey: ['myExercises'] });
          });
          return { ...t, status: 'COMPLETED', secondsLeft: 0, completedAt: new Date().toISOString() };
        }
        return t;
      })
    );
  };

  const handleDeleteTask = (id: string) => {
    setScheduledTasks((prev) => prev.filter((t) => t.id !== id));
  };

  const formatDate = (rawDate: any) => {
    if (!rawDate) return 'Today';
    try {
      if (Array.isArray(rawDate)) {
        const [y, m, d, h = 0, min = 0] = rawDate;
        return new Date(y, m - 1, d, h, min).toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
      }
      const dateObj = new Date(rawDate);
      if (isNaN(dateObj.getTime())) return 'Today';
      return dateObj.toLocaleString([], { dateStyle: 'medium', timeStyle: 'short' });
    } catch {
      return 'Today';
    }
  };

  const formatSeconds = (sec: number) => {
    const mins = Math.floor(sec / 60);
    const secs = sec % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Toast Notification Alert */}
      {notificationToast && (
        <div className="fixed top-5 right-5 z-50 bg-emerald-600 text-white px-5 py-3.5 rounded-2xl shadow-2xl flex items-center space-x-3 border border-emerald-400 animate-bounce">
          <BellRing className="w-5 h-5 text-amber-300" />
          <span className="text-xs font-bold">{notificationToast}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Workout & Task Scheduler
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Schedule multiple workouts with custom date & time, run live countdown timers, and track completed history
          </p>
        </div>
        <Button variant="primary" onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" /> Schedule or Log Workout
        </Button>
      </div>

      {/* Scheduled Workouts & Active Timers Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-extrabold text-slate-800 dark:text-slate-200 flex items-center">
            <Timer className="w-5 h-5 mr-2 text-brand-500" /> Scheduled Workouts & Live Timers
          </h2>
          <span className="text-xs text-slate-400 font-semibold">
            {scheduledTasks.length} Task(s) Active
          </span>
        </div>

        {scheduledTasks.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {scheduledTasks.map((task) => {
              const totalSec = task.durationMinutes * 60;
              const progressPct = Math.min(100, Math.max(0, ((totalSec - task.secondsLeft) / totalSec) * 100));

              return (
                <Card
                  key={task.id}
                  className={`p-5 flex flex-col justify-between border-2 transition-all ${
                    task.status === 'RUNNING'
                      ? 'border-emerald-500 shadow-lg shadow-emerald-500/10 bg-emerald-50/20 dark:bg-emerald-950/20'
                      : task.status === 'COMPLETED'
                      ? 'border-slate-200 dark:border-slate-800 opacity-80'
                      : 'border-slate-200 dark:border-slate-800'
                  }`}
                >
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <Badge
                        variant={
                          task.status === 'RUNNING'
                            ? 'brand'
                            : task.status === 'COMPLETED'
                            ? 'info'
                            : 'warning'
                        }
                      >
                        {task.exerciseType} • {task.status}
                      </Badge>
                      <button
                        onClick={() => handleDeleteTask(task.id)}
                        className="text-slate-400 hover:text-rose-500 transition-colors p-1"
                        title="Delete Task"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    <div className="space-y-1 mb-4">
                      <div className="flex items-center text-xs font-semibold text-slate-600 dark:text-slate-300">
                        <Calendar className="w-3.5 h-3.5 mr-1.5 text-brand-500" />
                        Scheduled: {formatDate(task.scheduledDateTime)}
                      </div>
                      <div className="flex items-center space-x-4 pt-2">
                        <div>
                          <span className="text-[10px] uppercase font-bold text-slate-400 block">Duration</span>
                          <span className="font-extrabold text-sm text-slate-800 dark:text-slate-200 flex items-center">
                            <Clock className="w-3.5 h-3.5 mr-1 text-sky-500" /> {task.durationMinutes} mins
                          </span>
                        </div>
                        <div>
                          <span className="text-[10px] uppercase font-bold text-slate-400 block">Est. Calories</span>
                          <span className="font-extrabold text-sm text-slate-800 dark:text-slate-200 flex items-center">
                            <Flame className="w-3.5 h-3.5 mr-1 text-orange-500" /> {task.caloriesBurned} kcal
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Timer Countdown Display */}
                    <div className="my-3 bg-slate-900 text-white rounded-xl p-3 text-center shadow-inner">
                      <div className="text-2xl font-black tracking-widest text-emerald-400 font-mono">
                        {formatSeconds(task.secondsLeft)}
                      </div>
                      <div className="text-[10px] uppercase tracking-wider text-slate-400 font-semibold mt-0.5">
                        {task.status === 'RUNNING'
                          ? 'Workout in Progress...'
                          : task.status === 'PAUSED'
                          ? 'Timer Paused'
                          : task.status === 'COMPLETED'
                          ? 'Workout Finished 🎉'
                          : getScheduleTimeUntilText(task.scheduledDateTime)}
                      </div>
                      {/* Progress Bar */}
                      <div className="w-full bg-slate-800 h-1.5 rounded-full mt-2 overflow-hidden">
                        <div
                          className="bg-emerald-500 h-full transition-all duration-500"
                          style={{ width: `${progressPct}%` }}
                        />
                      </div>
                    </div>

                    {task.notes && (
                      <p className="text-xs text-slate-500 dark:text-slate-400 mb-3 bg-slate-50 dark:bg-slate-800/60 p-2 rounded-lg italic">
                        "{task.notes}"
                      </p>
                    )}
                  </div>

                  {/* Action Buttons */}
                  <div className="pt-2 flex items-center gap-2">
                    {task.status === 'SCHEDULED' && (
                      (() => {
                        const isReady = !task.scheduledDateTime || new Date(task.scheduledDateTime).getTime() <= Date.now();
                        const timeUntil = getScheduleTimeUntilText(task.scheduledDateTime);

                        if (isReady) {
                          return (
                            <Button
                              variant="primary"
                              className="w-full text-xs py-2 bg-emerald-600 hover:bg-emerald-700 animate-pulse"
                              onClick={() => handleStartTask(task.id)}
                            >
                              <Play className="w-3.5 h-3.5 mr-1.5 fill-current" /> Start Timer Now
                            </Button>
                          );
                        }

                        return (
                          <Button
                            variant="outline"
                            disabled
                            className="w-full text-xs py-2 bg-slate-100 dark:bg-slate-800/80 text-slate-400 dark:text-slate-500 cursor-not-allowed border-slate-200 dark:border-slate-800"
                          >
                            <Lock className="w-3.5 h-3.5 mr-1.5 text-amber-500" /> {timeUntil}
                          </Button>
                        );
                      })()
                    )}

                    {task.status === 'RUNNING' && (
                      <>
                        <Button
                          variant="outline"
                          className="flex-1 text-xs py-2"
                          onClick={() => handlePauseTask(task.id)}
                        >
                          <Pause className="w-3.5 h-3.5 mr-1" /> Pause
                        </Button>
                        <Button
                          variant="primary"
                          className="flex-1 text-xs py-2 bg-blue-600 hover:bg-blue-700"
                          onClick={() => handleCompleteEarly(task.id)}
                        >
                          <CheckCircle className="w-3.5 h-3.5 mr-1" /> Finish
                        </Button>
                      </>
                    )}

                    {task.status === 'PAUSED' && (
                      <>
                        <Button
                          variant="primary"
                          className="flex-1 text-xs py-2 bg-emerald-600 hover:bg-emerald-700"
                          onClick={() => handleStartTask(task.id)}
                        >
                          <Play className="w-3.5 h-3.5 mr-1 fill-current" /> Resume
                        </Button>
                        <Button
                          variant="outline"
                          className="flex-1 text-xs py-2"
                          onClick={() => handleCompleteEarly(task.id)}
                        >
                          <CheckCircle className="w-3.5 h-3.5 mr-1" /> Finish
                        </Button>
                      </>
                    )}

                    {task.status === 'COMPLETED' && (
                      <div className="w-full text-center text-xs font-bold text-emerald-600 dark:text-emerald-400 py-1.5 bg-emerald-50 dark:bg-emerald-950/40 rounded-lg flex items-center justify-center">
                        <CheckCircle className="w-4 h-4 mr-1.5" /> Completed & Logged to DB
                      </div>
                    )}
                  </div>
                </Card>
              );
            })}
          </div>
        ) : (
          <div className="text-center py-10 bg-slate-50 dark:bg-slate-900/50 rounded-2xl border border-dashed border-slate-200 dark:border-slate-800 text-slate-400 text-xs">
            No scheduled workouts currently active. Click "Schedule or Log Workout" above to set dates & times for your sessions!
          </div>
        )}
      </div>

      {/* Logged History Section */}
      <div className="space-y-4 pt-6">
        <h2 className="text-lg font-extrabold text-slate-800 dark:text-slate-200 flex items-center">
          <Dumbbell className="w-5 h-5 mr-2 text-brand-500" /> Completed Workout History
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {isLoading ? (
            <p className="text-sm text-slate-400">Loading workout history...</p>
          ) : exercises && exercises.length > 0 ? (
            exercises.map((ex) => (
              <Card key={ex.id} className="p-5 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <Badge variant="brand">{ex.exerciseType}</Badge>
                    <span className="text-[11px] text-slate-400 font-medium">
                      {formatDate(ex.loggedAt || ex.createdAt)}
                    </span>
                  </div>

                  <div className="flex items-center space-x-4 my-2">
                    <div>
                      <span className="text-[10px] uppercase font-bold text-slate-400 block">Duration</span>
                      <span className="font-extrabold text-base text-slate-900 dark:text-slate-100 flex items-center">
                        <Clock className="w-3.5 h-3.5 mr-1 text-sky-500" /> {ex.durationMinutes} mins
                      </span>
                    </div>
                    <div>
                      <span className="text-[10px] uppercase font-bold text-slate-400 block">Calories</span>
                      <span className="font-extrabold text-base text-slate-900 dark:text-slate-100 flex items-center">
                        <Flame className="w-3.5 h-3.5 mr-1 text-orange-500" /> {ex.caloriesBurned} kcal
                      </span>
                    </div>
                  </div>

                  {ex.notes && (
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-2 bg-slate-50 dark:bg-slate-800/60 p-2.5 rounded-xl border border-slate-100 dark:border-slate-800">
                      "{ex.notes}"
                    </p>
                  )}
                </div>
              </Card>
            ))
          ) : (
            <div className="md:col-span-3 text-center py-12 text-slate-400 text-xs">
              No completed exercise sessions in history yet.
            </div>
          )}
        </div>
      </div>

      {/* Log / Schedule Workout Modal */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title="Schedule or Log Workout Session">
        {/* Mode Switch Tabs */}
        <div className="flex bg-slate-100 dark:bg-slate-800 p-1 rounded-xl mb-4">
          <button
            type="button"
            onClick={() => setActiveTab('schedule')}
            className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all ${
              activeTab === 'schedule'
                ? 'bg-white dark:bg-slate-900 text-brand-600 shadow-sm'
                : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            📅 Schedule for Date & Time
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('instant')}
            className={`flex-1 py-2 text-xs font-bold rounded-lg transition-all ${
              activeTab === 'instant'
                ? 'bg-white dark:bg-slate-900 text-brand-600 shadow-sm'
                : 'text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
            }`}
          >
            ⚡ Instant Log (Completed Now)
          </button>
        </div>

        {errorMessage && (
          <div className="mb-4 p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-rose-600 dark:text-rose-400 text-xs font-semibold">
            ⚠️ {errorMessage}
          </div>
        )}

        <form onSubmit={handleCreateTask} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
              Exercise Type
            </label>
            <select
              value={exerciseType}
              onChange={(e) => setExerciseType(e.target.value)}
              className="w-full rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 text-sm px-4 py-2.5 focus:outline-none focus:ring-2 focus:ring-brand-500"
            >
              <option value="WALKING">Walking / Brisk Walking</option>
              <option value="RUNNING">Running / Jogging</option>
              <option value="CYCLING">Cycling</option>
              <option value="SWIMMING">Swimming</option>
              <option value="YOGA">Yoga & Stretching</option>
              <option value="STRENGTH_TRAINING">Strength Training</option>
              <option value="HIIT">HIIT Workout</option>
              <option value="OTHER">Other Activity</option>
            </select>
          </div>

          {activeTab === 'schedule' && (
            <Input
              label="Scheduled Date & Time"
              type="datetime-local"
              value={scheduledDateTime}
              onChange={(e) => setScheduledDateTime(e.target.value)}
              required
            />
          )}

          <Input
            label="Duration (Minutes)"
            type="number"
            value={durationMinutes}
            onChange={(e) => {
              const mins = Number(e.target.value);
              setDurationMinutes(mins);
              setCaloriesBurned(mins * 5);
            }}
            required
          />

          <Input
            label="Estimated Calories Burned (kcal)"
            type="number"
            value={caloriesBurned}
            onChange={(e) => setCaloriesBurned(Number(e.target.value))}
            required
          />

          <Input
            label="Workout Notes (Optional)"
            placeholder="e.g. Evening park run with team"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />

          <Button type="submit" variant="primary" className="w-full mt-4" isLoading={logMutation.isPending}>
            {activeTab === 'schedule' ? '📅 Schedule Workout Task' : '⚡ Save Past Workout Session'}
          </Button>
        </form>
      </Modal>
    </div>
  );
};


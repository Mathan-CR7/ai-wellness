import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { exerciseService } from '../api/exerciseService';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { Dumbbell, Plus, Flame, Clock, Calendar } from 'lucide-react';

export const ExercisesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);

  const [exerciseType, setExerciseType] = useState('WALKING');
  const [durationMinutes, setDurationMinutes] = useState(30);
  const [caloriesBurned, setCaloriesBurned] = useState(150);
  const [notes, setNotes] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

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

  const handleLog = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    logMutation.mutate({
      exerciseType,
      durationMinutes: Number(durationMinutes),
      caloriesBurned: Number(caloriesBurned),
      notes,
    });
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Workout & Exercise Logging
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Record manual workout sessions, strength training, and sports
          </p>
        </div>
        <Button variant="primary" onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" /> Log Workout Session
        </Button>
      </div>

      {/* Exercise Logs Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {isLoading ? (
          <p className="text-sm text-slate-400">Loading workout logs...</p>
        ) : exercises && exercises.length > 0 ? (
          exercises.map((ex) => (
            <Card key={ex.id} className="p-5 flex flex-col justify-between">
              <div>
                <div className="flex items-center justify-between mb-3">
                  <Badge variant="brand">{ex.exerciseType}</Badge>
                  <span className="text-[11px] text-slate-400 font-medium">
                    {new Date(ex.loggedAt || ex.createdAt || '').toLocaleDateString()}
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
          <div className="md:col-span-3 text-center py-16 text-slate-400 text-xs">
            No logged exercise sessions yet. Click "Log Workout Session" to record your physical activity!
          </div>
        )}
      </div>

      {/* Log Workout Modal */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title="Log Workout Session">
        {errorMessage && (
          <div className="mb-4 p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-rose-600 dark:text-rose-400 text-xs font-semibold">
            ⚠️ {errorMessage}
          </div>
        )}
        <form onSubmit={handleLog} className="space-y-4">
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

          <Input
            label="Duration (Minutes)"
            type="number"
            value={durationMinutes}
            onChange={(e) => setDurationMinutes(Number(e.target.value))}
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
            placeholder="e.g. Morning outdoor jog around park"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />

          <Button type="submit" variant="primary" className="w-full mt-4" isLoading={logMutation.isPending}>
            Save Workout Session
          </Button>
        </form>
      </Modal>
    </div>
  );
};

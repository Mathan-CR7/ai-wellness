import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { challengeService } from '../api/challengeService';
import { ChallengeResponse } from '../types';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Progress } from '../components/ui/Progress';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { ChallengeDetailModal } from '../components/challenges/ChallengeDetailModal';
import { Trophy, Plus, Users, Calendar, CheckCircle2, Flag, Eye } from 'lucide-react';

export const ChallengesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedChallenge, setSelectedChallenge] = useState<ChallengeResponse | null>(null);

  // Form state
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [targetSteps, setTargetSteps] = useState(10000);
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState(new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0]);

  const [errorMessage, setErrorMessage] = useState('');

  const { data: challenges, isLoading } = useQuery({
    queryKey: ['activeChallenges'],
    queryFn: () => challengeService.getActiveChallenges(),
  });

  const joinMutation = useMutation({
    mutationFn: (id: number) => challengeService.joinChallenge(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['activeChallenges'] }),
  });

  const leaveMutation = useMutation({
    mutationFn: (id: number) => challengeService.leaveChallenge(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['activeChallenges'] }),
  });

  const createMutation = useMutation({
    mutationFn: challengeService.createChallenge,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activeChallenges'] });
      setIsModalOpen(false);
      setTitle('');
      setDescription('');
      setErrorMessage('');
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Failed to create challenge. Please check required fields.';
      setErrorMessage(msg);
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage('');
    createMutation.mutate({ title, description, targetSteps: Number(targetSteps), startDate, endDate });
  };

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Step Challenges
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Compete with teammates and complete daily activity targets
          </p>
        </div>
        <Button variant="primary" onClick={() => setIsModalOpen(true)}>
          <Plus className="w-4 h-4 mr-2" />
          Create Challenge
        </Button>
      </div>

      {/* Challenges Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {isLoading ? (
          <p className="text-sm text-slate-400">Loading active challenges...</p>
        ) : challenges && challenges.length > 0 ? (
          challenges.map((c) => (
            <Card
              key={c.id}
              className="p-6 flex flex-col justify-between hover:shadow-lg transition-all cursor-pointer border-slate-200/80 dark:border-slate-800"
              onClick={() => setSelectedChallenge(c)}
            >
              <div>
                <div className="flex items-start justify-between gap-2 mb-3">
                  <Badge variant="warning">
                    <Trophy className="w-3 h-3 mr-1" /> Target: {(c.targetValue || c.targetSteps || 10000).toLocaleString()} steps
                  </Badge>
                  {c.isParticipant ? (
                    <Badge variant="success">
                      <CheckCircle2 className="w-3 h-3 mr-1" /> Joined ✓
                    </Badge>
                  ) : (
                    <Badge variant="outline">Open</Badge>
                  )}
                </div>

                <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100 mb-1 group-hover:text-brand-500 transition-colors">
                  {c.title}
                </h3>
                <p className="text-xs text-slate-600 dark:text-slate-400 mb-4">{c.description}</p>
              </div>

              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                <span className="text-xs text-slate-500">
                  <Users className="w-3.5 h-3.5 inline mr-1" /> {c.totalParticipants || 1} Participants
                </span>

                <div className="flex items-center space-x-2" onClick={(e) => e.stopPropagation()}>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => setSelectedChallenge(c)}
                  >
                    <Eye className="w-3.5 h-3.5 mr-1" /> View Leaderboard
                  </Button>
                  {c.isParticipant ? (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => leaveMutation.mutate(c.id)}
                      isLoading={leaveMutation.isPending}
                    >
                      Leave
                    </Button>
                  ) : (
                    <Button
                      variant="primary"
                      size="sm"
                      onClick={() => joinMutation.mutate(c.id)}
                      isLoading={joinMutation.isPending}
                    >
                      Join Challenge
                    </Button>
                  )}
                </div>
              </div>
            </Card>
          ))
        ) : (
          <div className="md:col-span-2 text-center py-16 text-slate-400 text-xs">
            No active challenges yet. Click "Create Challenge" to start one!
          </div>
        )}
      </div>

      {/* Create Challenge Modal */}
      <Modal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} title="Create New Challenge">
        {errorMessage && (
          <div className="mb-4 p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-rose-600 dark:text-rose-400 text-xs font-semibold">
            ⚠️ {errorMessage}
          </div>
        )}
        <form onSubmit={handleCreate} className="space-y-4">
          <Input
            label="Challenge Title"
            placeholder="e.g. Daily 10k Steps Challenge"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
          <Input
            label="Description"
            placeholder="e.g. Walk at least 10,000 steps every day this week."
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            required
          />
          <Input
            label="Target Steps"
            type="number"
            value={targetSteps}
            onChange={(e) => setTargetSteps(Number(e.target.value))}
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Start Date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              required
            />
            <Input
              label="End Date"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              required
            />
          </div>
          <Button type="submit" variant="primary" className="w-full mt-4" isLoading={createMutation.isPending}>
            Publish Challenge
          </Button>
        </form>
      </Modal>

      {/* Challenge Detail & Joined Participants Leaderboard Modal */}
      <ChallengeDetailModal
        challenge={selectedChallenge}
        isOpen={!!selectedChallenge}
        onClose={() => setSelectedChallenge(null)}
      />
    </div>
  );
};

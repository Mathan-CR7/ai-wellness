import React, { useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { challengeService } from '../../api/challengeService';
import { ChallengeResponse, ChallengeLeaderboardResponse } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { wsManager } from '../../websocket/stompClient';
import { Modal } from '../ui/Modal';
import { Button } from '../ui/Button';
import { Badge } from '../ui/Badge';
import { Progress } from '../ui/Progress';
import { Skeleton } from '../ui/Skeleton';
import { Trophy, Calendar, Users, CheckCircle2, UserCheck, Flag, ArrowRight } from 'lucide-react';

interface ChallengeDetailModalProps {
  challenge: ChallengeResponse | null;
  isOpen: boolean;
  onClose: () => void;
}

export const ChallengeDetailModal: React.FC<ChallengeDetailModalProps> = ({
  challenge,
  isOpen,
  onClose,
}) => {
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const challengeId = challenge?.id || 0;

  // Query challenge details, members, and leaderboard with live polling
  const { data: members, isLoading: membersLoading } = useQuery({
    queryKey: ['challengeMembers', challengeId],
    queryFn: () => challengeService.getChallengeMembers(challengeId),
    enabled: isOpen && challengeId > 0,
    refetchInterval: 5000,
  });

  const { data: leaderboard, isLoading: leaderboardLoading } = useQuery({
    queryKey: ['challengeLeaderboard', challengeId],
    queryFn: () => challengeService.getChallengeLeaderboard(challengeId),
    enabled: isOpen && challengeId > 0,
    refetchInterval: 5000,
  });

  // Real-time WebSocket listener for instant leaderboard updates
  useEffect(() => {
    if (!isOpen || challengeId <= 0) return;

    const unsubscribe = wsManager.subscribe<ChallengeLeaderboardResponse>(
      `/topic/challenges/${challengeId}/leaderboard`,
      (updatedData) => {
        queryClient.setQueryData(['challengeLeaderboard', challengeId], updatedData);
      }
    );

    return () => {
      unsubscribe();
    };
  }, [isOpen, challengeId, queryClient]);

  const joinMutation = useMutation({
    mutationFn: (id: number) => challengeService.joinChallenge(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activeChallenges'] });
      queryClient.invalidateQueries({ queryKey: ['challengeMembers', challengeId] });
      queryClient.invalidateQueries({ queryKey: ['challengeLeaderboard', challengeId] });
    },
  });

  const leaveMutation = useMutation({
    mutationFn: (id: number) => challengeService.leaveChallenge(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activeChallenges'] });
      queryClient.invalidateQueries({ queryKey: ['challengeMembers', challengeId] });
      queryClient.invalidateQueries({ queryKey: ['challengeLeaderboard', challengeId] });
    },
  });

  if (!challenge) return null;

  const targetSteps = challenge.targetValue || challenge.targetSteps || 10000;
  const isUserJoined =
    challenge.isParticipant ||
    (members && user && members.some((m) => m.userId === user.id || (m.email && user.email && m.email.toLowerCase() === user.email.toLowerCase())));

  const participantCount = members?.length || challenge.totalParticipants || 1;

  const formatDate = (isoString?: string) => {
    if (!isoString) return '---';
    return new Date(isoString).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const rawEntries = leaderboard?.leaderboard || leaderboard?.rankings || [];

  const displayEntries: Array<{
    rank: number;
    userId: number;
    fullName?: string;
    email?: string;
    totalSteps: number;
    progressPercentage: number;
  }> =
    rawEntries.length > 0
      ? rawEntries.map((r) => ({
          rank: r.rank || 1,
          userId: r.userId,
          fullName: r.fullName || r.userFullName,
          email: r.email || r.userEmail,
          totalSteps: r.totalSteps ?? r.totalStepsInChallenge ?? 0,
          progressPercentage: r.progressPercentage || 0,
        }))
      : (members || []).map((m, idx) => ({
          rank: idx + 1,
          userId: m.userId,
          fullName: m.fullName || m.userFullName,
          email: m.email || m.userEmail,
          totalSteps: m.totalStepsInChallenge || 0,
          progressPercentage: Math.min(Math.round(((m.totalStepsInChallenge || 0) / targetSteps) * 100), 100),
        }));

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={challenge.title}>
      <div className="space-y-6">
        {/* Challenge Overview Header */}
        <div className="p-4 rounded-2xl bg-gradient-to-r from-emerald-900/10 via-brand-900/10 to-teal-900/10 border border-brand-500/20">
          <div className="flex items-center justify-between gap-2 mb-2">
            <Badge variant="warning" className="text-xs font-bold">
              <Trophy className="w-3.5 h-3.5 mr-1" /> Target Goal: {targetSteps.toLocaleString()} steps
            </Badge>
            {isUserJoined ? (
              <Badge variant="success" className="bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border-emerald-500/40">
                <UserCheck className="w-3.5 h-3.5 mr-1" /> Joined & Active
              </Badge>
            ) : (
              <Badge variant="outline">Open to Join</Badge>
            )}
          </div>

          <p className="text-sm text-slate-700 dark:text-slate-300 mt-2 font-medium">
            {challenge.description || 'Step up and complete this community walking target!'}
          </p>

          <div className="grid grid-cols-2 gap-4 mt-4 pt-3 border-t border-slate-200/60 dark:border-slate-800 text-xs text-slate-500">
            <div className="flex items-center space-x-1.5">
              <Calendar className="w-4 h-4 text-brand-500" />
              <span>
                <strong>Duration:</strong> {formatDate(challenge.startDate)} – {formatDate(challenge.endDate)}
              </span>
            </div>
            <div className="flex items-center space-x-1.5">
              <Users className="w-4 h-4 text-sky-500" />
              <span>
                <strong>Participants:</strong> {participantCount} enrolled
              </span>
            </div>
          </div>
        </div>

        {/* Join / Leave Actions */}
        <div className="flex items-center justify-between p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800">
          <div>
            <span className="text-xs font-bold uppercase text-slate-400 block">Your Status</span>
            <span className="text-sm font-extrabold text-slate-900 dark:text-white">
              {isUserJoined ? 'You are competing in this challenge!' : 'Ready to participate?'}
            </span>
          </div>

          {isUserJoined ? (
            <Button
              variant="outline"
              size="sm"
              className="border-rose-300 text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40"
              onClick={() => leaveMutation.mutate(challenge.id)}
              isLoading={leaveMutation.isPending}
            >
              Leave Challenge
            </Button>
          ) : (
            <Button
              variant="primary"
              size="sm"
              onClick={() => joinMutation.mutate(challenge.id)}
              isLoading={joinMutation.isPending}
            >
              <CheckCircle2 className="w-4 h-4 mr-1.5" /> Join Challenge Now
            </Button>
          )}
        </div>

        {/* Joined Participants & Challenge Leaderboard */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="font-extrabold text-sm text-slate-900 dark:text-slate-100 flex items-center">
              <Users className="w-4 h-4 mr-2 text-brand-500" /> Joined Participants & Rankings
            </h4>
            <span className="text-xs font-bold text-slate-400">{participantCount} Total Members</span>
          </div>

          {membersLoading || leaderboardLoading ? (
            <div className="space-y-2">
              <Skeleton className="w-full h-12 rounded-xl" />
              <Skeleton className="w-full h-12 rounded-xl" />
            </div>
          ) : (
            <div className="space-y-2 max-h-60 overflow-y-auto pr-1">
              {displayEntries.map((member) => {
                const isMe =
                  user &&
                  (user.id === member.userId ||
                    (user.email && member.email && user.email.toLowerCase() === member.email.toLowerCase()));

                const currentSteps = member.totalSteps || 0;
                const pct = member.progressPercentage || Math.min(Math.round((currentSteps / targetSteps) * 100), 100);
                const displayName = member.fullName || member.email || `User #${member.userId}`;

                return (
                  <div
                    key={member.userId || member.email || member.rank}
                    className={`p-3.5 rounded-xl border transition-all ${
                      isMe
                        ? 'bg-brand-50 dark:bg-brand-950/40 border-brand-500 shadow-xs'
                        : 'bg-white dark:bg-slate-900 border-slate-100 dark:border-slate-800'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center space-x-3">
                        <span className="w-6 h-6 rounded-lg bg-slate-100 dark:bg-slate-800 text-xs font-black flex items-center justify-center text-slate-700 dark:text-slate-300">
                          #{member.rank || 1}
                        </span>
                        <div>
                          <p className="text-xs font-bold text-slate-900 dark:text-slate-100 flex items-center space-x-1.5">
                            <span>{displayName}</span>
                            {isMe && <Badge variant="brand" className="text-[10px] py-0">YOU</Badge>}
                          </p>
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="font-extrabold text-xs text-brand-600 dark:text-brand-400">
                          {currentSteps.toLocaleString()} / {targetSteps.toLocaleString()} steps
                        </span>
                      </div>
                    </div>

                    <Progress value={currentSteps} max={targetSteps} variant={pct >= 100 ? 'warning' : 'brand'} className="h-1.5" />
                  </div>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </Modal>
  );
};

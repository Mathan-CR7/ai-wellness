import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { teamService } from '../../api/teamService';
import { TeamResponse, TeamMemberResponse, TeamLeaderboardResponse } from '../../types';
import { useAuth } from '../../context/AuthContext';
import { Modal } from '../ui/Modal';
import { Badge } from '../ui/Badge';
import { Skeleton } from '../ui/Skeleton';
import { Users, Crown, Calendar, ShieldCheck, Footprints, Flame, MapPin, Award } from 'lucide-react';
import { apiClient } from '../../api/apiClient';

interface TeamDetailModalProps {
  team: TeamResponse | null;
  isOpen: boolean;
  onClose: () => void;
}

export const TeamDetailModal: React.FC<TeamDetailModalProps> = ({
  team,
  isOpen,
  onClose,
}) => {
  const { user } = useAuth();
  const teamId = team?.id || 0;

  // Query team members list
  const { data: members, isLoading: membersLoading } = useQuery({
    queryKey: ['teamMembers', teamId],
    queryFn: () => teamService.getTeamMembers(teamId),
    enabled: isOpen && teamId > 0,
  });

  // Query team leaderboard rankings (steps, distance, calories)
  const { data: leaderboard, isLoading: leaderboardLoading } = useQuery({
    queryKey: ['teamLeaderboard', teamId],
    queryFn: async () => {
      const resp = await apiClient.get<TeamLeaderboardResponse>(`/api/teams/${teamId}/leaderboard`);
      return resp.data;
    },
    enabled: isOpen && teamId > 0,
  });

  if (!team) return null;

  const formatDate = (isoString?: string) => {
    if (!isoString) return 'Active Squad';
    return new Date(isoString).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
  };

  const ownerId = team.ownerId || team.ownerUserId;

  // Find creator/admin member
  const adminMember = (members || []).find(
    (m) =>
      (ownerId && m.userId === ownerId) ||
      m.role === 'LEADER' ||
      m.role === 'ADMIN'
  );

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={team.name}>
      <div className="space-y-6">
        {/* Team Overview Header */}
        <div className="p-4 rounded-2xl bg-gradient-to-r from-purple-900/10 via-brand-900/10 to-indigo-900/10 border border-purple-500/20">
          <div className="flex items-center justify-between gap-2 mb-2">
            <Badge variant="brand" className="text-xs font-bold">
              <Users className="w-3.5 h-3.5 mr-1" /> {team.memberCount || members?.length || 1} Members Enrolled
            </Badge>
            <span className="text-xs font-mono font-bold bg-slate-100 dark:bg-slate-800 px-3 py-1 rounded-lg text-slate-700 dark:text-slate-200">
              Invite Code: {team.inviteCode}
            </span>
          </div>

          <p className="text-sm text-slate-700 dark:text-slate-300 mt-2 font-medium">
            {team.description || 'Social fitness squad for step challenges and team activity tracking.'}
          </p>

          <div className="flex items-center justify-between mt-4 pt-3 border-t border-slate-200/60 dark:border-slate-800 text-xs text-slate-500">
            <div className="flex items-center space-x-1.5">
              <Calendar className="w-4 h-4 text-brand-500" />
              <span>
                <strong>Created Date:</strong> {formatDate(team.createdAt)}
              </span>
            </div>
            <Badge variant="success">Active Squad</Badge>
          </div>
        </div>

        {/* Highlight Team Admin / Creator */}
        <div className="p-4 rounded-2xl bg-amber-500/10 border border-amber-500/30">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-xl bg-amber-500 text-white font-extrabold flex items-center justify-center shadow-md shadow-amber-500/30">
                <Crown className="w-5 h-5" />
              </div>
              <div>
                <span className="text-[10px] font-extrabold uppercase tracking-wider text-amber-600 dark:text-amber-400 block">
                  Team Admin & Creator
                </span>
                <h4 className="text-sm font-extrabold text-slate-900 dark:text-white flex items-center space-x-2">
                  <span>{adminMember?.fullName || adminMember?.userFullName || adminMember?.email || 'Squad Creator'}</span>
                </h4>
              </div>
            </div>

            <Badge variant="warning" className="bg-amber-500/20 text-amber-700 dark:text-amber-300 border-amber-500/40 font-extrabold">
              <ShieldCheck className="w-3.5 h-3.5 mr-1" /> Squad Creator
            </Badge>
          </div>
        </div>

        {/* Joined Team Members & Activity Roster */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="font-extrabold text-sm text-slate-900 dark:text-slate-100 flex items-center">
              <Users className="w-4 h-4 mr-2 text-brand-500" /> Team Participants & Activity Rankings
            </h4>
            <span className="text-xs font-bold text-slate-400">
              {members?.length || 0} Total Members
            </span>
          </div>

          {membersLoading || leaderboardLoading ? (
            <div className="space-y-2">
              <Skeleton className="w-full h-14 rounded-xl" />
              <Skeleton className="w-full h-14 rounded-xl" />
            </div>
          ) : (
            <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
              {(members || []).map((m, idx) => {
                const isAdmin =
                  (ownerId && m.userId === ownerId) ||
                  m.role === 'LEADER' ||
                  m.role === 'ADMIN';

                const isMe = user && (user.id === m.userId || user.email === m.email || user.email === m.userEmail);

                // Find leaderboard metrics for this user if available
                const lbEntry = leaderboard?.rankings?.find((r) => r.userId === m.userId);

                const stepCount = lbEntry ? lbEntry.totalSteps : 0;
                const distanceKm = lbEntry ? parseFloat((lbEntry.totalDistanceMeters / 1000).toFixed(2)) : 0;
                const calories = lbEntry ? Math.round(lbEntry.totalCaloriesBurned) : 0;

                const nameDisplay = m.fullName || m.userFullName || m.email || m.userEmail || `User #${m.userId}`;

                return (
                  <div
                    key={m.userId || m.id}
                    className={`p-3.5 rounded-xl border transition-all ${
                      isAdmin
                        ? 'bg-amber-500/5 dark:bg-amber-950/20 border-amber-500/30 shadow-xs'
                        : isMe
                        ? 'bg-brand-50 dark:bg-brand-950/40 border-brand-500 shadow-xs'
                        : 'bg-white dark:bg-slate-900 border-slate-100 dark:border-slate-800'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-3">
                        <span
                          className={`w-7 h-7 rounded-lg text-xs font-black flex items-center justify-center ${
                            isAdmin
                              ? 'bg-amber-500 text-white shadow-xs'
                              : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                          }`}
                        >
                          {isAdmin ? <Crown className="w-4 h-4" /> : `#${idx + 1}`}
                        </span>

                        <div>
                          <p className="text-xs font-bold text-slate-900 dark:text-slate-100 flex items-center space-x-1.5">
                            <span>{nameDisplay}</span>
                            {isAdmin && (
                              <Badge variant="warning" className="text-[10px] py-0">
                                Admin
                              </Badge>
                            )}
                            {isMe && (
                              <Badge variant="brand" className="text-[10px] py-0">
                                YOU
                              </Badge>
                            )}
                          </p>
                          <p className="text-[11px] text-slate-400">
                            Joined: {formatDate(m.joinedAt)}
                          </p>
                        </div>
                      </div>

                      <div className="text-right">
                        <div className="flex items-center justify-end space-x-3 text-xs">
                          <span className="font-extrabold text-brand-600 dark:text-brand-400 flex items-center">
                            <Footprints className="w-3.5 h-3.5 mr-1" />
                            {stepCount.toLocaleString()} steps
                          </span>
                        </div>
                        {lbEntry && (
                          <div className="text-[10px] text-slate-400 mt-0.5 space-x-2">
                            <span>{distanceKm} km</span>
                            <span>•</span>
                            <span>{calories} kcal</span>
                          </div>
                        )}
                      </div>
                    </div>
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

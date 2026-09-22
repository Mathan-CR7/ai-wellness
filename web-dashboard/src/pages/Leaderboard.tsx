import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { leaderboardService } from '../api/leaderboardService';
import { teamService } from '../api/teamService';
import { useAuth } from '../context/AuthContext';
import { Card } from '../components/ui/Card';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Flame, Crown, Medal, Award, User, Users, ShieldAlert, Sparkles } from 'lucide-react';

export const LeaderboardPage: React.FC = () => {
  const { user } = useAuth();
  const [viewMode, setViewMode] = useState<'personal' | 'team'>('personal');

  const { data: leaderboard, isLoading } = useQuery({
    queryKey: ['teamLeaderboard', 1],
    queryFn: () => leaderboardService.getTeamLeaderboard(1),
    refetchInterval: 5000,
  });

  const { data: myTeams } = useQuery({
    queryKey: ['myTeams'],
    queryFn: () => teamService.getMyTeams(),
  });

  const rankings = leaderboard?.rankings || [];

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <div className="p-2 rounded-xl bg-orange-500/10 text-orange-500">
              <Flame className="w-5 h-5 animate-pulse" />
            </div>
            <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Community Leaderboards
            </h1>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Real-time step rankings for individual athletes and squads synced from Health Connect
          </p>
        </div>

        {/* View Switcher Toggle */}
        <div className="flex items-center space-x-1.5 p-1 rounded-xl bg-slate-200/60 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800">
          <button
            onClick={() => setViewMode('personal')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center space-x-1.5 ${
              viewMode === 'personal'
                ? 'bg-white dark:bg-slate-900 text-brand-600 dark:text-brand-400 shadow-sm'
                : 'text-slate-500 hover:text-slate-700 dark:text-slate-400'
            }`}
          >
            <User className="w-4 h-4" />
            <span>Personal Leaderboard</span>
          </button>
          <button
            onClick={() => setViewMode('team')}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-all flex items-center space-x-1.5 ${
              viewMode === 'team'
                ? 'bg-white dark:bg-slate-900 text-brand-600 dark:text-brand-400 shadow-sm'
                : 'text-slate-500 hover:text-slate-700 dark:text-slate-400'
            }`}
          >
            <Users className="w-4 h-4" />
            <span>Team Leaderboard</span>
          </button>
        </div>
      </div>

      {viewMode === 'personal' ? (
        <>
          {/* Top 3 Podium Highlights for Personal Leaderboard */}
          {rankings.length >= 3 && (
            <div className="grid grid-cols-3 gap-4 items-end pt-6 pb-2">
              {/* Rank 2 (Silver) */}
              <Card className="p-4 text-center order-1 bg-gradient-to-t from-slate-100 to-white dark:from-slate-800 dark:to-slate-900 border-slate-300">
                <Medal className="w-8 h-8 text-slate-400 mx-auto mb-2" />
                <p className="font-extrabold text-xs text-slate-900 dark:text-slate-100 truncate">
                  {rankings[1].fullName || rankings[1].email}
                </p>
                <span className="text-xs font-bold text-slate-500 block mt-1">
                  {rankings[1].totalSteps.toLocaleString()} steps
                </span>
                <Badge variant="outline" className="mt-2">#2 Silver</Badge>
              </Card>

              {/* Rank 1 (Gold) */}
              <Card className="p-5 text-center order-2 bg-gradient-to-t from-amber-500/20 via-slate-900/10 to-amber-500/10 border-amber-500/50 shadow-xl">
                <Crown className="w-10 h-10 text-amber-500 mx-auto mb-2 animate-bounce" />
                <p className="font-black text-sm text-slate-900 dark:text-white truncate">
                  {rankings[0].fullName || rankings[0].email}
                </p>
                <span className="text-sm font-extrabold text-brand-600 dark:text-brand-400 block mt-1">
                  {rankings[0].totalSteps.toLocaleString()} steps
                </span>
                <Badge variant="warning" className="mt-2">#1 Champion</Badge>
              </Card>

              {/* Rank 3 (Bronze) */}
              <Card className="p-4 text-center order-3 bg-gradient-to-t from-amber-900/20 to-white dark:from-slate-800 dark:to-slate-900 border-amber-800/40">
                <Award className="w-8 h-8 text-amber-700 mx-auto mb-2" />
                <p className="font-extrabold text-xs text-slate-900 dark:text-slate-100 truncate">
                  {rankings[2].fullName || rankings[2].email}
                </p>
                <span className="text-xs font-bold text-slate-500 block mt-1">
                  {rankings[2].totalSteps.toLocaleString()} steps
                </span>
                <Badge variant="outline" className="mt-2">#3 Bronze</Badge>
              </Card>
            </div>
          )}

          {/* Personal Rankings Table */}
          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-2">
                <User className="w-4 h-4 text-brand-500" />
                <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100">Individual Athlete Standings</h3>
              </div>
              <Badge variant="brand">Live Health Sensor Active</Badge>
            </div>

            {isLoading ? (
              <p className="text-sm text-slate-400">Loading live rankings...</p>
            ) : rankings.length > 0 ? (
              <div className="space-y-2.5">
                {rankings.map((entry) => {
                  const isCurrentUser =
                    user &&
                    (user.id === entry.userId ||
                      (user.email && entry.email && user.email.toLowerCase() === entry.email.toLowerCase()) ||
                      (user.fullName && entry.fullName && user.fullName.toLowerCase() === entry.fullName.toLowerCase()));

                  return (
                    <div
                      key={entry.userId || entry.email}
                      className={`flex items-center justify-between p-4 rounded-2xl border transition-all ${
                        isCurrentUser
                          ? 'bg-brand-50 dark:bg-brand-950/40 border-brand-500 shadow-md font-bold'
                          : 'bg-white dark:bg-slate-900/60 border-slate-100 dark:border-slate-800'
                      }`}
                    >
                      <div className="flex items-center space-x-4">
                        <span
                          className={`w-8 h-8 rounded-xl text-xs font-black flex items-center justify-center ${
                            entry.rank === 1
                              ? 'bg-amber-400 text-slate-900'
                              : entry.rank === 2
                              ? 'bg-slate-300 text-slate-900'
                              : entry.rank === 3
                              ? 'bg-amber-700 text-white'
                              : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
                          }`}
                        >
                          #{entry.rank}
                        </span>
                        <div>
                          <p className="text-sm text-slate-900 dark:text-slate-100 flex items-center space-x-2">
                            <span>{entry.fullName || entry.email}</span>
                            {isCurrentUser && <Badge variant="brand">YOU</Badge>}
                          </p>
                          <span className="text-xs text-slate-400">
                            {(entry.totalDistanceMeters / 1000).toFixed(2)} km covered
                          </span>
                        </div>
                      </div>

                      <div className="text-right">
                        <span className="font-extrabold text-sm text-brand-600 dark:text-brand-400 block">
                          {entry.totalSteps.toLocaleString()} steps
                        </span>
                        <span className="text-[11px] text-slate-400">
                          {Math.round(entry.totalCaloriesBurned)} kcal burned
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-12 text-slate-400 text-xs">
                No step records logged for team members yet.
              </div>
            )}
          </Card>
        </>
      ) : (
        /* Team Leaderboard View */
        <div className="space-y-6">
          <Card className="p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center space-x-2">
                <Users className="w-5 h-5 text-sky-500" />
                <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100">Squad Performance & Team Totals</h3>
              </div>
              <Badge variant="brand">Active Teams</Badge>
            </div>

            {myTeams && myTeams.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {myTeams.map((team) => {
                  const teamTotalSteps = rankings.reduce((acc, curr) => acc + curr.totalSteps, 0);
                  const avgSteps = team.memberCount > 0 ? Math.round(teamTotalSteps / team.memberCount) : 0;

                  return (
                    <Card key={team.id} className="p-5 border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/40">
                      <div className="flex items-start justify-between mb-2">
                        <div>
                          <h4 className="font-extrabold text-base text-slate-900 dark:text-slate-100">{team.name}</h4>
                          <p className="text-xs text-slate-500 dark:text-slate-400">{team.description || 'Active Wellness Squad'}</p>
                        </div>
                        <Badge variant="warning">Invite: {team.inviteCode}</Badge>
                      </div>

                      <div className="grid grid-cols-2 gap-4 my-4 pt-3 border-t border-slate-200 dark:border-slate-800 text-xs">
                        <div>
                          <span className="text-slate-400 font-semibold block uppercase text-[10px]">Team Total Steps</span>
                          <span className="text-base font-extrabold text-brand-600 dark:text-brand-400">
                            {teamTotalSteps.toLocaleString()} steps
                          </span>
                        </div>
                        <div>
                          <span className="text-slate-400 font-semibold block uppercase text-[10px]">Active Members</span>
                          <span className="text-base font-extrabold text-slate-900 dark:text-white">
                            {team.memberCount || rankings.length} Members
                          </span>
                        </div>
                      </div>

                      <div className="pt-2 flex items-center justify-between text-xs text-slate-500">
                        <span>Squad Step Avg: {avgSteps.toLocaleString()} steps/user</span>
                        <Badge variant="success">Active Rank #1</Badge>
                      </div>
                    </Card>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-12 text-slate-400 text-xs">
                You haven't joined a squad yet. Go to Teams page to create or join a squad!
              </div>
            )}
          </Card>
        </div>
      )}
    </div>
  );
};

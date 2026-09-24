import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../context/AuthContext';
import { activityService } from '../api/activityService';
import { challengeService } from '../api/challengeService';
import { leaderboardService } from '../api/leaderboardService';
import { Card } from '../components/ui/Card';
import { Progress } from '../components/ui/Progress';
import { Badge } from '../components/ui/Badge';
import { Button } from '../components/ui/Button';
import { Skeleton } from '../components/ui/Skeleton';
import { WellnessPulse } from '../components/pulse/WellnessPulse';
import {
  Flame,
  Footprints,
  Sparkles,
  Trophy,
  Zap,
  MapPin,
  Clock,
  ChevronRight,
  TrendingUp,
} from 'lucide-react';
import { Link } from 'react-router-dom';

export const Dashboard: React.FC = () => {
  const { user } = useAuth();

  // Queries for real backend data
  const { data: todaySteps, isLoading: stepsLoading } = useQuery({
    queryKey: ['todaySteps'],
    queryFn: () => activityService.getTodaySteps(),
    refetchInterval: 3000,
  });

  const { data: activitySummary } = useQuery({
    queryKey: ['activitySummaryToday'],
    queryFn: () => {
      const now = new Date();
      const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).toISOString();
      return activityService.getActivitySummary(startOfDay, now.toISOString());
    },
    refetchInterval: 3000,
  });

  const { data: trends } = useQuery({
    queryKey: ['activityTrends'],
    queryFn: () => activityService.getActivityTrends(),
    refetchInterval: 5000,
  });

  const { data: challenges } = useQuery({
    queryKey: ['activeChallenges'],
    queryFn: () => challengeService.getActiveChallenges(),
    refetchInterval: 5000,
  });

  const { data: leaderboard } = useQuery({
    queryKey: ['teamLeaderboard', 1],
    queryFn: () => leaderboardService.getTeamLeaderboard(1),
    refetchInterval: 3000,
  });

  // Dynamic Greeting based on current time
  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good Morning';
    if (hour < 17) return 'Good Afternoon';
    return 'Good Evening';
  };

  const steps = Math.max(todaySteps?.steps || 0, activitySummary?.totalSteps || 0);
  const goal = user?.dailyStepGoal || todaySteps?.goal || 10000;
  const progressPercent = Math.min(Math.round((steps / goal) * 100), 100);

  // Use genuine Health Connect synced metrics from backend activity summary, fallback to step formula if no sync
  const distanceKm =
    activitySummary?.totalDistanceMeters && activitySummary.totalDistanceMeters > 0
      ? (activitySummary.totalDistanceMeters / 1000).toFixed(2)
      : (steps * 0.00075).toFixed(2);

  const caloriesBurned =
    activitySummary?.totalCaloriesBurned && activitySummary.totalCaloriesBurned > 0
      ? Math.round(activitySummary.totalCaloriesBurned)
      : Math.round(steps * 0.04);

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Hero Welcome Banner */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-gradient-to-r from-brand-700 via-emerald-700 to-teal-800 p-6 md:p-8 rounded-3xl text-white shadow-xl shadow-brand-600/20 relative overflow-hidden">
        <div className="relative z-10 space-y-1">
          <Badge variant="brand" className="bg-white/20 text-white border-white/30 mb-2">
            ✨ Real-Time Health Sensor Active
          </Badge>
          <h1 className="text-2xl md:text-3xl font-extrabold tracking-tight">
            {getGreeting()}, {user?.fullName || 'Athlete'}!
          </h1>
          <p className="text-emerald-100 text-sm max-w-xl">
            Here is your physical movement report and activity intelligence for today.
          </p>
        </div>
        <div className="relative z-10 flex items-center space-x-3">
          <Link to="/ai-coach">
            <Button variant="ai" size="md" className="shadow-lg">
              <Sparkles className="w-4 h-4 mr-2" />
              Ask AI Coach
            </Button>
          </Link>
        </div>
        {/* Background Decorative Circles */}
        <div className="absolute -right-10 -bottom-10 w-48 h-48 bg-white/10 rounded-full blur-2xl pointer-events-none" />
      </div>

      {/* Main Metric Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Primary Step Counter Ring Card */}
        <Card className="p-6 md:col-span-2 flex flex-col justify-between relative overflow-hidden bg-white dark:bg-slate-900 border-slate-200/80 dark:border-slate-800">
          <div className="flex items-center justify-between mb-4">
            <div>
              <span className="text-xs font-bold uppercase tracking-wider text-slate-400">TODAY'S MOVEMENT</span>
              <h2 className="text-lg font-extrabold text-slate-900 dark:text-slate-100">Step Sensor Tracking</h2>
            </div>
            <Badge variant="brand">{progressPercent}% Goal Completed</Badge>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 items-center my-4">
            {/* Visual Ring Indicator */}
            <div className="relative w-44 h-44 mx-auto flex items-center justify-center">
              <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
                <circle
                  cx="50"
                  cy="50"
                  r="42"
                  className="text-slate-100 dark:text-slate-800"
                  strokeWidth="10"
                  stroke="currentColor"
                  fill="transparent"
                />
                <circle
                  cx="50"
                  cy="50"
                  r="42"
                  className="text-brand-500 transition-all duration-1000 ease-out"
                  strokeWidth="10"
                  strokeDasharray={264}
                  strokeDashoffset={264 - (264 * progressPercent) / 100}
                  strokeLinecap="round"
                  stroke="currentColor"
                  fill="transparent"
                />
              </svg>
              <div className="absolute flex flex-col items-center justify-center text-center">
                <Footprints className="w-6 h-6 text-brand-500 mb-1 animate-bounce" />
                <span className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
                  {stepsLoading ? <Skeleton className="w-16 h-8" /> : steps.toLocaleString()}
                </span>
                <span className="text-[11px] font-semibold text-slate-400">/ {goal.toLocaleString()} steps</span>
              </div>
            </div>

            {/* Quick Metrics Column */}
            <div className="space-y-4">
              <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-800 flex items-center space-x-3">
                <div className="p-2.5 rounded-xl bg-blue-500/10 text-blue-500">
                  <MapPin className="w-5 h-5" />
                </div>
                <div>
                  <span className="text-[11px] text-slate-400 uppercase font-semibold block">Distance Covered</span>
                  <span className="font-extrabold text-base text-slate-900 dark:text-slate-100">
                    {distanceKm} km
                  </span>
                </div>
              </div>

              <div className="p-3.5 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-100 dark:border-slate-800 flex items-center space-x-3">
                <div className="p-2.5 rounded-xl bg-orange-500/10 text-orange-500">
                  <Flame className="w-5 h-5" />
                </div>
                <div>
                  <span className="text-[11px] text-slate-400 uppercase font-semibold block">Active Calories</span>
                  <span className="font-extrabold text-base text-slate-900 dark:text-slate-100">
                    {caloriesBurned} kcal
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
            <span>7-Day Daily Avg: {trends?.sevenDayAverageSteps ? Math.round(trends.sevenDayAverageSteps).toLocaleString() : '---'} steps</span>
            <span>Streak: {trends?.activeStreakDays || 1} days</span>
          </div>
        </Card>

        {/* Wellness Pulse Component */}
        <WellnessPulse
          currentSteps={steps}
          dailyGoal={goal}
          activeStreakDays={trends?.activeStreakDays || 1}
        />
      </div>

      {/* Social & Challenges Row */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Active Step Challenge Card */}
        <Card className="p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <div className="p-2 rounded-xl bg-amber-500/10 text-amber-500">
                <Trophy className="w-5 h-5" />
              </div>
              <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100">Active Challenge</h3>
            </div>
            <Link to="/challenges">
              <Button variant="ghost" size="sm">
                View All <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </Link>
          </div>

          {challenges && challenges.length > 0 ? (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800">
                <h4 className="font-bold text-sm text-slate-900 dark:text-slate-100 mb-1">
                  {challenges[0].title}
                </h4>
                <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2 mb-3">
                  {challenges[0].description}
                </p>
                {(() => {
                  const target = challenges[0].targetValue || challenges[0].targetSteps || 10000;
                  const pct = Math.min(Math.round((steps / target) * 100), 100);
                  return (
                    <>
                      <Progress value={steps} max={target} variant="warning" className="h-3" />
                      <div className="flex items-center justify-between text-xs text-slate-500 mt-2">
                        <span>{steps.toLocaleString()} / {target.toLocaleString()} steps</span>
                        <span>{pct}%</span>
                      </div>
                    </>
                  );
                })()}
              </div>
            </div>
          ) : (
            <div className="text-center py-8 text-slate-400 text-xs">
              No active challenges right now. Join one from the Challenges page!
            </div>
          )}
        </Card>

        {/* Live Team Leaderboard Preview Card */}
        <Card className="p-6">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <div className="p-2 rounded-xl bg-blue-500/10 text-blue-500">
                <Flame className="w-5 h-5" />
              </div>
              <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100">Team Leaderboard</h3>
            </div>
            <Link to="/leaderboard">
              <Button variant="ghost" size="sm">
                Full Rankings <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            </Link>
          </div>

          {leaderboard?.rankings && leaderboard.rankings.length > 0 ? (
            <div className="space-y-2.5">
              {leaderboard.rankings.slice(0, 3).map((entry) => (
                <div
                  key={entry.userId}
                  className="flex items-center justify-between p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 border border-slate-100 dark:border-slate-800"
                >
                  <div className="flex items-center space-x-3">
                    <span
                      className={`w-6 h-6 rounded-lg text-xs font-black flex items-center justify-center ${
                        entry.rank === 1
                          ? 'bg-amber-400 text-slate-900'
                          : entry.rank === 2
                          ? 'bg-slate-300 text-slate-900'
                          : 'bg-amber-700 text-white'
                      }`}
                    >
                      #{entry.rank}
                    </span>
                    <span className="font-bold text-xs text-slate-800 dark:text-slate-200">
                      {entry.fullName || entry.email}
                    </span>
                  </div>
                  <span className="font-extrabold text-xs text-brand-600 dark:text-brand-400">
                    {entry.totalSteps.toLocaleString()} steps
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8 text-slate-400 text-xs">
              No team activity recorded yet.
            </div>
          )}
        </Card>
      </div>
    </div>
  );
};

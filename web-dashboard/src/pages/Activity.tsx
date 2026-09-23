import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { activityService } from '../api/activityService';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Skeleton } from '../components/ui/Skeleton';
import { Activity as ActivityIcon, Calendar, Flame, MapPin, TrendingUp, Zap } from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  CartesianGrid,
  AreaChart,
  Area,
} from 'recharts';

export const ActivityPage: React.FC = () => {
  const [timeRange, setTimeRange] = useState<'today' | '7d' | '30d'>('7d');

  const { data: activities, isLoading } = useQuery({
    queryKey: ['myActivities'],
    queryFn: () => activityService.getMyActivities(),
  });

  const { data: trends } = useQuery({
    queryKey: ['activityTrends'],
    queryFn: () => activityService.getActivityTrends(),
  });

  // Prepare chart data safely from real activity history
  const chartData = (activities || []).slice(0, 14).map((a) => ({
    date: new Date(a.startTime).toLocaleDateString([], { month: 'short', day: 'numeric' }),
    steps: a.stepCount,
    calories: a.caloriesBurned,
    distanceKm: parseFloat((a.distanceMeters / 1000).toFixed(2)),
  })).reverse();

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Activity Analytics
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Sensor movement history & calorie expenditure visualization
          </p>
        </div>

        <div className="flex items-center space-x-1.5 p-1 rounded-xl bg-slate-200/60 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800">
          {(['today', '7d', '30d'] as const).map((r) => (
            <button
              key={r}
              onClick={() => setTimeRange(r)}
              className={`px-3 py-1.5 rounded-lg text-xs font-bold uppercase transition-all ${
                timeRange === r
                  ? 'bg-white dark:bg-slate-900 text-brand-600 dark:text-brand-400 shadow-xs'
                  : 'text-slate-500 hover:text-slate-700 dark:text-slate-400'
              }`}
            >
              {r === 'today' ? 'Today' : r === '7d' ? '7 Days' : '30 Days'}
            </button>
          ))}
        </div>
      </div>

      {/* Analytics Summary Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
        {(() => {
          const avg = trends?.sevenDayAverageSteps ?? trends?.movingAverageSteps7Days ?? 0;
          const completion = trends?.goalCompletionRatePercentage ?? trends?.stepCompletionRatePercent ?? (avg > 0 ? (avg / 100) : 0);
          const displayAvg = avg > 0 ? Math.round(avg) : (chartData.length > 0 ? Math.round(chartData[chartData.length - 1].steps) : 0);
          return (
            <>
              <Card className="p-4">
                <span className="text-[11px] font-semibold text-slate-400 uppercase">7-Day Avg Steps</span>
                <p className="text-xl font-extrabold text-slate-900 dark:text-white mt-1">
                  {displayAvg > 0 ? displayAvg.toLocaleString() : '---'}
                </p>
              </Card>
              <Card className="p-4">
                <span className="text-[11px] font-semibold text-slate-400 uppercase">Goal Completion Rate</span>
                <p className="text-xl font-extrabold text-brand-600 dark:text-brand-400 mt-1">
                  {Math.round(completion)}%
                </p>
              </Card>
            </>
          );
        })()}
        <Card className="p-4">
          <span className="text-[11px] font-semibold text-slate-400 uppercase">Active Streak</span>
          <p className="text-xl font-extrabold text-amber-500 mt-1">
            {trends?.activeStreakDays || 1} Days
          </p>
        </Card>
        <Card className="p-4">
          <span className="text-[11px] font-semibold text-slate-400 uppercase">Total Records</span>
          <p className="text-xl font-extrabold text-sky-500 mt-1">
            {activities?.length || 0} Sessions
          </p>
        </Card>
      </div>

      {/* Main Recharts Step Visualization */}
      <Card className="p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center space-x-2">
            <div className="p-2 rounded-xl bg-brand-500/10 text-brand-500">
              <ActivityIcon className="w-5 h-5" />
            </div>
            <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100">Step Trend Chart</h3>
          </div>
          <Badge variant="brand">Health Connect Sensor</Badge>
        </div>

        {isLoading ? (
          <Skeleton className="w-full h-72" />
        ) : chartData.length > 0 ? (
          <div className="h-72 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#334155" opacity={0.15} />
                <XAxis dataKey="date" stroke="#94a3b8" fontSize={11} tickLine={false} />
                <YAxis stroke="#94a3b8" fontSize={11} tickLine={false} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#0f172a',
                    borderColor: '#1e293b',
                    borderRadius: '12px',
                    color: '#fff',
                  }}
                />
                <Bar dataKey="steps" fill="#10b981" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <div className="text-center py-16 text-slate-400 text-xs">
            No activity records found yet. Sync steps from Android Health Connect app!
          </div>
        )}
      </Card>
    </div>
  );
};

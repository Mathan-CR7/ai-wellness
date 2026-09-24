import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { userService } from '../api/userService';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { User, Mail, Target, Scale, Ruler, CheckCircle2, AlertCircle } from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user, refreshProfile } = useAuth();

  const [fullName, setFullName] = useState(user?.fullName || '');
  const [dailyStepGoal, setDailyStepGoal] = useState(user?.dailyStepGoal || 10000);
  const [weightKg, setWeightKg] = useState(user?.weightKg || 70);
  const [heightCm, setHeightCm] = useState(user?.heightCm || 175);
  const [successMessage, setSuccessMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    if (user) {
      setFullName(user.fullName || '');
      setDailyStepGoal(user.dailyStepGoal || 10000);
      setWeightKg(user.weightKg || 70);
      setHeightCm(user.heightCm || 175);
    }
  }, [user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSuccessMessage('');
    setErrorMessage('');
    setIsLoading(true);

    try {
      await userService.updateProfile({
        fullName,
        dailyStepGoal: Number(dailyStepGoal),
        weightKg: Number(weightKg),
        heightCm: Number(heightCm),
      });
      await refreshProfile();
      setSuccessMessage('Profile updated successfully!');
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || 'Failed to update profile. Please try again.';
      setErrorMessage(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="space-y-8 max-w-2xl mx-auto animate-fade-in">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
          User Profile
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          Manage your personal details and daily activity targets
        </p>
      </div>

      <Card className="p-8">
        <div className="flex items-center space-x-4 mb-6 pb-6 border-b border-slate-100 dark:border-slate-800">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-brand-600 to-emerald-400 text-white font-black text-2xl flex items-center justify-center shadow-lg shadow-brand-500/20">
            {user?.fullName?.charAt(0).toUpperCase() || 'U'}
          </div>
          <div>
            <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100">{user?.fullName}</h2>
            <p className="text-xs text-slate-500">{user?.email}</p>
            <Badge variant="brand" className="mt-1">Role: {user?.role || 'USER'}</Badge>
          </div>
        </div>

        {successMessage && (
          <div className="mb-4 p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 text-emerald-600 dark:text-emerald-400 text-xs font-semibold flex items-center space-x-2">
            <CheckCircle2 className="w-4 h-4" />
            <span>{successMessage}</span>
          </div>
        )}

        {errorMessage && (
          <div className="mb-4 p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 text-rose-600 dark:text-rose-400 text-xs font-semibold flex items-center space-x-2">
            <AlertCircle className="w-4 h-4" />
            <span>{errorMessage}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <Input
            label="Full Name"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            leftIcon={<User className="w-4 h-4" />}
            required
          />

          <Input
            label="Daily Step Goal"
            type="number"
            value={dailyStepGoal}
            onChange={(e) => setDailyStepGoal(Number(e.target.value))}
            leftIcon={<Target className="w-4 h-4" />}
            required
          />

          <div className="grid grid-cols-2 gap-4">
            <Input
              label="Weight (kg)"
              type="number"
              value={weightKg}
              onChange={(e) => setWeightKg(Number(e.target.value))}
              leftIcon={<Scale className="w-4 h-4" />}
            />
            <Input
              label="Height (cm)"
              type="number"
              value={heightCm}
              onChange={(e) => setHeightCm(Number(e.target.value))}
              leftIcon={<Ruler className="w-4 h-4" />}
            />
          </div>

          <Button type="submit" variant="primary" size="lg" className="w-full mt-4" isLoading={isLoading}>
            Save Profile Changes
          </Button>
        </form>
      </Card>
    </div>
  );
};

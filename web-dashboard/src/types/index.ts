export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  role: string;
  dailyStepGoal: number;
  weightKg?: number;
  heightCm?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  tokenType: string;
  user: UserProfile;
}

export interface DailyStepResponse {
  id: number;
  userId: number;
  date: string;
  steps: number;
  goal: number;
  goalMet: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ActivityResponse {
  id: number;
  userId: number;
  stepCount: number;
  distanceMeters: number;
  caloriesBurned: number;
  activeDurationSeconds: number;
  startTime: string;
  endTime: string;
  sourceDevice?: string;
  syncedAt?: string;
}

export interface ActivitySummaryResponse {
  totalSteps: number;
  totalDistanceMeters: number;
  totalCaloriesBurned: number;
  totalActiveSeconds: number;
  recordCount: number;
  averageDailySteps: number;
}

export interface ActivityTrendResponse {
  sevenDayAverageSteps?: number;
  movingAverageSteps7Days?: number;
  goalCompletionRatePercentage?: number;
  stepCompletionRatePercent?: number;
  activeStreakDays?: number;
  activeDaysInPeriod?: number;
}

export interface ChallengeResponse {
  id: number;
  title: string;
  description: string;
  targetType?: string;
  targetValue?: number;
  targetSteps?: number;
  startDate: string;
  endDate: string;
  createdBy?: number;
  createdByUserId?: number;
  isParticipant?: boolean;
  totalParticipants?: number;
  createdAt?: string;
}

export interface ChallengeMemberResponse {
  id: number;
  challengeId: number;
  userId: number;
  userEmail: string;
  userFullName: string;
  totalStepsInChallenge: number;
  joinedAt: string;
}

export interface LeaderboardEntryDto {
  rank: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  totalStepsInChallenge: number;
  progressPercentage: number;
  isCurrentUser?: boolean;
}

export interface ChallengeLeaderboardResponse {
  challengeId: number;
  challengeTitle: string;
  targetSteps: number;
  rankings: LeaderboardEntryDto[];
}

export interface ChallengeProgressResponse {
  challengeId: number;
  challengeTitle: string;
  targetSteps: number;
  currentSteps: number;
  progressPercentage: number;
  isCompleted: boolean;
  daysRemaining: number;
}

export interface TeamResponse {
  id: number;
  name: string;
  description?: string;
  inviteCode: string;
  ownerUserId: number;
  memberCount: number;
  createdAt?: string;
}

export interface TeamMemberResponse {
  id: number;
  teamId: number;
  userId: number;
  userFullName: string;
  userEmail: string;
  role: string;
  joinedAt: string;
}

export interface TeamLeaderboardEntry {
  rank: number;
  userId: number;
  fullName: string;
  email: string;
  totalSteps: number;
  totalDistanceMeters: number;
  totalCaloriesBurned: number;
}

export interface TeamLeaderboardResponse {
  teamId: number;
  teamName: string;
  startTime: string;
  endTime: string;
  rankings: TeamLeaderboardEntry[];
}

export interface ExerciseResponse {
  id: number;
  userId: number;
  exerciseType: string;
  durationMinutes: number;
  caloriesBurned: number;
  notes?: string;
  loggedAt: string;
  createdAt?: string;
}

export interface AIChatResponse {
  message: string;
  conversationId: number;
  timestamp: string;
}

export interface InactivitySuggestionMessage {
  userId: number;
  userEmail: string;
  suggestion: string;
  inactivityMinutes: number;
  currentSteps: number;
  timestamp: string;
}

export interface NotificationItem {
  id: string;
  type: 'inactivity' | 'ai_recommendation' | 'challenge' | 'leaderboard' | 'team';
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
}

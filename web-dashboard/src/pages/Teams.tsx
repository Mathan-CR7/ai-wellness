import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { teamService } from '../api/teamService';
import { TeamResponse } from '../types';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { Modal } from '../components/ui/Modal';
import { Input } from '../components/ui/Input';
import { TeamDetailModal } from '../components/teams/TeamDetailModal';
import { Users, Plus, Key, ShieldCheck, UserCheck, ChevronRight } from 'lucide-react';

export const TeamsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isJoinOpen, setIsJoinOpen] = useState(false);
  const [selectedTeam, setSelectedTeam] = useState<TeamResponse | null>(null);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [inviteCode, setInviteCode] = useState('');

  const { data: teams, isLoading } = useQuery({
    queryKey: ['myTeams'],
    queryFn: () => teamService.getMyTeams(),
  });

  const createMutation = useMutation({
    mutationFn: teamService.createTeam,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myTeams'] });
      setIsCreateOpen(false);
      setName('');
      setDescription('');
    },
  });

  const joinMutation = useMutation({
    mutationFn: teamService.joinTeamByInviteCode,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['myTeams'] });
      setIsJoinOpen(false);
      setInviteCode('');
    },
  });

  return (
    <div className="space-y-8 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
            Team Squads
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Collaborate, compete, and track daily activity with teammates
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <Button variant="outline" onClick={() => setIsJoinOpen(true)}>
            <Key className="w-4 h-4 mr-2" /> Join via Code
          </Button>
          <Button variant="primary" onClick={() => setIsCreateOpen(true)}>
            <Plus className="w-4 h-4 mr-2" /> Create Team
          </Button>
        </div>
      </div>

      {/* Teams Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {isLoading ? (
          <p className="text-sm text-slate-400">Loading your teams...</p>
        ) : teams && teams.length > 0 ? (
          teams.map((t) => (
            <Card
              key={t.id}
              onClick={() => setSelectedTeam(t)}
              className="p-6 flex flex-col justify-between cursor-pointer hover:border-brand-500 hover:shadow-lg transition-all group"
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <Badge variant="brand">
                    <Users className="w-3 h-3 mr-1" /> {t.memberCount} Members
                  </Badge>
                  <span className="text-xs font-mono font-bold bg-slate-100 dark:bg-slate-800 px-2.5 py-1 rounded-lg text-slate-600 dark:text-slate-300">
                    Code: {t.inviteCode}
                  </span>
                </div>

                <div className="flex items-center justify-between mb-1">
                  <h3 className="font-extrabold text-base text-slate-900 dark:text-slate-100 group-hover:text-brand-600 dark:group-hover:text-brand-400 transition-colors">
                    {t.name}
                  </h3>
                  <ChevronRight className="w-4 h-4 text-slate-400 group-hover:text-brand-500 group-hover:translate-x-1 transition-all" />
                </div>
                <p className="text-xs text-slate-600 dark:text-slate-400 mb-4">
                  {t.description || 'Social fitness squad for step challenges and activity tracking.'}
                </p>
              </div>

              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
                <span>Created: {t.createdAt ? new Date(t.createdAt).toLocaleDateString() : 'Active'}</span>
                <span className="text-xs font-bold text-brand-600 dark:text-brand-400 group-hover:underline">
                  View Squad & Admin →
                </span>
              </div>
            </Card>
          ))
        ) : (
          <div className="md:col-span-2 text-center py-16 text-slate-400 text-xs">
            You are not part of any team yet. Create a new team or join with an 8-character invite code!
          </div>
        )}
      </div>

      {/* Team Detail & Participants Modal */}
      <TeamDetailModal
        team={selectedTeam}
        isOpen={!!selectedTeam}
        onClose={() => setSelectedTeam(null)}
      />

      {/* Create Team Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Create Team Squad">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate({ name, description });
          }}
          className="space-y-4"
        >
          <Input
            label="Team Name"
            placeholder="e.g. Alpha Squad"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />
          <Input
            label="Description"
            placeholder="e.g. Engineering & Product fitness squad"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
          <Button type="submit" variant="primary" className="w-full mt-4" isLoading={createMutation.isPending}>
            Create Squad
          </Button>
        </form>
      </Modal>

      {/* Join Team Modal */}
      <Modal isOpen={isJoinOpen} onClose={() => setIsJoinOpen(false)} title="Join Team via Invite Code">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            joinMutation.mutate(inviteCode);
          }}
          className="space-y-4"
        >
          <Input
            label="8-Character Invite Code"
            placeholder="e.g. ALPHA123"
            value={inviteCode}
            onChange={(e) => setInviteCode(e.target.value.toUpperCase())}
            required
          />
          <Button type="submit" variant="primary" className="w-full mt-4" isLoading={joinMutation.isPending}>
            Join Squad
          </Button>
        </form>
      </Modal>
    </div>
  );
};

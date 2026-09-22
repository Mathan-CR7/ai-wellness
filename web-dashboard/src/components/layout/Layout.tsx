import React, { useState } from 'react';
import { Sidebar } from './Sidebar';
import { TopNav } from './TopNav';
import { MobileNav } from './MobileNav';
import { useWebSocket } from '../../context/WebSocketContext';
import { InactivityWellnessModal } from '../wellness/InactivityWellnessModal';
import { Sparkles, X, Activity } from 'lucide-react';
import { Button } from '../ui/Button';

export const Layout: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [collapsed, setCollapsed] = useState(false);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { latestAiSuggestion, dismissAiSuggestion } = useWebSocket();

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 flex flex-col md:flex-row antialiased text-slate-900 dark:text-slate-100">
      {/* Sidebar (Desktop) */}
      <Sidebar collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />

      {/* Main Content Workspace */}
      <div className="flex-1 flex flex-col min-w-0 pb-20 md:pb-8">
        <TopNav />

        {/* Global Floating AI Inactivity Banner */}
        {latestAiSuggestion && (
          <div className="mx-4 md:mx-8 mt-4 animate-slide-down z-30">
            <div className="p-4 rounded-2xl bg-gradient-to-r from-amber-500/10 via-purple-500/10 to-indigo-500/10 border border-amber-500/30 shadow-lg backdrop-blur-md flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div className="flex items-start space-x-3">
                <div className="p-2.5 rounded-xl bg-amber-500 text-white shadow-md shadow-amber-500/30">
                  <Sparkles className="w-5 h-5 animate-spin" />
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-extrabold text-xs uppercase tracking-wider text-amber-600 dark:text-amber-400">
                      ⚡ AI Wellness Alert ({latestAiSuggestion.inactivityMinutes} mins inactive)
                    </span>
                  </div>
                  <p className="font-semibold text-sm text-slate-800 dark:text-slate-100 mt-1">
                    {latestAiSuggestion.suggestion}
                  </p>
                </div>
              </div>

              <div className="flex items-center space-x-2 self-end sm:self-auto">
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => setIsModalOpen(true)}
                  className="shadow-md shadow-brand-500/20"
                >
                  <Activity className="w-3.5 h-3.5 mr-1.5" /> Start Movement Break
                </Button>
                <button
                  onClick={dismissAiSuggestion}
                  className="p-1.5 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Inactivity Wellness Modal */}
        <InactivityWellnessModal
          isOpen={isModalOpen || (!!latestAiSuggestion && false)}
          onClose={() => setIsModalOpen(false)}
          inactivityMinutes={latestAiSuggestion?.inactivityMinutes || 135}
          recommendation={latestAiSuggestion?.suggestion}
        />

        <main className="flex-1 p-4 md:p-8 max-w-7xl w-full mx-auto">{children}</main>
      </div>

      {/* Bottom Nav (Mobile) */}
      <MobileNav />
    </div>
  );
};

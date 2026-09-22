import React from 'react';

export type StretchType = 'neck' | 'shoulder' | 'side' | 'arm' | 'walking';

interface StretchAnimationProps {
  type: StretchType;
  className?: string;
}

export const StretchAnimation: React.FC<StretchAnimationProps> = ({ type, className = '' }) => {
  return (
    <div className={`relative w-56 h-56 mx-auto flex items-center justify-center p-4 bg-gradient-to-b from-slate-900/90 via-slate-900 to-slate-950 rounded-3xl border border-brand-500/30 shadow-2xl overflow-hidden ${className}`}>
      {/* Background Motion Glow */}
      <div className="absolute inset-0 bg-gradient-to-tr from-brand-500/10 via-emerald-500/5 to-teal-500/10 pointer-events-none" />

      {type === 'walking' && (
        <svg viewBox="0 0 120 120" className="w-44 h-44 drop-shadow-lg">
          <style>{`
            @keyframes adultWalkLeftLeg {
              0%, 100% { transform: rotate(-30deg); }
              50% { transform: rotate(30deg); }
            }
            @keyframes adultWalkRightLeg {
              0%, 100% { transform: rotate(30deg); }
              50% { transform: rotate(-30deg); }
            }
            @keyframes adultWalkLeftArm {
              0%, 100% { transform: rotate(25deg); }
              50% { transform: rotate(-25deg); }
            }
            @keyframes adultWalkRightArm {
              0%, 100% { transform: rotate(-25deg); }
              50% { transform: rotate(25deg); }
            }
            @keyframes adultTorsoBob {
              0%, 50%, 100% { transform: translateY(0px); }
              25%, 75% { transform: translateY(-4px); }
            }
            .left-leg-group { animation: adultWalkLeftLeg 1.1s infinite ease-in-out; transform-origin: 60px 70px; }
            .right-leg-group { animation: adultWalkRightLeg 1.1s infinite ease-in-out; transform-origin: 60px 70px; }
            .left-arm-group { animation: adultWalkLeftArm 1.1s infinite ease-in-out; transform-origin: 60px 42px; }
            .right-arm-group { animation: adultWalkRightArm 1.1s infinite ease-in-out; transform-origin: 60px 42px; }
            .upper-body-group { animation: adultTorsoBob 1.1s infinite ease-in-out; }
          `}</style>

          {/* Shadow Ground */}
          <ellipse cx="60" cy="110" rx="30" ry="4" fill="#000" opacity="0.4" />

          {/* Right Leg (Back) */}
          <g className="right-leg-group">
            <rect x="55" y="70" width="10" height="22" rx="4" fill="#1e293b" />
            <rect x="56" y="90" width="8" height="18" rx="3" fill="#334155" />
            <path d="M 54 106 L 68 106 C 70 106 72 108 72 110 L 52 110 Z" fill="#e2e8f0" />
          </g>

          {/* Left Leg (Front) */}
          <g className="left-leg-group">
            <rect x="55" y="70" width="10" height="22" rx="4" fill="#0f172a" />
            <rect x="56" y="90" width="8" height="18" rx="3" fill="#1e293b" />
            <path d="M 54 106 L 68 106 C 70 106 72 108 72 110 L 52 110 Z" fill="#10b981" />
          </g>

          {/* Torso & Head */}
          <g className="upper-body-group">
            {/* Athletic Shirt Torso */}
            <path d="M 48 40 L 72 40 L 68 70 L 52 70 Z" fill="#10b981" />
            <path d="M 48 40 L 60 46 L 72 40 L 68 70 L 52 70 Z" fill="#059669" opacity="0.3" />

            {/* Neck & Head */}
            <rect x="56" y="32" width="8" height="10" fill="#f87171" rx="2" />
            {/* Head */}
            <circle cx="60" cy="24" r="11" fill="#f87171" />
            {/* Short Hair */}
            <path d="M 50 20 C 52 11, 68 11, 70 20 C 66 14, 54 14, 50 20 Z" fill="#1e293b" />
          </g>

          {/* Right Arm (Back) */}
          <g className="right-arm-group">
            <rect x="56" y="42" width="7" height="18" rx="3" fill="#dc2626" />
            <rect x="57" y="58" width="6" height="16" rx="3" fill="#f87171" />
            <circle cx="60" cy="76" r="4" fill="#f87171" />
          </g>

          {/* Left Arm (Front) */}
          <g className="left-arm-group">
            <rect x="56" y="42" width="7" height="18" rx="3" fill="#10b981" />
            <rect x="57" y="58" width="6" height="16" rx="3" fill="#f87171" />
            <circle cx="60" cy="76" r="4" fill="#f87171" />
          </g>
        </svg>
      )}

      {type === 'shoulder' && (
        <svg viewBox="0 0 120 120" className="w-44 h-44 drop-shadow-lg">
          <style>{`
            @keyframes shoulderRotateCircle {
              0% { transform: rotate(0deg); }
              100% { transform: rotate(360deg); }
            }
            .shoulder-joint-left { animation: shoulderRotateCircle 2.2s infinite linear; transform-origin: 40px 45px; }
            .shoulder-joint-right { animation: shoulderRotateCircle 2.2s infinite linear; transform-origin: 80px 45px; }
          `}</style>

          {/* Torso & Head Facing Forward */}
          <path d="M 44 42 L 76 42 L 72 80 L 48 80 Z" fill="#10b981" />
          <rect x="56" y="32" width="8" height="12" fill="#f87171" />
          <circle cx="60" cy="24" r="11" fill="#f87171" />
          <path d="M 50 20 C 52 11, 68 11, 70 20 C 66 14, 54 14, 50 20 Z" fill="#1e293b" />

          {/* Shorts & Legs */}
          <rect x="46" y="80" width="28" height="15" fill="#1e293b" rx="2" />
          <rect x="49" y="95" width="9" height="18" fill="#f87171" rx="2" />
          <rect x="62" y="95" width="9" height="18" fill="#f87171" rx="2" />

          {/* Rotation Orbit Guide Circles */}
          <circle cx="40" cy="45" r="18" fill="none" stroke="#34d399" strokeWidth="1.5" strokeDasharray="4 4" opacity="0.6" />
          <circle cx="80" cy="45" r="18" fill="none" stroke="#34d399" strokeWidth="1.5" strokeDasharray="4 4" opacity="0.6" />

          {/* Rotating Left Arm */}
          <g className="shoulder-joint-left">
            <line x1="40" y1="45" x2="22" y2="45" stroke="#f87171" strokeWidth="6" strokeLinecap="round" />
            <circle cx="20" cy="45" r="4" fill="#f87171" />
          </g>

          {/* Rotating Right Arm */}
          <g className="shoulder-joint-right">
            <line x1="80" y1="45" x2="98" y2="45" stroke="#f87171" strokeWidth="6" strokeLinecap="round" />
            <circle cx="100" cy="45" r="4" fill="#f87171" />
          </g>
        </svg>
      )}

      {type === 'neck' && (
        <svg viewBox="0 0 120 120" className="w-44 h-44 drop-shadow-lg">
          <style>{`
            @keyframes adultNeckTilt {
              0%, 100% { transform: rotate(0deg); }
              25% { transform: rotate(-24deg); }
              75% { transform: rotate(24deg); }
            }
            .head-neck-tilt { animation: adultNeckTilt 3.2s infinite ease-in-out; transform-origin: 60px 60px; }
          `}</style>

          {/* Broad Shoulders */}
          <path d="M 30 85 C 38 65, 82 65, 90 85 L 90 110 L 30 110 Z" fill="#10b981" />
          <rect x="55" y="55" width="10" height="20" fill="#f87171" />

          {/* Tilting Head Assembly */}
          <g className="head-neck-tilt">
            <circle cx="60" cy="35" r="16" fill="#f87171" />
            {/* Hair */}
            <path d="M 44 32 C 46 16, 74 16, 76 32 C 70 24, 50 24, 44 32 Z" fill="#1e293b" />
            {/* Facial details */}
            <circle cx="55" cy="34" r="1.8" fill="#1e293b" />
            <circle cx="65" cy="34" r="1.8" fill="#1e293b" />
            <path d="M 57 42 Q 60 45 63 42" fill="none" stroke="#1e293b" strokeWidth="1.5" strokeLinecap="round" />

            {/* Neck Motion Arc Indicator */}
            <path d="M 40 20 Q 60 10 80 20" fill="none" stroke="#34d399" strokeWidth="2" strokeDasharray="3 3" />
          </g>
        </svg>
      )}

      {type === 'side' && (
        <svg viewBox="0 0 120 120" className="w-44 h-44 drop-shadow-lg">
          <style>{`
            @keyframes adultSideStretch {
              0%, 100% { transform: rotate(0deg); }
              25% { transform: rotate(-26deg); }
              75% { transform: rotate(26deg); }
            }
            .upper-torso-bend { animation: adultSideStretch 3.5s infinite ease-in-out; transform-origin: 60px 85px; }
          `}</style>

          {/* Legs & Shorts */}
          <rect x="46" y="85" width="28" height="15" fill="#1e293b" rx="2" />
          <rect x="47" y="98" width="9" height="18" fill="#f87171" rx="2" />
          <rect x="64" y="98" width="9" height="18" fill="#f87171" rx="2" />

          {/* Lateral Bending Upper Body */}
          <g className="upper-torso-bend">
            <path d="M 46 45 L 74 45 L 70 85 L 50 85 Z" fill="#10b981" />
            <rect x="56" y="34" width="8" height="12" fill="#f87171" />
            <circle cx="60" cy="24" r="11" fill="#f87171" />
            <path d="M 50 20 C 52 11, 68 11, 70 20 Z" fill="#1e293b" />

            {/* Reaching Side Stretch Arm Overhead */}
            <path d="M 74 45 C 90 20, 80 5, 55 10" fill="none" stroke="#f87171" strokeWidth="6" strokeLinecap="round" />
            <circle cx="53" cy="10" r="4" fill="#f87171" />
          </g>
        </svg>
      )}

      {type === 'arm' && (
        <svg viewBox="0 0 120 120" className="w-44 h-44 drop-shadow-lg">
          <style>{`
            @keyframes adultOverheadReach {
              0%, 100% { transform: translateY(0px) scaleY(1); }
              50% { transform: translateY(-10px) scaleY(1.08); }
            }
            .arms-overhead-group { animation: adultOverheadReach 2.2s infinite ease-in-out; transform-origin: 60px 60px; }
          `}</style>

          {/* Lower Body */}
          <rect x="48" y="80" width="24" height="15" fill="#1e293b" rx="2" />
          <rect x="49" y="95" width="8" height="20" fill="#f87171" rx="2" />
          <rect x="63" y="95" width="8" height="20" fill="#f87171" rx="2" />

          {/* Torso & Reaching Arms */}
          <path d="M 48 45 L 72 45 L 68 80 L 52 80 Z" fill="#10b981" />

          <g className="arms-overhead-group">
            <rect x="56" y="32" width="8" height="14" fill="#f87171" />
            <circle cx="60" cy="24" r="10" fill="#f87171" />
            <path d="M 50 20 C 52 11, 68 11, 70 20 Z" fill="#1e293b" />

            {/* Both Arms Reaching Straight Up Overhead */}
            <line x1="48" y1="45" x2="40" y2="8" stroke="#f87171" strokeWidth="6" strokeLinecap="round" />
            <line x1="72" y1="45" x2="80" y2="8" stroke="#f87171" strokeWidth="6" strokeLinecap="round" />
            {/* Clasping Hands Accent */}
            <circle cx="40" cy="7" r="4" fill="#f87171" />
            <circle cx="80" cy="7" r="4" fill="#f87171" />
            <line x1="40" y1="7" x2="80" y2="7" stroke="#34d399" strokeWidth="2" strokeDasharray="2 2" />
          </g>
        </svg>
      )}
    </div>
  );
};

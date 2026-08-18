import React from 'react';
import { AppTheme, THEMES } from '../utils/theme';
import { Palette, Sun, Moon, Sparkles, Eye, Shield } from 'lucide-react';

interface ThemeSelectorProps {
  currentTheme: AppTheme;
  onSelectTheme: (theme: AppTheme) => void;
  isOpen: boolean;
  onClose: () => void;
}

export const ThemeSelector: React.FC<ThemeSelectorProps> = ({
  currentTheme,
  onSelectTheme,
  isOpen,
  onClose,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-in fade-in">
      <div className="bg-slate-900 border border-slate-800 w-full max-w-md rounded-2xl shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800 bg-slate-950/50">
          <div className="flex items-center gap-2 font-bold text-slate-100 text-sm">
            <Palette className="w-4 h-4 text-cyan-400" />
            <span>High-Contrast Optical Palettes</span>
          </div>
          <button
            onClick={onClose}
            className="text-xs text-slate-400 hover:text-white px-2 py-1 rounded bg-slate-800/80"
          >
            Close
          </button>
        </div>

        <div className="p-4 space-y-2.5 text-xs">
          <p className="text-slate-400 leading-relaxed mb-3">
            Select high-contrast optical themes tuned for outdoor daylight glare, direct sunlight, or darkroom night vision.
          </p>

          {Object.values(THEMES).map((theme) => {
            const isSelected = currentTheme === theme.id;
            return (
              <button
                key={theme.id}
                onClick={() => {
                  onSelectTheme(theme.id);
                  onClose();
                }}
                className={`w-full p-3.5 rounded-xl border text-left flex items-center justify-between transition-all ${
                  isSelected
                    ? 'bg-slate-950 border-cyan-400 ring-1 ring-cyan-400 shadow-md shadow-cyan-500/20'
                    : 'bg-slate-950/60 border-slate-800 hover:border-slate-700'
                }`}
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-slate-100">{theme.name}</span>
                    {isSelected && (
                      <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-cyan-500/20 text-cyan-300 font-mono">
                        ACTIVE
                      </span>
                    )}
                  </div>
                  <div className="text-[11px] text-slate-400">{theme.desc}</div>
                </div>

                {/* Color swatches */}
                <div className="flex items-center gap-1.5 p-1 rounded-lg bg-slate-900 border border-slate-800">
                  <div
                    className="w-4 h-4 rounded-full border border-slate-700 shadow-sm"
                    style={{ backgroundColor: theme.qrBg }}
                  />
                  <div
                    className="w-4 h-4 rounded-full border border-slate-700 shadow-sm"
                    style={{ backgroundColor: theme.qrFg }}
                  />
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
};

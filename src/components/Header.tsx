import React from 'react';
import { WifiOff, Radio, RefreshCw, BookOpen, History, ShieldCheck } from 'lucide-react';

export type AppMode = 'sender' | 'receiver' | 'loopback';

interface HeaderProps {
  mode: AppMode;
  onSelectMode: (mode: AppMode) => void;
  onOpenDocs: () => void;
  onOpenHistory?: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  mode,
  onSelectMode,
  onOpenDocs,
  onOpenHistory,
}) => {
  return (
    <header className="border-b border-slate-800/80 bg-slate-950/80 backdrop-blur sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-3.5 flex flex-col sm:flex-row items-center justify-between gap-3">
        {/* Brand */}
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-cyan-950/80 border border-cyan-500/40 flex items-center justify-center text-cyan-400 shadow-lg shadow-cyan-950/50">
            <WifiOff className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-base font-bold tracking-tight text-white flex items-center gap-1.5">
                AirQR <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-cyan-500/20 text-cyan-300 font-mono border border-cyan-500/30">AIR2</span>
              </h1>
            </div>
            <p className="text-xs text-slate-400">Visual Optical Air-Gapped Transfer</p>
          </div>
        </div>

        {/* Mode Switcher */}
        <div className="flex items-center p-1 bg-slate-900/90 rounded-xl border border-slate-800">
          <button
            onClick={() => onSelectMode('sender')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
              mode === 'sender'
                ? 'bg-cyan-500 text-slate-950 shadow-md shadow-cyan-500/20'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            Transmitter
          </button>

          <button
            onClick={() => onSelectMode('receiver')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
              mode === 'receiver'
                ? 'bg-emerald-400 text-slate-950 shadow-md shadow-emerald-400/20'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Radio className="w-3.5 h-3.5" />
            Receiver
          </button>

          <button
            onClick={() => onSelectMode('loopback')}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition-all ${
              mode === 'loopback'
                ? 'bg-purple-400 text-slate-950 shadow-md shadow-purple-400/20'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Loopback
          </button>
        </div>

        {/* Actions */}
        <div className="flex items-center gap-2">
          {onOpenHistory && (
            <button
              onClick={onOpenHistory}
              className="p-2 text-slate-400 hover:text-slate-200 hover:bg-slate-900 rounded-lg border border-slate-800 text-xs flex items-center gap-1.5"
            >
              <History className="w-4 h-4" />
              <span className="hidden sm:inline">History</span>
            </button>
          )}
          <button
            onClick={onOpenDocs}
            className="p-2 text-cyan-400 hover:text-cyan-300 hover:bg-slate-900 rounded-lg border border-cyan-500/30 text-xs flex items-center gap-1.5 font-medium"
          >
            <BookOpen className="w-4 h-4" />
            <span className="hidden sm:inline">Protocol Specs</span>
          </button>
        </div>
      </div>
    </header>
  );
};

import React from 'react';
import { BatteryStatus } from '../types/transfer';
import { Battery, BatteryCharging, BatteryWarning, Zap, Leaf } from 'lucide-react';

interface BatteryIndicatorProps {
  status: BatteryStatus;
  batterySaver: boolean;
  onToggleBatterySaver?: () => void;
}

export const BatteryIndicator: React.FC<BatteryIndicatorProps> = ({
  status,
  batterySaver,
  onToggleBatterySaver,
}) => {
  const percent = status.supported ? Math.round(status.level * 100) : 85;

  return (
    <div className="flex items-center gap-2">
      {/* Battery Status Badge */}
      <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-slate-900 border border-slate-800 text-xs text-slate-300">
        {status.charging ? (
          <BatteryCharging className="w-3.5 h-3.5 text-emerald-400" />
        ) : status.isLow || percent <= 20 ? (
          <BatteryWarning className="w-3.5 h-3.5 text-amber-400 animate-pulse" />
        ) : (
          <Battery className="w-3.5 h-3.5 text-cyan-400" />
        )}
        <span className="font-mono text-[11px] font-semibold">{percent}%</span>
      </div>

      {/* Battery Saver Toggle Button */}
      {onToggleBatterySaver && (
        <button
          onClick={onToggleBatterySaver}
          title={batterySaver ? 'Battery Saver Active (Reduced brightness & FPS)' : 'Enable Battery Saver'}
          className={`flex items-center gap-1 px-2.5 py-1 rounded-lg border text-xs font-semibold transition-all ${
            batterySaver
              ? 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40 shadow-sm shadow-emerald-500/20'
              : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-slate-200'
          }`}
        >
          <Leaf className={`w-3.5 h-3.5 ${batterySaver ? 'text-emerald-400' : 'text-slate-400'}`} />
          <span>Saver {batterySaver ? 'ON' : 'OFF'}</span>
        </button>
      )}
    </div>
  );
};

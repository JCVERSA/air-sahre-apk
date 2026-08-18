import React from 'react';
import { BatteryStatus } from '../types/transfer';
import { Battery, BatteryCharging, BatteryWarning } from 'lucide-react';

interface BatteryIndicatorProps {
  status: BatteryStatus;
}

export const BatteryIndicator: React.FC<BatteryIndicatorProps> = ({ status }) => {
  if (!status.supported) return null;

  const percent = Math.round(status.level * 100);

  return (
    <div className="flex items-center gap-1.5 text-xs text-slate-400">
      {status.charging ? (
        <BatteryCharging className="w-4 h-4 text-emerald-400" />
      ) : status.isLow ? (
        <BatteryWarning className="w-4 h-4 text-rose-400" />
      ) : (
        <Battery className="w-4 h-4 text-cyan-400" />
      )}
      <span className="font-mono">{percent}%</span>
    </div>
  );
};

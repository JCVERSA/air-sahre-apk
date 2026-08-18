import React from 'react';
import { OpticalDiagnostics } from '../types/transfer';
import { Activity, Zap, Eye, Gauge } from 'lucide-react';

interface AdaptiveLinkHudProps {
  diagnostics: OpticalDiagnostics;
  instantSpeedKb: number;
}

export const AdaptiveLinkHud: React.FC<AdaptiveLinkHudProps> = ({
  diagnostics,
  instantSpeedKb,
}) => {
  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-3.5 text-xs">
      <div className="flex items-center justify-between mb-2.5">
        <span className="flex items-center gap-1.5 font-semibold text-slate-200">
          <Activity className="w-3.5 h-3.5 text-purple-400" />
          Optical Link HUD & Telemetry
        </span>
        <span className="font-mono font-bold text-emerald-400">
          Quality {diagnostics.linkQualityScore}/100
        </span>
      </div>

      <div className="grid grid-cols-3 gap-2">
        <div className="bg-slate-950/60 p-2 rounded-lg border border-slate-800/80">
          <div className="text-[10px] text-slate-400 flex items-center gap-1">
            <Gauge className="w-3 h-3 text-cyan-400" /> Speed
          </div>
          <div className="font-mono font-bold text-slate-100 mt-0.5">
            {instantSpeedKb.toFixed(1)} KB/s
          </div>
        </div>

        <div className="bg-slate-950/60 p-2 rounded-lg border border-slate-800/80">
          <div className="text-[10px] text-slate-400 flex items-center gap-1">
            <Zap className="w-3 h-3 text-amber-400" /> Suggested FPS
          </div>
          <div className="font-mono font-bold text-slate-100 mt-0.5">
            {diagnostics.suggestedFps} fps
          </div>
        </div>

        <div className="bg-slate-950/60 p-2 rounded-lg border border-slate-800/80">
          <div className="text-[10px] text-slate-400 flex items-center gap-1">
            <Eye className="w-3 h-3 text-emerald-400" /> Lighting
          </div>
          <div className="font-mono font-bold text-slate-100 mt-0.5">
            {diagnostics.lightingCondition}
          </div>
        </div>
      </div>
    </div>
  );
};

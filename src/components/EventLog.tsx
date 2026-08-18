import React from 'react';
import { LogEntry } from '../types/transfer';
import { Terminal } from 'lucide-react';

interface EventLogProps {
  logs: LogEntry[];
}

export const EventLog: React.FC<EventLogProps> = ({ logs }) => {
  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-3 text-xs">
      <div className="flex items-center gap-1.5 font-semibold text-slate-200 mb-2">
        <Terminal className="w-3.5 h-3.5 text-slate-400" />
        <span>Event Terminal ({logs.length})</span>
      </div>

      <div className="bg-slate-950/70 p-2 rounded-lg border border-slate-800/80 font-mono text-[10px] max-h-24 overflow-y-auto space-y-1">
        {logs.length === 0 ? (
          <div className="text-slate-500">No events logged yet.</div>
        ) : (
          logs.slice(0, 30).map((log) => {
            const color =
              log.level === 'success'
                ? 'text-emerald-400'
                : log.level === 'error'
                ? 'text-rose-400'
                : log.level === 'warn'
                ? 'text-amber-400'
                : 'text-cyan-400';

            return (
              <div key={log.id} className="flex gap-1.5">
                <span className="text-slate-500">[{log.timestamp.toLocaleTimeString()}]</span>
                <span className={color}>{log.message}</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

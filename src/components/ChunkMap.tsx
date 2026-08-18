import React from 'react';
import { Grid } from 'lucide-react';

interface ChunkMapProps {
  totalChunks: number;
  receivedIndices: Set<number> | number[];
  currentChunkIndex?: number | null;
  missingChunks?: number[];
}

export const ChunkMap: React.FC<ChunkMapProps> = ({
  totalChunks,
  receivedIndices,
  currentChunkIndex,
  missingChunks = [],
}) => {
  if (totalChunks <= 0) return null;

  const receivedSet = receivedIndices instanceof Set ? receivedIndices : new Set(receivedIndices);
  const count = receivedSet.size;
  const percent = Math.round((count / totalChunks) * 100);
  const displayLimit = Math.min(totalChunks, 140);

  return (
    <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-3 text-xs">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-1.5 font-semibold text-slate-200">
          <Grid className="w-3.5 h-3.5 text-cyan-400" />
          <span>Chunk Matrix ({count} / {totalChunks})</span>
        </div>
        <span className={`font-mono font-bold ${percent === 100 ? 'text-emerald-400' : 'text-cyan-400'}`}>
          {percent}%
        </span>
      </div>

      <div className="flex flex-wrap gap-1 max-h-24 overflow-y-auto p-1 bg-slate-950/60 rounded-lg border border-slate-800/80">
        {Array.from({ length: displayLimit }).map((_, i) => {
          const isReceived = receivedSet.has(i);
          const isCurrent = currentChunkIndex === i;
          const isMissing = missingChunks.includes(i);

          let bg = 'bg-slate-800';
          if (isCurrent) bg = 'bg-cyan-400 ring-1 ring-white';
          else if (isReceived) bg = 'bg-emerald-400';
          else if (isMissing) bg = 'bg-rose-500/70';

          return (
            <div
              key={i}
              className={`w-2.5 h-2.5 rounded-[2px] transition-colors ${bg}`}
              title={`Chunk #${i}`}
            />
          );
        })}
        {totalChunks > displayLimit && (
          <span className="text-[10px] text-slate-500 self-center px-1">
            +{totalChunks - displayLimit} more
          </span>
        )}
      </div>

      <div className="flex items-center gap-3 mt-2 text-[10px] text-slate-400">
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-[2px] bg-emerald-400" /> Received
        </div>
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-[2px] bg-cyan-400" /> Active
        </div>
        <div className="flex items-center gap-1">
          <span className="w-2 h-2 rounded-[2px] bg-slate-800" /> Pending
        </div>
        {missingChunks.length > 0 && (
          <div className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-[2px] bg-rose-500" /> Missing
          </div>
        )}
      </div>
    </div>
  );
};

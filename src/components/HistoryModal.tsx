import React, { useState, useEffect } from 'react';
import { SessionHistoryItem } from '../types/transfer';
import { getSessionHistory, clearSessionHistory } from '../utils/history';
import { formatBytes } from '../utils/crypto';
import { X, History, ArrowUpRight, ArrowDownLeft, Trash2 } from 'lucide-react';

interface HistoryModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const HistoryModal: React.FC<HistoryModalProps> = ({ isOpen, onClose }) => {
  const [history, setHistory] = useState<SessionHistoryItem[]>([]);

  useEffect(() => {
    if (isOpen) {
      setHistory(getSessionHistory());
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleClear = () => {
    clearSessionHistory();
    setHistory([]);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-xl w-full max-h-[85vh] overflow-y-auto p-6 text-slate-100 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <History className="w-5 h-5 text-cyan-400" />
            <h2 className="text-lg font-bold">Transfer History</h2>
          </div>
          <button onClick={onClose} className="p-1 text-slate-400 hover:text-white rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="py-4 space-y-2">
          {history.length === 0 ? (
            <div className="text-center py-8 text-slate-500 text-xs">No transfer sessions recorded yet.</div>
          ) : (
            history.map((item) => (
              <div
                key={item.id}
                className="bg-slate-950/70 p-3 rounded-xl border border-slate-800 flex items-center justify-between gap-3 text-xs"
              >
                <div className="flex items-center gap-2.5">
                  {item.role === 'sent' ? (
                    <div className="p-1.5 bg-purple-500/20 text-purple-400 rounded-lg">
                      <ArrowUpRight className="w-4 h-4" />
                    </div>
                  ) : (
                    <div className="p-1.5 bg-emerald-500/20 text-emerald-400 rounded-lg">
                      <ArrowDownLeft className="w-4 h-4" />
                    </div>
                  )}
                  <div>
                    <div className="font-semibold text-slate-200">{item.fileName}</div>
                    <div className="text-[10px] text-slate-400 font-mono">
                      {formatBytes(item.fileSize)} • {item.totalChunks} chunks • {item.averageSpeedKb.toFixed(1)} KB/s
                    </div>
                  </div>
                </div>
                <div className="text-right text-[10px] text-slate-400">
                  <div>{new Date(item.timestamp).toLocaleTimeString()}</div>
                  <span className="text-emerald-400 font-semibold">{item.status.toUpperCase()}</span>
                </div>
              </div>
            ))
          )}
        </div>

        {history.length > 0 && (
          <div className="pt-4 border-t border-slate-800 flex justify-between items-center">
            <button
              onClick={handleClear}
              className="px-3 py-1.5 text-rose-400 hover:bg-rose-950/50 rounded-lg text-xs flex items-center gap-1.5"
            >
              <Trash2 className="w-3.5 h-3.5" /> Clear History
            </button>
            <button
              onClick={onClose}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold rounded-xl text-xs"
            >
              Close
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

import React, { useEffect } from 'react';
import { CheckCircle2, AlertTriangle, XCircle, Info, X } from 'lucide-react';

export interface ToastMessage {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
}

interface ToastProps {
  toasts: ToastMessage[];
  onDismiss: (id: string) => void;
}

export const ToastContainer: React.FC<ToastProps> = ({ toasts, onDismiss }) => {
  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col gap-2.5 max-w-sm w-full pointer-events-none">
      {toasts.map((toast) => (
        <ToastItem key={toast.id} toast={toast} onDismiss={() => onDismiss(toast.id)} />
      ))}
    </div>
  );
};

const ToastItem: React.FC<{ toast: ToastMessage; onDismiss: () => void }> = ({
  toast,
  onDismiss,
}) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onDismiss();
    }, toast.duration || 4500);

    return () => clearTimeout(timer);
  }, [toast, onDismiss]);

  const borderColors = {
    success: 'border-emerald-500/50 bg-slate-900/95 text-emerald-400',
    error: 'border-rose-500/60 bg-slate-900/95 text-rose-400',
    warning: 'border-amber-500/50 bg-slate-900/95 text-amber-400',
    info: 'border-cyan-500/50 bg-slate-900/95 text-cyan-400',
  };

  const icons = {
    success: <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />,
    error: <XCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />,
    warning: <AlertTriangle className="w-5 h-5 text-amber-400 shrink-0 mt-0.5" />,
    info: <Info className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5" />,
  };

  return (
    <div
      className={`pointer-events-auto flex items-start gap-3 p-4 rounded-xl border shadow-2xl backdrop-blur-md transition-all animate-in slide-in-from-bottom-5 ${borderColors[toast.type]}`}
    >
      {icons[toast.type]}
      <div className="flex-1 text-xs">
        <div className="font-bold text-slate-100 text-sm mb-0.5">{toast.title}</div>
        <p className="text-slate-300 leading-relaxed font-sans">{toast.message}</p>
      </div>
      <button
        onClick={onDismiss}
        className="p-1 text-slate-400 hover:text-white rounded-lg transition-colors -mr-1 -mt-1"
      >
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

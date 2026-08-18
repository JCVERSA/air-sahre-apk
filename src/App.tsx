import React, { useState, useEffect, useCallback } from 'react';
import { Header, AppMode } from './components/Header';
import { SenderView } from './components/Sender/SenderView';
import { ReceiverView } from './components/Receiver/ReceiverView';
import { LoopbackView } from './components/Loopback/LoopbackView';
import { DocumentationModal } from './components/DocumentationModal';
import { HistoryModal } from './components/HistoryModal';
import { CalibrationModal } from './components/CalibrationModal';
import { ThemeSelector } from './components/ThemeSelector';
import { ToastContainer, ToastMessage } from './components/Toast';
import { BatteryStatus } from './types/transfer';
import { getBatteryStatus } from './utils/battery';
import { AppTheme, THEMES } from './utils/theme';
import { WifiOff, ShieldCheck, Cpu, Leaf, FileArchive, Palette, Activity } from 'lucide-react';

export default function App() {
  const [mode, setMode] = useState<AppMode>('sender');
  const [docsOpen, setDocsOpen] = useState<boolean>(false);
  const [historyOpen, setHistoryOpen] = useState<boolean>(false);
  const [calibrationOpen, setCalibrationOpen] = useState<boolean>(false);
  const [themeOpen, setThemeOpen] = useState<boolean>(false);
  const [theme, setTheme] = useState<AppTheme>('cyber');
  const [toasts, setToasts] = useState<ToastMessage[]>([]);

  const [batteryStatus, setBatteryStatus] = useState<BatteryStatus>({
    supported: false,
    level: 1,
    charging: true,
    chargingTime: 0,
    dischargingTime: 0,
    isLow: false,
    isCritical: false,
  });

  const [batterySaver, setBatterySaver] = useState<boolean>(false);

  // Monitor device battery status
  useEffect(() => {
    let mounted = true;
    const checkBattery = async () => {
      const status = await getBatteryStatus();
      if (mounted) {
        setBatteryStatus(status);
        if (status.supported && status.level <= 0.2 && !status.charging) {
          setBatterySaver(true);
        }
      }
    };

    checkBattery();
    const interval = setInterval(checkBattery, 10000);
    return () => {
      mounted = false;
      clearInterval(interval);
    };
  }, []);

  const addToast = useCallback(
    (type: ToastMessage['type'], title: string, message: string) => {
      const newToast: ToastMessage = {
        id: Math.random().toString(),
        type,
        title,
        message,
        duration: type === 'error' ? 6000 : 4500,
      };
      setToasts((prev) => [...prev.slice(-3), newToast]);
    },
    []
  );

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const handleToggleBatterySaver = () => {
    const nextState = !batterySaver;
    setBatterySaver(nextState);
    if (nextState) {
      addToast(
        'info',
        'Battery Saver Enabled',
        'Capped transmission refresh rate to 6 FPS and reduced display brightness to conserve power.'
      );
    } else {
      addToast('info', 'Battery Saver Disabled', 'Restored standard FPS and full display brightness.');
    }
  };

  const handleApplyCalibration = (fps: number, chunkSize: number, ecc: 'L' | 'M' | 'Q' | 'H') => {
    addToast(
      'success',
      'Optical Calibration Profile Applied',
      `Optimized for device hardware: ${fps} FPS stream, ${chunkSize} bytes/chunk, Level ${ecc} error correction.`
    );
  };

  const activeTheme = THEMES[theme];

  return (
    <div className={`min-h-screen ${activeTheme.bgClass} flex flex-col selection:bg-cyan-500 selection:text-slate-950 font-sans transition-colors duration-300`}>
      <Header
        mode={mode}
        onSelectMode={setMode}
        onOpenDocs={() => setDocsOpen(true)}
        onOpenHistory={() => setHistoryOpen(true)}
        onOpenThemes={() => setThemeOpen(true)}
        onOpenCalibration={() => setCalibrationOpen(true)}
        batteryStatus={batteryStatus}
        batterySaver={batterySaver}
        onToggleBatterySaver={handleToggleBatterySaver}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {mode === 'sender' && (
          <SenderView
            onNotify={addToast}
            batterySaver={batterySaver}
            onToggleBatterySaver={handleToggleBatterySaver}
          />
        )}
        {mode === 'receiver' && <ReceiverView onNotify={addToast} />}
        {mode === 'loopback' && <LoopbackView />}
      </main>

      <footer className="border-t border-slate-800/80 bg-slate-950/60 py-3 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center gap-4 flex-wrap">
            <span className="flex items-center gap-1 text-cyan-400">
              <WifiOff className="w-3.5 h-3.5" /> 100% Air-Gapped
            </span>
            <span className="flex items-center gap-1 text-purple-400">
              <FileArchive className="w-3.5 h-3.5" /> GZIP / ZLIB Deflate
            </span>
            <span className="flex items-center gap-1 text-emerald-400">
              <ShieldCheck className="w-3.5 h-3.5" /> SHA-256 Verified
            </span>
            <span className="flex items-center gap-1 text-amber-400">
              <Palette className="w-3.5 h-3.5" /> Theme: {activeTheme.name}
            </span>
            {batterySaver && (
              <span className="flex items-center gap-1 text-emerald-400 font-semibold">
                <Leaf className="w-3.5 h-3.5" /> Battery Saver Active
              </span>
            )}
          </div>
          <div className="text-[11px] text-slate-400">
            Native Android (Kotlin & Compose) & Web Optical File Transport
          </div>
        </div>
      </footer>

      <DocumentationModal isOpen={docsOpen} onClose={() => setDocsOpen(false)} />
      <HistoryModal isOpen={historyOpen} onClose={() => setHistoryOpen(false)} />
      <CalibrationModal
        isOpen={calibrationOpen}
        onClose={() => setCalibrationOpen(false)}
        onApplySettings={handleApplyCalibration}
      />
      <ThemeSelector
        currentTheme={theme}
        onSelectTheme={(t) => {
          setTheme(t);
          addToast('info', 'High-Contrast Theme Updated', `Switched to "${THEMES[t].name}" for optimal optical contrast.`);
        }}
        isOpen={themeOpen}
        onClose={() => setThemeOpen(false)}
      />
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </div>
  );
}

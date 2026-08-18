import React, { useState } from 'react';
import { Header, AppMode } from './components/Header';
import { SenderView } from './components/Sender/SenderView';
import { ReceiverView } from './components/Receiver/ReceiverView';
import { LoopbackView } from './components/Loopback/LoopbackView';
import { DocumentationModal } from './components/DocumentationModal';
import { HistoryModal } from './components/HistoryModal';
import { WifiOff, ShieldCheck, Cpu } from 'lucide-react';

export default function App() {
  const [mode, setMode] = useState<AppMode>('sender');
  const [docsOpen, setDocsOpen] = useState<boolean>(false);
  const [historyOpen, setHistoryOpen] = useState<boolean>(false);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-cyan-500 selection:text-slate-950 font-sans">
      <Header
        mode={mode}
        onSelectMode={setMode}
        onOpenDocs={() => setDocsOpen(true)}
        onOpenHistory={() => setHistoryOpen(true)}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {mode === 'sender' && <SenderView />}
        {mode === 'receiver' && <ReceiverView />}
        {mode === 'loopback' && <LoopbackView />}
      </main>

      <footer className="border-t border-slate-800/80 bg-slate-950/60 py-3 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-2">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1 text-cyan-400">
              <WifiOff className="w-3.5 h-3.5" /> 100% Air-Gapped
            </span>
            <span className="flex items-center gap-1 text-purple-400">
              <Cpu className="w-3.5 h-3.5" /> Base45 RFC 9285
            </span>
            <span className="flex items-center gap-1 text-emerald-400">
              <ShieldCheck className="w-3.5 h-3.5" /> SHA-256 Verified
            </span>
          </div>
          <div className="text-[11px] text-slate-400">
            Native Android (Kotlin & Compose) & Web Optical File Transport
          </div>
        </div>
      </footer>

      <DocumentationModal isOpen={docsOpen} onClose={() => setDocsOpen(false)} />
      <HistoryModal isOpen={historyOpen} onClose={() => setHistoryOpen(false)} />
    </div>
  );
}

import React from 'react';
import { X, ShieldCheck, WifiOff, Zap, Lock } from 'lucide-react';

interface DocumentationModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const DocumentationModal: React.FC<DocumentationModalProps> = ({
  isOpen,
  onClose,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-sm">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-2xl w-full max-h-[85vh] overflow-y-auto p-6 text-slate-100 shadow-2xl">
        <div className="flex items-center justify-between pb-4 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <ShieldCheck className="w-5 h-5 text-cyan-400" />
            <h2 className="text-lg font-bold">AirQR Protocol & Architecture</h2>
          </div>
          <button onClick={onClose} className="p-1 text-slate-400 hover:text-white rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="py-4 space-y-4 text-xs text-slate-300 leading-relaxed">
          <div className="flex gap-3">
            <WifiOff className="w-5 h-5 text-cyan-400 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-semibold text-white text-sm">100% Air-Gapped Optical P2P</h3>
              <p className="text-slate-400 mt-1">
                Zero radio frequencies used (No Wi-Fi, No Bluetooth, No NFC, No Cellular). Data is encoded into rapid visual QR codes displayed on screen and captured by camera sensors.
              </p>
            </div>
          </div>

          <div className="flex gap-3">
            <Zap className="w-5 h-5 text-purple-400 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-semibold text-white text-sm">Base45 RFC 9285 & Gzip Compression</h3>
              <p className="text-slate-400 mt-1">
                AIR2 encodes binary chunks into QR Alphanumeric Mode using Base45, yielding ~45% smaller matrix density and allowing 15–25 FPS optical streaming without motion blur.
              </p>
            </div>
          </div>

          <div className="flex gap-3">
            <Lock className="w-5 h-5 text-emerald-400 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-semibold text-white text-sm">SHA-256 Cryptographic Checksum</h3>
              <p className="text-slate-400 mt-1">
                Prior to transmission, the sender calculates a SHA-256 hash. The receiver verifies this hash byte-for-byte upon reassembling all chunks.
              </p>
            </div>
          </div>

          <div className="bg-slate-950 p-3 rounded-xl border border-slate-800 font-mono text-[11px] space-y-1">
            <div className="text-cyan-400">AIR2:M:&lt;id&gt;:&lt;size&gt;:&lt;chunks&gt;:&lt;chunkSize&gt;:&lt;sha256&gt;:&lt;name&gt;:&lt;type&gt;:&lt;comp&gt;:&lt;compSize&gt;:&lt;enc&gt;</div>
            <div className="text-emerald-400">AIR2:C:&lt;id&gt;:&lt;index&gt;:&lt;total&gt;:&lt;enc&gt;:&lt;payload&gt;</div>
            <div className="text-purple-400">AIR2:FB:&lt;id&gt;:&lt;quality&gt;:&lt;failureRate&gt;:&lt;lighting&gt;:&lt;fps&gt;:&lt;chunkSize&gt;:&lt;ecc&gt;:&lt;missing&gt;</div>
          </div>
        </div>

        <div className="pt-4 border-t border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-semibold rounded-xl text-xs"
          >
            Close Specifications
          </button>
        </div>
      </div>
    </div>
  );
};

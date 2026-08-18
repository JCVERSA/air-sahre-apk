import React, { useState, useEffect, useRef } from 'react';
import {
  ReceiverState,
  ReceiverConfig,
  LogEntry,
  TransferMeta,
} from '../../types/transfer';
import { parsePacket } from '../../utils/protocol';
import { base45ToUint8Array, decompressData } from '../../utils/base45';
import { computeSHA256, formatBytes, soundEffects } from '../../utils/crypto';
import { scanQRFromCanvas } from '../../utils/qr';
import { addSessionHistoryItem } from '../../utils/history';
import { OpticalQualityTracker } from '../../utils/opticalQuality';
import { ChunkMap } from '../ChunkMap';
import { AdaptiveLinkHud } from '../AdaptiveLinkHud';
import { EventLog } from '../EventLog';
import {
  Camera,
  CheckCircle,
  AlertCircle,
  Download,
  Share2,
  RefreshCw,
  Eye,
  Radio,
} from 'lucide-react';

const DEFAULT_RECEIVER_STATE: ReceiverState = {
  status: 'idle',
  transferId: null,
  meta: null,
  receivedChunks: new Map(),
  totalChunks: 0,
  receivedCount: 0,
  startTime: null,
  lastChunkTime: null,
  bytesReceived: 0,
  computedHash: null,
  isHashValid: null,
  reconstructedBlob: null,
  reconstructedUrl: null,
  errorMessage: null,
  recoveryHint: null,
  fpsDetected: 0,
  scanLatencyMs: 0,
  uniqueFrameRate: 0,
  diagnostics: {
    luminance: 128,
    contrast: 75,
    lightingCondition: 'GOOD',
    failureRate: 0,
    successRate: 100,
    linkQualityScore: 90,
    consecutiveFailures: 0,
    suggestedFps: 14,
    suggestedChunkSize: 700,
    suggestedEcc: 'M',
    recommendedAction: 'Align camera with sender screen',
  },
};

export const ReceiverView: React.FC = () => {
  const [state, setState] = useState<ReceiverState>(DEFAULT_RECEIVER_STATE);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [cameraActive, setCameraActive] = useState<boolean>(false);
  const [instantSpeedKb, setInstantSpeedKb] = useState<number>(0);

  const videoRef = useRef<HTMLVideoElement | null>(null);
  const hiddenCanvasRef = useRef<HTMLCanvasElement | null>(null);
  const trackerRef = useRef<OpticalQualityTracker>(new OpticalQualityTracker());
  const lastScannedTextRef = useRef<string | null>(null);

  const addLog = (level: LogEntry['level'], message: string) => {
    setLogs((prev) => [{ id: Math.random().toString(), timestamp: new Date(), level, message }, ...prev.slice(0, 40)]);
  };

  const startCamera = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment', width: { ideal: 1280 }, height: { ideal: 720 } },
      });
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        videoRef.current.play();
        setCameraActive(true);
        setState((prev) => ({ ...prev, status: 'scanning' }));
        addLog('info', 'Optical camera initialized. Scanning for QR stream...');
      }
    } catch (err: any) {
      addLog('error', `Camera access denied or unavailable: ${err.message}`);
    }
  };

  const stopCamera = () => {
    if (videoRef.current?.srcObject) {
      const stream = videoRef.current.srcObject as MediaStream;
      stream.getTracks().forEach((track) => track.stop());
      videoRef.current.srcObject = null;
    }
    setCameraActive(false);
  };

  useEffect(() => {
    startCamera();
    return () => stopCamera();
  }, []);

  // Frame scanner loop
  useEffect(() => {
    if (!cameraActive) return;

    let animId: number;

    const processFrame = () => {
      const video = videoRef.current;
      const canvas = hiddenCanvasRef.current;

      if (video && canvas && video.readyState === video.HAVE_ENOUGH_DATA) {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });

        if (ctx) {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const scannedText = scanQRFromCanvas(canvas, 0.8);

          if (scannedText) {
            trackerRef.current.recordSuccess();
            handleDecodedData(scannedText);
          } else {
            trackerRef.current.recordFailure();
          }

          setState((prev) => ({
            ...prev,
            diagnostics: trackerRef.current.getDiagnostics(),
          }));
        }
      }

      animId = requestAnimationFrame(processFrame);
    };

    animId = requestAnimationFrame(processFrame);
    return () => cancelAnimationFrame(animId);
  }, [cameraActive, state]);

  const handleDecodedData = async (text: string) => {
    if (text === lastScannedTextRef.current && state.status === 'receiving') {
      return; // Skip duplicate frame
    }
    lastScannedTextRef.current = text;

    const packet = parsePacket(text);
    if (!packet) return;

    if (packet.type === 'META' && packet.meta) {
      const meta = packet.meta;
      if (state.meta?.id !== meta.id) {
        setState((prev) => ({
          ...prev,
          status: 'receiving',
          meta,
          totalChunks: meta.totalChunks,
          transferId: meta.id,
          startTime: Date.now(),
        }));
        addLog('info', `Detected optical stream: ${meta.name} (${formatBytes(meta.size)})`);
      }
    }

    if (packet.type === 'CHUNK' && packet.chunk) {
      const chunk = packet.chunk;
      const chunksMap = new Map<number, Uint8Array>(state.receivedChunks);

      if (!chunksMap.has(chunk.index)) {
        soundEffects.playChunkTick();
        const chunkBytes = base45ToUint8Array(chunk.data);
        chunksMap.set(chunk.index, chunkBytes);

        const count = chunksMap.size;
        const total = chunk.total;
        const elapsedSec = Math.max(0.1, (Date.now() - (state.startTime || Date.now())) / 1000);
        const totalBytesReceived = Array.from(chunksMap.values()).reduce((acc: number, cur: Uint8Array) => acc + cur.byteLength, 0);
        const speedKb = totalBytesReceived / 1024 / elapsedSec;
        setInstantSpeedKb(speedKb);

        setState((prev) => ({
          ...prev,
          status: 'receiving',
          receivedChunks: chunksMap,
          receivedCount: count,
          totalChunks: total,
          bytesReceived: totalBytesReceived,
          lastChunkTime: Date.now(),
        }));

        if (count >= total && state.meta) {
          verifyAndReconstruct(state.meta, chunksMap);
        }
      }
    }
  };

  const verifyAndReconstruct = async (meta: TransferMeta, chunks: Map<number, Uint8Array>) => {
    setState((prev) => ({ ...prev, status: 'verifying' }));
    addLog('info', `All ${meta.totalChunks} chunks received. Reconstructing payload...`);

    const totalLen = Array.from(chunks.values()).reduce((acc, c) => acc + c.byteLength, 0);
    const combined = new Uint8Array(totalLen);
    let offset = 0;
    for (let i = 0; i < meta.totalChunks; i++) {
      const chunk = chunks.get(i);
      if (chunk) {
        combined.set(chunk, offset);
        offset += chunk.byteLength;
      }
    }

    const decompressed = meta.compressed ? await decompressData(combined) : combined;
    const computedSha = await computeSHA256(decompressed);
    const isValid = computedSha.toLowerCase() === meta.hash.toLowerCase();

    if (isValid) {
      soundEffects.playSuccess();
      const blob = new Blob([decompressed as unknown as BlobPart], { type: meta.type || 'application/octet-stream' });
      const url = URL.createObjectURL(blob);

      setState((prev) => ({
        ...prev,
        status: 'completed',
        computedHash: computedSha,
        isHashValid: true,
        reconstructedBlob: blob,
        reconstructedUrl: url,
      }));

      addLog('success', `SHA-256 Checksum Passed! ${meta.name} ready for download.`);

      addSessionHistoryItem({
        id: Math.random().toString(),
        transferId: meta.id,
        fileName: meta.name,
        fileSize: meta.size,
        fileType: meta.type,
        role: 'received',
        timestamp: Date.now(),
        hash: computedSha,
        totalChunks: meta.totalChunks,
        durationSeconds: Math.max(0.1, (Date.now() - (state.startTime || Date.now())) / 1000),
        averageSpeedKb: (meta.size / 1024) / Math.max(0.1, (Date.now() - (state.startTime || Date.now())) / 1000),
        status: 'success',
        downloadUrl: url,
      });
    } else {
      soundEffects.playError();
      setState((prev) => ({
        ...prev,
        status: 'error',
        computedHash: computedSha,
        isHashValid: false,
        errorMessage: 'SHA-256 Checksum Mismatch! Transmission corrupted.',
      }));
      addLog('error', `SHA-256 Checksum FAILED! Expected: ${meta.hash}, Got: ${computedSha}`);
    }
  };

  const resetReceiver = () => {
    setState(DEFAULT_RECEIVER_STATE);
    setInstantSpeedKb(0);
    lastScannedTextRef.current = null;
    addLog('info', 'Receiver reset to idle state.');
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      {/* Left Column: Viewfinder & Camera */}
      <div className="lg:col-span-7 flex flex-col items-center">
        <div className="w-full bg-slate-900 border border-slate-800 rounded-2xl p-5 flex flex-col items-center shadow-xl">
          {/* Header */}
          <div className="w-full flex items-center justify-between pb-3 mb-4 border-b border-slate-800 text-xs">
            <div className="flex items-center gap-2 font-semibold text-slate-200">
              <Camera className="w-4 h-4 text-emerald-400" />
              <span>Optical Receiver</span>
            </div>
            <button
              onClick={resetReceiver}
              className="px-2 py-1 text-slate-400 hover:text-white bg-slate-950 rounded-lg border border-slate-800 flex items-center gap-1 text-xs"
            >
              <RefreshCw className="w-3.5 h-3.5" /> Reset
            </button>
          </div>

          {/* Camera Viewfinder */}
          <div className="relative w-full max-w-[420px] aspect-square bg-black rounded-2xl overflow-hidden border-2 border-emerald-500/40 shadow-inner flex items-center justify-center">
            <video ref={videoRef} className="w-full h-full object-cover" playsInline muted />
            <canvas ref={hiddenCanvasRef} className="hidden" />

            {/* Viewfinder Target Reticle */}
            <div className="absolute inset-12 border-2 border-dashed border-emerald-400/60 rounded-xl pointer-events-none" />

            {/* Overlay */}
            <div className="absolute bottom-2 inset-x-2 bg-slate-950/85 backdrop-blur rounded-lg px-3 py-1.5 text-[11px] font-mono flex items-center justify-between text-slate-200">
              <span className="font-bold text-emerald-400">
                {state.status === 'receiving'
                  ? `CHUNKS ${state.receivedChunks.size} / ${state.totalChunks}`
                  : 'ALIGN CAMERA WITH QR'}
              </span>
              <span className="text-slate-400">{instantSpeedKb.toFixed(1)} KB/s</span>
            </div>
          </div>

          {/* Download Box */}
          {state.status === 'completed' && state.reconstructedUrl && (
            <div className="w-full mt-4 p-4 bg-emerald-950/40 border border-emerald-500/40 rounded-xl flex items-center justify-between">
              <div>
                <div className="flex items-center gap-1.5 text-emerald-400 font-bold text-xs">
                  <CheckCircle className="w-4 h-4" /> Transfer Verified (SHA-256 Match)
                </div>
                <div className="text-xs text-slate-300 mt-0.5">{state.meta?.name}</div>
              </div>
              <a
                href={state.reconstructedUrl}
                download={state.meta?.name || 'airqr_download'}
                className="px-4 py-2 bg-emerald-400 hover:bg-emerald-300 text-slate-950 font-bold rounded-lg text-xs flex items-center gap-1.5 shadow"
              >
                <Download className="w-3.5 h-3.5" /> Save File
              </a>
            </div>
          )}
        </div>
      </div>

      {/* Right Column: Chunk Map, HUD & Log */}
      <div className="lg:col-span-5 space-y-4">
        {state.totalChunks > 0 && (
          <ChunkMap
            totalChunks={state.totalChunks}
            receivedIndices={state.receivedChunks.keys()}
          />
        )}

        <AdaptiveLinkHud
          diagnostics={state.diagnostics}
          instantSpeedKb={instantSpeedKb}
        />

        <EventLog logs={logs} />
      </div>
    </div>
  );
};

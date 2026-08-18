import React, { useState, useEffect, useRef } from 'react';
import {
  SenderConfig,
  TransferMeta,
  ChunkPayload,
  LogEntry,
  QrDensityPreset,
} from '../../types/transfer';
import { computeSHA256, formatBytes, generateTransferId } from '../../utils/crypto';
import { uint8ArrayToBase45, compressData } from '../../utils/base45';
import { encodeMetaPacket, encodeChunkPacket } from '../../utils/protocol';
import { renderQRToCanvas, preRenderQRCache } from '../../utils/qr';
import { addSessionHistoryItem } from '../../utils/history';
import { ChunkMap } from '../ChunkMap';
import { EventLog } from '../EventLog';
import {
  Upload,
  Play,
  Pause,
  ChevronLeft,
  ChevronRight,
  Sliders,
  Maximize2,
  FileText,
  Radio,
} from 'lucide-react';

const DEFAULT_CONFIG: SenderConfig = {
  chunkSize: 700,
  fps: 14,
  qrSize: 420,
  errorCorrection: 'M',
  brightness: 100,
  contrast: 100,
  invertColor: false,
  metaFrequency: 8,
  useCompression: true,
  encodingMode: 'base45',
  enablePreRendering: true,
  densityPreset: 'medium',
  adaptiveRateControl: true,
  minFps: 3,
  maxFps: 22,
  minChunkSize: 180,
  maxChunkSize: 1150,
};

export const SenderView: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [meta, setMeta] = useState<TransferMeta | null>(null);
  const [encodedPackets, setEncodedPackets] = useState<string[]>([]);
  const [preRenderedCanvases, setPreRenderedCanvases] = useState<HTMLCanvasElement[]>([]);
  const [currentFrameIndex, setCurrentFrameIndex] = useState<number>(0);
  const [isPlaying, setIsPlaying] = useState<boolean>(true);
  const [config, setConfig] = useState<SenderConfig>(DEFAULT_CONFIG);
  const [showSettings, setShowSettings] = useState<boolean>(false);
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);

  const displayCanvasRef = useRef<HTMLCanvasElement | null>(null);

  const addLog = (level: LogEntry['level'], message: string) => {
    setLogs((prev) => [{ id: Math.random().toString(), timestamp: new Date(), level, message }, ...prev.slice(0, 40)]);
  };

  // Prepare default sample payload
  useEffect(() => {
    if (!file && encodedPackets.length === 0) {
      const sampleText = `AirQR Optical Protocol V2 Payload\n=================================\nThis is a secure air-gapped file transfer beamed across screens via high-density Base45 optical QR codes.\nRFC 9285 Base45 + GZIP Compression + SHA-256 Checksum.\n100% offline visual communication.`;
      const blob = new Blob([sampleText], { type: 'text/plain' });
      const sampleFile = new File([blob], 'airqr_sample_memo.txt', { type: 'text/plain' });
      processFile(sampleFile);
    }
  }, []);

  const processFile = async (f: File) => {
    setIsProcessing(true);
    setFile(f);
    addLog('info', `Encoding payload: ${f.name} (${formatBytes(f.size)})`);

    const arrayBuffer = await f.arrayBuffer();
    const rawBytes = new Uint8Array(arrayBuffer);
    const sha256 = await computeSHA256(rawBytes);

    const { compressed, wasCompressed } = config.useCompression
      ? await compressData(rawBytes)
      : { compressed: rawBytes, wasCompressed: false };

    const chunkSize = config.chunkSize;
    const rawChunks: Uint8Array[] = [];
    let offset = 0;
    while (offset < compressed.length) {
      const end = Math.min(offset + chunkSize, compressed.length);
      rawChunks.push(compressed.slice(offset, end));
      offset = end;
    }

    const transferId = generateTransferId();
    const transferMeta: TransferMeta = {
      id: transferId,
      name: f.name,
      size: f.size,
      type: f.type || 'text/plain',
      totalChunks: rawChunks.length,
      chunkSize,
      hash: sha256,
      compressed: wasCompressed,
      compressedSize: compressed.length,
      encoding: config.encodingMode,
      timestamp: Date.now(),
    };

    const packets: string[] = [];
    const metaStr = encodeMetaPacket(transferMeta);

    for (let i = 0; i < rawChunks.length; i++) {
      const chunkData = uint8ArrayToBase45(rawChunks[i]);
      const chunkPayload: ChunkPayload = {
        id: transferId,
        index: i,
        total: rawChunks.length,
        data: chunkData,
        encoding: config.encodingMode,
      };

      if (i % config.metaFrequency === 0) {
        packets.push(metaStr);
      }
      packets.push(encodeChunkPacket(chunkPayload));
    }
    packets.push(metaStr);

    const canvases = await preRenderQRCache(packets, {
      size: config.qrSize,
      errorCorrection: config.errorCorrection,
      invert: config.invertColor,
    });

    setMeta(transferMeta);
    setEncodedPackets(packets);
    setPreRenderedCanvases(canvases);
    setCurrentFrameIndex(0);
    setIsProcessing(false);
    addLog('success', `Stream ready: ${rawChunks.length} chunks (${packets.length} optical frames)`);

    addSessionHistoryItem({
      id: Math.random().toString(),
      transferId,
      fileName: f.name,
      fileSize: f.size,
      fileType: f.type || 'application/octet-stream',
      role: 'sent',
      timestamp: Date.now(),
      hash: sha256,
      totalChunks: rawChunks.length,
      durationSeconds: rawChunks.length / config.fps,
      averageSpeedKb: (f.size / 1024) / Math.max(0.1, rawChunks.length / config.fps),
      status: 'success',
    });
  };

  // Animation Loop
  useEffect(() => {
    if (!isPlaying || preRenderedCanvases.length === 0) return;

    const interval = setInterval(() => {
      setCurrentFrameIndex((prev) => (prev + 1) % preRenderedCanvases.length);
    }, 1000 / config.fps);

    return () => clearInterval(interval);
  }, [isPlaying, preRenderedCanvases, config.fps]);

  // Paint active canvas frame
  useEffect(() => {
    const canvas = displayCanvasRef.current;
    const source = preRenderedCanvases[currentFrameIndex];
    if (canvas && source) {
      canvas.width = source.width;
      canvas.height = source.height;
      const ctx = canvas.getContext('2d');
      ctx?.drawImage(source, 0, 0);
    }
  }, [currentFrameIndex, preRenderedCanvases]);

  const isMetaFrame = encodedPackets[currentFrameIndex]?.startsWith('AIR2:M');

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      {/* Left Column: QR Code Display */}
      <div className="lg:col-span-7 flex flex-col items-center">
        <div className="w-full bg-slate-900 border border-slate-800 rounded-2xl p-5 flex flex-col items-center shadow-xl">
          {/* Header */}
          <div className="w-full flex items-center justify-between pb-3 mb-4 border-b border-slate-800 text-xs">
            <div className="flex items-center gap-2 font-semibold text-slate-200">
              <Radio className="w-4 h-4 text-cyan-400" />
              <span>Optical Transmitter</span>
            </div>
            <div className="flex items-center gap-2">
              <label className="cursor-pointer px-2.5 py-1 bg-cyan-500/20 hover:bg-cyan-500/30 text-cyan-300 font-semibold rounded-lg border border-cyan-500/30 flex items-center gap-1.5 transition-colors">
                <Upload className="w-3.5 h-3.5" /> Select File
                <input
                  type="file"
                  className="hidden"
                  onChange={(e) => e.target.files?.[0] && processFile(e.target.files[0])}
                />
              </label>
            </div>
          </div>

          {/* QR Canvas Stage */}
          <div className="relative p-4 bg-white rounded-2xl shadow-inner flex items-center justify-center border-4 border-cyan-500/40">
            <canvas ref={displayCanvasRef} className="max-w-full h-auto rounded-lg" />

            <div className="absolute bottom-2 inset-x-2 bg-slate-950/80 backdrop-blur rounded-lg px-3 py-1 text-[11px] font-mono flex items-center justify-between text-slate-200">
              <span className={isMetaFrame ? 'text-purple-400 font-bold' : 'text-cyan-400 font-bold'}>
                {isMetaFrame ? 'METADATA PACKET' : `FRAME #${currentFrameIndex + 1} / ${encodedPackets.length}`}
              </span>
              <span className="text-slate-400">{config.fps} FPS • BASE45</span>
            </div>
          </div>

          {/* Controls Bar */}
          <div className="w-full mt-5 bg-slate-950/70 p-3 rounded-xl border border-slate-800 flex flex-col gap-3">
            <input
              type="range"
              min={0}
              max={Math.max(0, encodedPackets.length - 1)}
              value={currentFrameIndex}
              onChange={(e) => setCurrentFrameIndex(parseInt(e.target.value, 10))}
              className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded-lg cursor-pointer"
            />

            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setCurrentFrameIndex((prev) => (prev - 1 + encodedPackets.length) % encodedPackets.length)}
                  className="p-1.5 text-slate-300 hover:text-white bg-slate-900 rounded-lg border border-slate-800"
                >
                  <ChevronLeft className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setIsPlaying(!isPlaying)}
                  className="px-4 py-1.5 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold rounded-lg text-xs flex items-center gap-1.5"
                >
                  {isPlaying ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
                  {isPlaying ? 'Pause' : 'Play'}
                </button>
                <button
                  onClick={() => setCurrentFrameIndex((prev) => (prev + 1) % encodedPackets.length)}
                  className="p-1.5 text-slate-300 hover:text-white bg-slate-900 rounded-lg border border-slate-800"
                >
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>

              <button
                onClick={() => setShowSettings(!showSettings)}
                className="px-2.5 py-1 text-slate-400 hover:text-white bg-slate-900 rounded-lg border border-slate-800 text-xs flex items-center gap-1.5"
              >
                <Sliders className="w-3.5 h-3.5" /> Parameters
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Right Column: Telemetry, Chunk Map & Event Log */}
      <div className="lg:col-span-5 space-y-4">
        {meta && (
          <ChunkMap
            totalChunks={meta.totalChunks}
            receivedIndices={Array.from({ length: meta.totalChunks }, (_, i) => i)}
            currentChunkIndex={currentFrameIndex % meta.totalChunks}
          />
        )}

        {showSettings && (
          <div className="bg-slate-900 border border-slate-800 rounded-xl p-4 text-xs space-y-3">
            <h4 className="font-semibold text-slate-200">Transmission Parameters</h4>
            <div>
              <div className="flex justify-between text-slate-400 mb-1">
                <span>Speed: {config.fps} FPS</span>
              </div>
              <input
                type="range"
                min={1}
                max={30}
                value={config.fps}
                onChange={(e) => setConfig({ ...config, fps: parseInt(e.target.value, 10) })}
                className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded cursor-pointer"
              />
            </div>
            <div>
              <div className="flex justify-between text-slate-400 mb-1">
                <span>Chunk Size: {config.chunkSize} bytes</span>
              </div>
              <input
                type="range"
                min={150}
                max={1200}
                step={50}
                value={config.chunkSize}
                onChange={(e) => {
                  const sz = parseInt(e.target.value, 10);
                  setConfig({ ...config, chunkSize: sz });
                  if (file) processFile(file);
                }}
                className="w-full accent-cyan-400 h-1.5 bg-slate-800 rounded cursor-pointer"
              />
            </div>
          </div>
        )}

        <EventLog logs={logs} />
      </div>
    </div>
  );
};

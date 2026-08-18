export interface TransferMeta {
  id: string;
  name: string;
  size: number;
  type: string;
  totalChunks: number;
  chunkSize: number;
  hash: string;
  timestamp: number;
  compressed?: boolean;
  compressedSize?: number;
  encoding?: 'base45' | 'base64';
}

export interface ChunkPayload {
  id: string;
  index: number;
  total: number;
  data: string;
  encoding?: 'base45' | 'base64';
}

export interface OpticalFeedbackPayload {
  transferId: string;
  qualityScore: number;
  failureRate: number;
  lighting: 'EXCELLENT' | 'GOOD' | 'LOW_LIGHT' | 'HIGH_GLARE' | 'BLUR';
  recommendedFps: number;
  recommendedChunkSize: number;
  recommendedEcc: 'L' | 'M' | 'Q' | 'H';
  missingChunks: number[];
}

export type PacketType = 'META' | 'CHUNK' | 'FEEDBACK';

export interface Packet {
  version: number;
  type: PacketType;
  meta?: TransferMeta;
  chunk?: ChunkPayload;
  feedback?: OpticalFeedbackPayload;
}

export type QrDensityPreset =
  | 'ultra_low'
  | 'low'
  | 'medium'
  | 'high'
  | 'ultra_high'
  | 'custom';

export interface SenderConfig {
  chunkSize: number;
  fps: number;
  qrSize: number;
  errorCorrection: 'L' | 'M' | 'Q' | 'H';
  brightness: number;
  contrast: number;
  invertColor: boolean;
  metaFrequency: number;
  useCompression: boolean;
  encodingMode: 'base45' | 'base64';
  enablePreRendering: boolean;
  densityPreset?: QrDensityPreset;
  adaptiveRateControl: boolean;
  minFps: number;
  maxFps: number;
  minChunkSize: number;
  maxChunkSize: number;
}

export interface ReceiverConfig {
  facingMode: 'environment' | 'user';
  resolution: '480p' | '720p' | '1080p';
  scanIntervalMs: number;
  adaptiveThreshold: boolean;
  audioFeedback: boolean;
  autoDownload: boolean;
  roiCropEnabled: boolean;
  adaptiveFeedbackBeacon: boolean;
}

export interface LogEntry {
  id: string;
  timestamp: Date;
  level: 'info' | 'success' | 'warn' | 'error';
  message: string;
  details?: string;
}

export type LightingCondition = 'EXCELLENT' | 'GOOD' | 'LOW_LIGHT' | 'HIGH_GLARE' | 'BLUR';

export interface OpticalDiagnostics {
  luminance: number;
  contrast: number;
  lightingCondition: LightingCondition;
  failureRate: number;
  successRate: number;
  linkQualityScore: number;
  consecutiveFailures: number;
  suggestedFps: number;
  suggestedChunkSize: number;
  suggestedEcc: 'L' | 'M' | 'Q' | 'H';
  recommendedAction: string;
}

export interface ReceiverState {
  status: 'idle' | 'requesting_permission' | 'scanning' | 'receiving' | 'verifying' | 'completed' | 'error';
  transferId: string | null;
  meta: TransferMeta | null;
  receivedChunks: Map<number, Uint8Array>;
  totalChunks: number;
  receivedCount: number;
  startTime: number | null;
  lastChunkTime: number | null;
  bytesReceived: number;
  computedHash: string | null;
  isHashValid: boolean | null;
  reconstructedBlob: Blob | null;
  reconstructedUrl: string | null;
  errorMessage: string | null;
  recoveryHint: string | null;
  fpsDetected: number;
  scanLatencyMs: number;
  uniqueFrameRate: number;
  diagnostics: OpticalDiagnostics;
}

export interface BatteryStatus {
  supported: boolean;
  level: number;
  charging: boolean;
  chargingTime: number;
  dischargingTime: number;
  isLow: boolean;
  isCritical: boolean;
}

export interface SessionHistoryItem {
  id: string;
  transferId: string;
  fileName: string;
  fileSize: number;
  fileType: string;
  role: 'sent' | 'received';
  timestamp: number;
  hash: string;
  totalChunks: number;
  durationSeconds: number;
  averageSpeedKb: number;
  status: 'success' | 'failed';
  downloadUrl?: string | null;
}

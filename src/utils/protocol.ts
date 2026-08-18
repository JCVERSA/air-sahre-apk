import { Packet, TransferMeta, ChunkPayload, OpticalFeedbackPayload } from '../types/transfer';

const PROTOCOL_PREFIX_V1 = 'AIR1:';
const PROTOCOL_PREFIX_V2 = 'AIR2:';

export function encodeMetaPacket(meta: TransferMeta): string {
  const b64Name = btoa(encodeURIComponent(meta.name));
  const b64Type = btoa(encodeURIComponent(meta.type || 'application/octet-stream'));
  const compFlag = meta.compressed ? '1' : '0';
  const compSize = meta.compressedSize || meta.size;
  const encFlag = meta.encoding === 'base45' ? '45' : '64';

  return `${PROTOCOL_PREFIX_V2}M:${meta.id}:${meta.size}:${meta.totalChunks}:${meta.chunkSize}:${meta.hash}:${b64Name}:${b64Type}:${compFlag}:${compSize}:${encFlag}`;
}

export function encodeChunkPacket(chunk: ChunkPayload): string {
  const encFlag = chunk.encoding === 'base45' ? '45' : '64';
  return `${PROTOCOL_PREFIX_V2}C:${chunk.id}:${chunk.index}:${chunk.total}:${encFlag}:${chunk.data}`;
}

export function encodeFeedbackPacket(feedback: OpticalFeedbackPayload): string {
  const missingStr = feedback.missingChunks.slice(0, 50).join(',');
  return `${PROTOCOL_PREFIX_V2}FB:${feedback.transferId}:${feedback.qualityScore}:${feedback.failureRate}:${feedback.lighting}:${feedback.recommendedFps}:${feedback.recommendedChunkSize}:${feedback.recommendedEcc}:${missingStr}`;
}

export function parsePacket(rawText: string): Packet | null {
  if (!rawText) return null;
  const text = rawText.trim();

  if (text.startsWith(PROTOCOL_PREFIX_V2)) {
    const parts = text.substring(PROTOCOL_PREFIX_V2.length).split(':');
    const type = parts[0];

    if (type === 'FB' && parts.length >= 8) {
      try {
        const transferId = parts[1];
        const qualityScore = parseInt(parts[2], 10);
        const failureRate = parseInt(parts[3], 10);
        const lighting = parts[4] as OpticalFeedbackPayload['lighting'];
        const recommendedFps = parseInt(parts[5], 10);
        const recommendedChunkSize = parseInt(parts[6], 10);
        const recommendedEcc = parts[7] as OpticalFeedbackPayload['recommendedEcc'];
        const missingChunks = parts[8]
          ? parts[8].split(',').map((n) => parseInt(n, 10)).filter((n) => !isNaN(n))
          : [];

        const feedback: OpticalFeedbackPayload = {
          transferId,
          qualityScore: isNaN(qualityScore) ? 50 : qualityScore,
          failureRate: isNaN(failureRate) ? 0 : failureRate,
          lighting: lighting || 'GOOD',
          recommendedFps: isNaN(recommendedFps) ? 12 : recommendedFps,
          recommendedChunkSize: isNaN(recommendedChunkSize) ? 500 : recommendedChunkSize,
          recommendedEcc: recommendedEcc || 'M',
          missingChunks,
        };

        return {
          version: 2,
          type: 'FEEDBACK',
          feedback,
        };
      } catch {
        return null;
      }
    }

    if (type === 'M' && parts.length >= 8) {
      try {
        const id = parts[1];
        const size = parseInt(parts[2], 10);
        const totalChunks = parseInt(parts[3], 10);
        const chunkSize = parseInt(parts[4], 10);
        const hash = parts[5];
        const name = decodeURIComponent(atob(parts[6]));
        const mimeType = decodeURIComponent(atob(parts[7]));
        const compressed = parts[8] === '1';
        const compressedSize = parts[9] ? parseInt(parts[9], 10) : size;
        const encoding = parts[10] === '45' ? 'base45' : 'base64';

        if (isNaN(size) || isNaN(totalChunks) || isNaN(chunkSize) || !hash || !name) {
          return null;
        }

        const meta: TransferMeta = {
          id,
          name,
          size,
          type: mimeType,
          totalChunks,
          chunkSize,
          hash,
          compressed,
          compressedSize,
          encoding,
          timestamp: Date.now(),
        };

        return {
          version: 2,
          type: 'META',
          meta,
        };
      } catch {
        return null;
      }
    }

    if (type === 'C' && parts.length >= 5) {
      try {
        const id = parts[1];
        const index = parseInt(parts[2], 10);
        const total = parseInt(parts[3], 10);
        const encoding = parts[4] === '45' ? 'base45' : 'base64';
        const data = parts.slice(5).join(':');

        if (isNaN(index) || isNaN(total) || !data || !id) {
          return null;
        }

        return {
          version: 2,
          type: 'CHUNK',
          chunk: {
            id,
            index,
            total,
            encoding,
            data,
          },
        };
      } catch {
        return null;
      }
    }
  }

  if (text.startsWith(PROTOCOL_PREFIX_V1)) {
    const parts = text.substring(PROTOCOL_PREFIX_V1.length).split(':');
    const type = parts[0];

    if (type === 'M' && parts.length >= 8) {
      try {
        const id = parts[1];
        const size = parseInt(parts[2], 10);
        const totalChunks = parseInt(parts[3], 10);
        const chunkSize = parseInt(parts[4], 10);
        const hash = parts[5];
        const name = decodeURIComponent(atob(parts[6]));
        const mimeType = decodeURIComponent(atob(parts[7]));

        if (isNaN(size) || isNaN(totalChunks) || isNaN(chunkSize) || !hash || !name) {
          return null;
        }

        return {
          version: 1,
          type: 'META',
          meta: {
            id,
            name,
            size,
            type: mimeType,
            totalChunks,
            chunkSize,
            hash,
            encoding: 'base64',
            timestamp: Date.now(),
          },
        };
      } catch {
        return null;
      }
    }

    if (type === 'C' && parts.length >= 5) {
      try {
        const id = parts[1];
        const index = parseInt(parts[2], 10);
        const total = parseInt(parts[3], 10);
        const data = parts.slice(4).join(':');

        if (isNaN(index) || isNaN(total) || !data || !id) {
          return null;
        }

        return {
          version: 1,
          type: 'CHUNK',
          chunk: {
            id,
            index,
            total,
            encoding: 'base64',
            data,
          },
        };
      } catch {
        return null;
      }
    }
  }

  return null;
}

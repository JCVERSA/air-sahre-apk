import QRCode from 'qrcode';
import jsQR from 'jsqr';

export async function renderQRToCanvas(
  canvas: HTMLCanvasElement,
  text: string,
  options: {
    size?: number;
    errorCorrection?: 'L' | 'M' | 'Q' | 'H';
    invert?: boolean;
  } = {}
): Promise<void> {
  const { size = 420, errorCorrection = 'M', invert = false } = options;

  await QRCode.toCanvas(canvas, text, {
    width: size,
    margin: 1,
    errorCorrectionLevel: errorCorrection,
    color: {
      dark: invert ? '#ffffff' : '#000000',
      light: invert ? '#000000' : '#ffffff',
    },
  });
}

export async function preRenderQRCache(
  packets: string[],
  options: {
    size?: number;
    errorCorrection?: 'L' | 'M' | 'Q' | 'H';
    invert?: boolean;
  } = {},
  onProgress?: (index: number, total: number) => void
): Promise<HTMLCanvasElement[]> {
  const canvases: HTMLCanvasElement[] = [];

  for (let i = 0; i < packets.length; i++) {
    const c = document.createElement('canvas');
    c.width = options.size || 420;
    c.height = options.size || 420;
    await renderQRToCanvas(c, packets[i], options);
    canvases.push(c);
    onProgress?.(i + 1, packets.length);
  }

  return canvases;
}

export function scanQRFromCanvas(
  canvas: HTMLCanvasElement,
  cropRatio: number = 0.75
): string | null {
  const ctx = canvas.getContext('2d', { willReadFrequently: true });
  if (!ctx) return null;

  const w = canvas.width;
  const h = canvas.height;

  if (cropRatio < 1.0) {
    const cropW = Math.floor(w * cropRatio);
    const cropH = Math.floor(h * cropRatio);
    const cropX = Math.floor((w - cropW) / 2);
    const cropY = Math.floor((h - cropH) / 2);

    const imgData = ctx.getImageData(cropX, cropY, cropW, cropH);
    const code = jsQR(imgData.data, cropW, cropH, {
      inversionAttempts: 'dontInvert',
    });
    if (code) return code.data;
  }

  const imgData = ctx.getImageData(0, 0, w, h);
  const code = jsQR(imgData.data, w, h, {
    inversionAttempts: 'attemptBoth',
  });

  return code ? code.data : null;
}

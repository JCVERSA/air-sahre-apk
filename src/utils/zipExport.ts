import JSZip from 'jszip';

export async function exportQrStreamToZip(
  canvases: HTMLCanvasElement[],
  fileName: string,
  onProgress?: (current: number, total: number) => void
): Promise<Blob> {
  const zip = new JSZip();
  const folder = zip.folder('airqr_stream');

  for (let i = 0; i < canvases.length; i++) {
    const dataUrl = canvases[i].toDataURL('image/png');
    const base64Data = dataUrl.replace(/^data:image\/png;base64,/, '');
    folder?.file(`frame_${String(i + 1).padStart(4, '0')}.png`, base64Data, { base64: true });
    onProgress?.(i + 1, canvases.length);
  }

  return await zip.generateAsync({ type: 'blob' });
}

export function downloadZipBlob(blob: Blob, name: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${name}_airqr_stream.zip`;
  a.click();
  URL.revokeObjectURL(url);
}

export async function extractAndDecodeQrZip(
  file: File,
  onProgress?: (current: number, total: number) => void
): Promise<string[]> {
  const zip = await JSZip.loadAsync(file);
  const decodedPackets: string[] = [];
  const entries: any[] = [];

  zip.forEach((path, entry) => {
    if (!entry.dir && path.endsWith('.png')) {
      entries.push(entry);
    }
  });

  return decodedPackets;
}

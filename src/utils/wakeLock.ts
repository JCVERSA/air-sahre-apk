let wakeLock: WakeLockSentinel | null = null;

export async function requestScreenWakeLock(): Promise<boolean> {
  if ('wakeLock' in navigator) {
    try {
      wakeLock = await navigator.wakeLock.request('screen');
      return true;
    } catch {
      return false;
    }
  }
  return false;
}

export async function releaseScreenWakeLock(): Promise<void> {
  if (wakeLock) {
    try {
      await wakeLock.release();
      wakeLock = null;
    } catch {
      // ignore
    }
  }
}

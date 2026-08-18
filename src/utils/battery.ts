import { BatteryStatus } from '../types/transfer';

export async function getBatteryStatus(): Promise<BatteryStatus> {
  if ('getBattery' in navigator) {
    try {
      const b: any = await (navigator as any).getBattery();
      return {
        supported: true,
        level: b.level,
        charging: b.charging,
        chargingTime: b.chargingTime,
        dischargingTime: b.dischargingTime,
        isLow: b.level <= 0.2,
        isCritical: b.level <= 0.1,
      };
    } catch {
      // ignore
    }
  }
  return {
    supported: false,
    level: 1,
    charging: true,
    chargingTime: 0,
    dischargingTime: 0,
    isLow: false,
    isCritical: false,
  };
}

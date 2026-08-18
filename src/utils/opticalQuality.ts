import { OpticalDiagnostics, LightingCondition } from '../types/transfer';

export class OpticalQualityTracker {
  private frameTimes: number[] = [];
  private failures: number = 0;
  private successes: number = 0;

  recordSuccess(): void {
    this.successes++;
    this.frameTimes.push(Date.now());
    if (this.frameTimes.length > 30) this.frameTimes.shift();
  }

  recordFailure(): void {
    this.failures++;
  }

  getFps(): number {
    if (this.frameTimes.length < 2) return 0;
    const duration = (this.frameTimes[this.frameTimes.length - 1] - this.frameTimes[0]) / 1000;
    return duration > 0 ? Math.round(this.frameTimes.length / duration) : 0;
  }

  getDiagnostics(): OpticalDiagnostics {
    const total = this.successes + this.failures;
    const failureRate = total > 0 ? Math.round((this.failures / total) * 100) : 0;
    const score = Math.max(0, 100 - failureRate * 2);

    return {
      luminance: 128,
      contrast: 75,
      lightingCondition: 'GOOD' as LightingCondition,
      failureRate,
      successRate: 100 - failureRate,
      linkQualityScore: score,
      consecutiveFailures: this.failures,
      suggestedFps: this.getFps() || 14,
      suggestedChunkSize: 700,
      suggestedEcc: 'M',
      recommendedAction: score > 70 ? 'Optical link nominal' : 'Adjust angle or increase brightness',
    };
  }
}

export type AppTheme = 'cyber' | 'sunlight' | 'oled' | 'amber';

export interface ThemeConfig {
  id: AppTheme;
  name: string;
  desc: string;
  bgClass: string;
  cardClass: string;
  borderClass: string;
  accentText: string;
  accentBg: string;
  qrBg: string;
  qrFg: string;
}

export const THEMES: Record<AppTheme, ThemeConfig> = {
  cyber: {
    id: 'cyber',
    name: 'Cyber Slate',
    desc: 'Deep slate with neon cyan & emerald (Standard)',
    bgClass: 'bg-slate-950 text-slate-100',
    cardClass: 'bg-slate-900',
    borderClass: 'border-slate-800',
    accentText: 'text-cyan-400',
    accentBg: 'bg-cyan-500',
    qrBg: '#ffffff',
    qrFg: '#000000',
  },
  sunlight: {
    id: 'sunlight',
    name: 'Sunlight / Glare',
    desc: 'Ultra high-contrast for bright outdoor daylight',
    bgClass: 'bg-neutral-950 text-neutral-50',
    cardClass: 'bg-neutral-900',
    borderClass: 'border-amber-400/50',
    accentText: 'text-amber-400',
    accentBg: 'bg-amber-400',
    qrBg: '#ffffff',
    qrFg: '#000000',
  },
  oled: {
    id: 'oled',
    name: 'OLED Midnight',
    desc: 'Pitch-black #000000 for max contrast & battery savings',
    bgClass: 'bg-black text-white',
    cardClass: 'bg-zinc-950',
    borderClass: 'border-zinc-800',
    accentText: 'text-emerald-400',
    accentBg: 'bg-emerald-400',
    qrBg: '#ffffff',
    qrFg: '#000000',
  },
  amber: {
    id: 'amber',
    name: 'Tactical Amber',
    desc: 'Low-light / night vision eye-safe high-contrast monochrome',
    bgClass: 'bg-stone-950 text-amber-100',
    cardClass: 'bg-stone-900',
    borderClass: 'border-amber-600/60',
    accentText: 'text-amber-500',
    accentBg: 'bg-amber-500',
    qrBg: '#000000',
    qrFg: '#f59e0b',
  },
};

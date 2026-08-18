import { SessionHistoryItem } from '../types/transfer';

const HISTORY_STORAGE_KEY = 'airqr_transfer_history';

export function getSessionHistory(): SessionHistoryItem[] {
  try {
    const raw = localStorage.getItem(HISTORY_STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function addSessionHistoryItem(item: SessionHistoryItem): void {
  try {
    const list = getSessionHistory();
    list.unshift(item);
    if (list.length > 50) list.length = 50;
    localStorage.setItem(HISTORY_STORAGE_KEY, JSON.stringify(list));
  } catch {
    // ignore
  }
}

export function clearSessionHistory(): void {
  try {
    localStorage.removeItem(HISTORY_STORAGE_KEY);
  } catch {
    // ignore
  }
}

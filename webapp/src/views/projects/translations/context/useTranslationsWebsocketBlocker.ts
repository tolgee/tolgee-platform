import { useEffect } from 'react';
import { useTranslationsActions } from './TranslationsContext';

/**
 * Blocks incloming websocket events, while some operation is performed
 * events are applied after the block is lifted
 */
export function useTranslationsWebsocketBlocker(blocking: boolean) {
  const { setEventBlockers } = useTranslationsActions();
  useEffect(() => {
    if (blocking) {
      setEventBlockers((num) => num + 1);
      return () => setEventBlockers((num) => num - 1);
    }
  }, [blocking]);
}

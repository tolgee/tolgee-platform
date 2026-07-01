import { useEffect } from 'react';

import { useAppTriggerDispatch, useAppTriggers } from './useAppTriggers';

/**
 * Listens for keydown events anywhere in the project view and dispatches
 * any registered plugin shortcut whose combination matches. Mounted once
 * at the translations view level.
 */
export function useAppShortcuts(projectId: number): void {
  const shortcuts = useAppTriggers(projectId, 'shortcut');
  const dispatch = useAppTriggerDispatch();

  useEffect(() => {
    if (shortcuts.length === 0) return;
    const handler = (event: KeyboardEvent) => {
      for (const trigger of shortcuts) {
        const combination = trigger.item.combination;
        if (!combination) continue;
        if (eventMatches(event, combination)) {
          event.preventDefault();
          dispatch(trigger, { templateVars: { projectId } });
          return;
        }
      }
    };
    window.addEventListener('keydown', handler);
    return () => window.removeEventListener('keydown', handler);
  }, [shortcuts, dispatch, projectId]);
}

const isMac = (): boolean =>
  typeof navigator !== 'undefined' &&
  /Mac|iPhone|iPod|iPad/i.test(navigator.platform);

/**
 * Matches a mousetrap-style combination string (e.g. `Mod+Shift+E`)
 * against a keyboard event. `Mod` resolves to Meta on macOS, Ctrl
 * everywhere else.
 */
const eventMatches = (event: KeyboardEvent, combination: string): boolean => {
  const parts = combination.split('+').map((p) => p.trim().toLowerCase());
  const wantedKey = parts[parts.length - 1];
  const modifiers = parts.slice(0, -1);
  if (event.key.toLowerCase() !== wantedKey) return false;
  const wantMeta =
    modifiers.includes('meta') || (modifiers.includes('mod') && isMac());
  const wantCtrl =
    modifiers.includes('ctrl') || (modifiers.includes('mod') && !isMac());
  const wantShift = modifiers.includes('shift');
  const wantAlt = modifiers.includes('alt');
  return (
    event.metaKey === wantMeta &&
    event.ctrlKey === wantCtrl &&
    event.shiftKey === wantShift &&
    event.altKey === wantAlt
  );
};

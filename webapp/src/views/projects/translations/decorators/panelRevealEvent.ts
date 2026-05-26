export const PANEL_REVEAL_EVENT = 'tolgee-app:reveal-panel';

export type PanelRevealDetail = {
  id: string;
};

// Held when a decorator requests a reveal but the ToolsPanel listener
// isn't mounted yet (the side panel was closed at click time). The next
// ToolsPanel mount consumes and clears this so the reveal still happens.
let pendingReveal: string | null = null;

export function requestPanelReveal(id: string): void {
  pendingReveal = id;
  window.dispatchEvent(
    new CustomEvent<PanelRevealDetail>(PANEL_REVEAL_EVENT, { detail: { id } })
  );
}

export function consumePendingPanelReveal(): string | null {
  const id = pendingReveal;
  pendingReveal = null;
  return id;
}

export function onPanelRevealRequest(
  handler: (id: string) => void
): () => void {
  const listener = (event: Event) => {
    const detail = (event as CustomEvent<PanelRevealDetail>).detail;
    if (detail?.id) {
      pendingReveal = null;
      handler(detail.id);
    }
  };
  window.addEventListener(PANEL_REVEAL_EVENT, listener);
  return () => window.removeEventListener(PANEL_REVEAL_EVENT, listener);
}

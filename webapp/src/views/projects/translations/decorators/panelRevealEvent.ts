export const PANEL_REVEAL_EVENT = 'tolgee-app:reveal-panel';

export type PanelRevealDetail = {
  id: string;
};

export function requestPanelReveal(id: string): void {
  window.dispatchEvent(
    new CustomEvent<PanelRevealDetail>(PANEL_REVEAL_EVENT, { detail: { id } })
  );
}

export function onPanelRevealRequest(
  handler: (id: string) => void
): () => void {
  const listener = (event: Event) => {
    const detail = (event as CustomEvent<PanelRevealDetail>).detail;
    if (detail?.id) handler(detail.id);
  };
  window.addEventListener(PANEL_REVEAL_EVENT, listener);
  return () => window.removeEventListener(PANEL_REVEAL_EVENT, listener);
}

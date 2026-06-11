export const KEY_DIALOG_OPEN_EVENT = 'tolgee-app:open-key-dialog';

export type KeyDialogOpenDetail = {
  keyId: number;
  /**
   * Tab id to focus when the dialog opens. Plugin tabs use the form
   * `app:<installId>:<tabKey>`.
   */
  initialTab: string;
};

export function requestKeyDialogOpen(detail: KeyDialogOpenDetail): void {
  window.dispatchEvent(
    new CustomEvent<KeyDialogOpenDetail>(KEY_DIALOG_OPEN_EVENT, { detail })
  );
}

export function onKeyDialogOpenRequest(
  handler: (detail: KeyDialogOpenDetail) => void
): () => void {
  const listener = (event: Event) => {
    const detail = (event as CustomEvent<KeyDialogOpenDetail>).detail;
    if (detail?.initialTab && typeof detail.keyId === 'number') handler(detail);
  };
  window.addEventListener(KEY_DIALOG_OPEN_EVENT, listener);
  return () => window.removeEventListener(KEY_DIALOG_OPEN_EVENT, listener);
}

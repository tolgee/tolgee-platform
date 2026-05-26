import { useEffect, useMemo, useRef } from 'react';

import { useAppToken } from './useAppToken';

export type IframeSelection = {
  keyId: number | null;
  languageId: number | null;
  languageTag: string | null;
  translationId: number | null;
};

export type UseAppIframeMessagingOptions = {
  installId: number;
  projectId: number;
  organizationId: number | null;
  baseUrl: string;
  entry: string;
  selection: IframeSelection;
  onResize?: (height: number) => void;
  /** Invoked when the iframe posts `{ type: 'tolgee-app:close' }`. */
  onClose?: () => void;
  /** Anchor-specific fields merged into the tolgee-app:init payload. */
  extraInitPayload?: Record<string, unknown>;
};

export type UseAppIframeMessagingResult = {
  iframeRef: React.RefObject<HTMLIFrameElement>;
  iframeSrc: string | null;
  appOrigin: string | null;
  token: string | null;
};

const safeOrigin = (url: string): string | null => {
  try {
    return new URL(url).origin;
  } catch {
    return null;
  }
};

export function useAppIframeMessaging(
  options: UseAppIframeMessagingOptions
): UseAppIframeMessagingResult {
  const {
    installId,
    projectId,
    organizationId,
    baseUrl,
    entry,
    selection,
    onResize,
    onClose,
    extraInitPayload,
  } = options;

  const token = useAppToken(projectId, installId);
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const initSentRef = useRef(false);

  const appOrigin = useMemo(() => safeOrigin(baseUrl), [baseUrl]);
  const iframeSrc = useMemo(
    () => (appOrigin ? `${baseUrl}${entry}` : null),
    [appOrigin, baseUrl, entry]
  );

  const apiUrl = import.meta.env.VITE_APP_API_URL ?? window.location.origin;

  const selectionKey = `${selection.keyId}|${selection.languageId}|${selection.translationId}`;

  const sendInit = () => {
    if (!appOrigin || !token || !iframeRef.current?.contentWindow) return;
    iframeRef.current.contentWindow.postMessage(
      {
        type: 'tolgee-app:init',
        token,
        apiUrl,
        organizationId,
        projectId,
        ...selection,
        ...extraInitPayload,
      },
      appOrigin
    );
    initSentRef.current = true;
  };

  useEffect(() => {
    if (!appOrigin) return;

    const handler = (event: MessageEvent) => {
      if (event.origin !== appOrigin) return;
      if (event.source !== iframeRef.current?.contentWindow) return;
      const data = event.data;
      if (!data || typeof data !== 'object') return;

      if (data.type === 'tolgee-app:ready') {
        sendInit();
      } else if (data.type === 'tolgee-app:resize') {
        const next = Number(data.height);
        if (Number.isFinite(next) && next > 0 && onResize) {
          onResize(next);
        }
      } else if (data.type === 'tolgee-app:close' && onClose) {
        onClose();
      }
    };
    window.addEventListener('message', handler);
    return () => window.removeEventListener('message', handler);
  }, [
    appOrigin,
    token,
    apiUrl,
    organizationId,
    projectId,
    selectionKey,
    onResize,
    onClose,
  ]);

  useEffect(() => {
    if (initSentRef.current) return;
    sendInit();
  }, [token, appOrigin]);

  useEffect(() => {
    if (!initSentRef.current || !appOrigin) return;
    iframeRef.current?.contentWindow?.postMessage(
      { type: 'tolgee-app:selection-changed', ...selection },
      appOrigin
    );
  }, [selectionKey, appOrigin]);

  return { iframeRef, iframeSrc, appOrigin, token };
}

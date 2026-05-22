import { useEffect, useMemo, useRef, useState } from 'react';
import { Box, CircularProgress, styled } from '@mui/material';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { PanelContentProps } from '../../common/types';

type AppPanelBinding = {
  installId: number;
  baseUrl: string;
  entry: string;
};

const StyledIframe = styled('iframe')`
  width: 100%;
  border: none;
  background: transparent;
  display: block;
`;

const DEFAULT_HEIGHT = 80;

const buildIframeSrc = (binding: AppPanelBinding): string =>
  `${binding.baseUrl}${binding.entry}`;

const safeOrigin = (url: string): string | null => {
  try {
    return new URL(url).origin;
  } catch {
    return null;
  }
};

const buildSelectionPayload = ({
  keyData,
  language,
}: PanelContentProps): {
  keyId: number | null;
  languageId: number | null;
  languageTag: string | null;
  translationId: number | null;
} => {
  const keyId = keyData?.keyId ?? null;
  const languageId = language?.id ?? null;
  const languageTag = language?.tag ?? null;
  const translationId =
    keyData && language
      ? keyData.translations?.[language.tag]?.id ?? null
      : null;
  return { keyId, languageId, languageTag, translationId };
};

export function createAppPanelComponent(binding: AppPanelBinding) {
  return function AppPanelContent(props: PanelContentProps) {
    const { project } = props;
    const organizationId = project.organizationOwner?.id ?? null;

    const tokenMutation = useApiMutation({
      url: '/v2/projects/{projectId}/apps/{installId}/token',
      method: 'post',
    });

    const [token, setToken] = useState<string | null>(null);
    const [height, setHeight] = useState<number>(DEFAULT_HEIGHT);
    const iframeRef = useRef<HTMLIFrameElement | null>(null);
    const initSentRef = useRef(false);

    const appOrigin = useMemo(() => safeOrigin(binding.baseUrl), []);
    const iframeSrc = useMemo(() => buildIframeSrc(binding), []);
    const apiUrl = import.meta.env.VITE_APP_API_URL ?? window.location.origin;

    const selection = buildSelectionPayload(props);
    const selectionKey = `${selection.keyId}|${selection.languageId}|${selection.translationId}`;

    useEffect(() => {
      let cancelled = false;
      tokenMutation.mutate(
        { path: { projectId: project.id, installId: binding.installId } },
        {
          onSuccess: (data) => {
            if (!cancelled) setToken(data.token);
          },
        }
      );
      return () => {
        cancelled = true;
      };
    }, [project.id]);

    useEffect(() => {
      if (!appOrigin) return;

      const handler = (event: MessageEvent) => {
        if (event.origin !== appOrigin) return;
        if (event.source !== iframeRef.current?.contentWindow) return;
        const data = event.data;
        if (!data || typeof data !== 'object') return;

        if (data.type === 'tolgee-app:ready') {
          if (!token) return;
          iframeRef.current?.contentWindow?.postMessage(
            {
              type: 'tolgee-app:init',
              token,
              apiUrl,
              organizationId,
              projectId: project.id,
              ...selection,
            },
            appOrigin
          );
          initSentRef.current = true;
        } else if (data.type === 'tolgee-app:resize') {
          const next = Number(data.height);
          if (Number.isFinite(next) && next > 0) {
            setHeight(Math.max(DEFAULT_HEIGHT, Math.min(next, 800)));
          }
        }
      };
      window.addEventListener('message', handler);
      return () => window.removeEventListener('message', handler);
    }, [
      appOrigin,
      token,
      apiUrl,
      organizationId,
      project.id,
      selection.keyId,
      selection.languageId,
      selection.languageTag,
      selection.translationId,
    ]);

    useEffect(() => {
      if (!appOrigin) return;
      if (!initSentRef.current) return;
      iframeRef.current?.contentWindow?.postMessage(
        { type: 'tolgee-app:selection-changed', ...selection },
        appOrigin
      );
    }, [selectionKey, appOrigin]);

    if (!appOrigin) {
      return (
        <Box p={2} fontSize="0.85rem" color="text.secondary">
          App misconfigured: invalid baseUrl.
        </Box>
      );
    }

    if (!token) {
      return (
        <Box
          display="flex"
          justifyContent="center"
          alignItems="center"
          minHeight={DEFAULT_HEIGHT}
        >
          <CircularProgress size={20} />
        </Box>
      );
    }

    return (
      <StyledIframe
        ref={iframeRef}
        data-cy="translation-tools-app-iframe"
        data-cy-install-id={binding.installId}
        src={iframeSrc}
        sandbox="allow-scripts allow-forms allow-same-origin"
        style={{ height }}
      />
    );
  };
}

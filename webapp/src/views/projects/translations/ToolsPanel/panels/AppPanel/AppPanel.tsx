import { useCallback, useState } from 'react';
import { Box, CircularProgress, styled } from '@mui/material';

import { useAppIframeMessaging } from '../../../decorators/useAppIframeMessaging';
import { PanelContentProps } from '../../common/types';
import { useTranslationsSelector } from '../../../context/TranslationsContext';

type AppPanelBinding = {
  installId: number;
  baseUrl: string;
  entry: string;
  /**
   * When true the panel renders in the no-cell-selected state and is handed the
   * view's selected language tags instead of a key/language selection.
   */
  empty?: boolean;
};

const StyledIframe = styled('iframe')`
  width: 100%;
  border: none;
  background: transparent;
  display: block;
`;

const DEFAULT_HEIGHT = 80;
const MAX_HEIGHT = 800;

const buildSelectionPayload = ({ keyData, language }: PanelContentProps) => {
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
    const selectedLanguages = useTranslationsSelector(
      (c) => c.selectedLanguages
    );
    const [height, setHeight] = useState<number>(DEFAULT_HEIGHT);

    const onResize = useCallback((next: number) => {
      setHeight(Math.max(DEFAULT_HEIGHT, Math.min(next, MAX_HEIGHT)));
    }, []);

    const { iframeRef, iframeSrc, appOrigin, token } = useAppIframeMessaging({
      installId: binding.installId,
      projectId: project.id,
      organizationId,
      baseUrl: binding.baseUrl,
      entry: binding.entry,
      selection: binding.empty
        ? {
            keyId: null,
            languageId: null,
            languageTag: null,
            translationId: null,
            selectedLanguages: selectedLanguages ?? [],
          }
        : buildSelectionPayload(props),
      onResize,
    });

    if (!appOrigin || !iframeSrc) {
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
        sandbox="allow-scripts allow-forms allow-same-origin allow-popups allow-popups-to-escape-sandbox allow-top-navigation-by-user-activation"
        style={{ height }}
      />
    );
  };
}

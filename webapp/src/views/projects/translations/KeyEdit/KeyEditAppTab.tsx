import { Box, CircularProgress, styled } from '@mui/material';

import { useProject } from 'tg.hooks/useProject';
import { DeletableKeyWithTranslationsModelType } from '../context/types';
import { useAppIframeMessaging } from '../decorators/useAppIframeMessaging';

const StyledIframe = styled('iframe')`
  width: 100%;
  min-height: 360px;
  border: none;
  background: transparent;
  display: block;
`;

type Props = {
  installId: number;
  baseUrl: string;
  entry: string;
  data: DeletableKeyWithTranslationsModelType;
};

export const KeyEditAppTab = ({ installId, baseUrl, entry, data }: Props) => {
  const project = useProject();
  const organizationId = project.organizationOwner?.id ?? null;

  const { iframeRef, iframeSrc, appOrigin, token } = useAppIframeMessaging({
    installId,
    projectId: project.id,
    organizationId,
    baseUrl,
    entry,
    selection: {
      keyId: data.keyId,
      languageId: null,
      languageTag: null,
      translationId: null,
    },
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
        minHeight={360}
      >
        <CircularProgress size={20} />
      </Box>
    );
  }

  return (
    <StyledIframe
      ref={iframeRef}
      data-cy="key-edit-app-tab-iframe"
      data-cy-install-id={installId}
      src={iframeSrc}
      sandbox="allow-scripts allow-forms allow-same-origin allow-popups allow-popups-to-escape-sandbox allow-top-navigation-by-user-activation"
    />
  );
};

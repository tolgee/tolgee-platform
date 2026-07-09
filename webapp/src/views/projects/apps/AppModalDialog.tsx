import {
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  styled,
} from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';

import { useProject } from 'tg.hooks/useProject';
import { useAppIframeMessaging } from '../translations/decorators/useAppIframeMessaging';
import { AppIcon } from './AppIcon';

export type AppModalRequest = {
  installId: number;
  baseUrl: string;
  entry: string;
  title: string;
  icon?: string | null;
  width?: number;
  height?: number;
  extraInitPayload?: Record<string, unknown>;
  /** Selection context for the iframe init payload. */
  keyId?: number | null;
  languageId?: number | null;
  languageTag?: string | null;
  translationId?: number | null;
};

const DEFAULT_WIDTH = 640;
const DEFAULT_HEIGHT = 400;

const StyledDialogContent = styled(DialogContent)`
  padding: 0;
  display: flex;
  flex-direction: column;
`;

const StyledIframe = styled('iframe')`
  flex: 1;
  width: 100%;
  border: none;
  background: transparent;
`;

const StyledTitle = styled(DialogTitle)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 16px;
`;

type Props = {
  request: AppModalRequest;
  onClose: () => void;
};

export const AppModalDialog = ({ request, onClose }: Props) => {
  const project = useProject();
  const organizationId = project.organizationOwner?.id ?? null;
  const width = request.width ?? DEFAULT_WIDTH;
  const height = request.height ?? DEFAULT_HEIGHT;

  const { iframeRef, iframeSrc, appOrigin, token } = useAppIframeMessaging({
    installId: request.installId,
    projectId: project.id,
    organizationId,
    baseUrl: request.baseUrl,
    entry: request.entry,
    selection: {
      keyId: request.keyId ?? null,
      languageId: request.languageId ?? null,
      languageTag: request.languageTag ?? null,
      translationId: request.translationId ?? null,
    },
    extraInitPayload: request.extraInitPayload,
    onClose,
  });

  return (
    <Dialog
      open
      onClose={onClose}
      PaperProps={{
        sx: {
          width,
          height,
          maxWidth: '90vw',
          maxHeight: '90vh',
        },
      }}
    >
      <StyledTitle>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 8 }}>
          <AppIcon icon={request.icon} size={20} />
          {request.title}
        </span>
        <IconButton size="small" onClick={onClose} aria-label="close">
          <XClose />
        </IconButton>
      </StyledTitle>
      <StyledDialogContent dividers>
        {appOrigin && iframeSrc && token ? (
          <StyledIframe
            ref={iframeRef}
            data-cy="app-modal-iframe"
            data-cy-install-id={request.installId}
            src={iframeSrc}
            sandbox="allow-scripts allow-forms allow-same-origin allow-popups allow-popups-to-escape-sandbox allow-top-navigation-by-user-activation"
          />
        ) : null}
      </StyledDialogContent>
    </Dialog>
  );
};

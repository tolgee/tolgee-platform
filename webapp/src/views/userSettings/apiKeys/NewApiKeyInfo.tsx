import { Alert, Box } from '@mui/material';
import { T } from '@tolgee/react';
import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';
import { NewApiKeyType } from './ApiKeyListItem';

export const NewApiKeyInfo = (props: {
  newTokenType: NewApiKeyType | undefined;
  newTokenValue: string;
}) => (
  <Box sx={{ gridColumn: '1/5' }}>
    <Alert color="success" sx={{ mb: 1 }} data-cy="pat-list-item-alert">
      {props.newTokenType == 'created' && (
        <T keyName="api-key-new-token-message" />
      )}
      {props.newTokenType == 'regenerated' && (
        <T keyName="api-key-regenerated-token-message" />
      )}
    </Alert>
    <Box data-cy="api-key-list-item-new-token-input" data-sentry-mask="">
      <ClipboardCopyInput value={props.newTokenValue!} />
    </Box>
  </Box>
);

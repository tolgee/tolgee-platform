import { Alert, Box } from '@mui/material';
import { T } from '@tolgee/react';
import { ClipboardCopyInput } from 'tg.component/common/ClipboardCopyInput';
import { NewTokenType } from './PatListItem';

export const NewTokenInfo = (props: {
  newTokenType: NewTokenType | undefined;
  newTokenValue: string;
}) => (
  <Box sx={{ gridColumn: '1/5' }}>
    <Alert color="success" sx={{ mb: 1 }} data-cy="pat-list-item-alert">
      {props.newTokenType == 'created' && <T keyName="pat-new-token-message" />}
      {props.newTokenType == 'regenerated' && (
        <T keyName="pat-regenerated-token-message" />
      )}
    </Alert>
    <Box data-cy="pat-list-item-new-token-input" data-sentry-mask="">
      <ClipboardCopyInput value={props.newTokenValue!} />
    </Box>
  </Box>
);

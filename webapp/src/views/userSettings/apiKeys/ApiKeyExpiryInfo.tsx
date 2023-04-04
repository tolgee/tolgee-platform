import { components } from 'tg.service/apiSchema.generated';
import { Box } from '@mui/material';
import { T } from '@tolgee/react';

export const ApiKeyExpiryInfo = (props: {
  apiKey: components['schemas']['ApiKeyModel'];
}) => (
  <Box sx={{ gridColumn: '1/2' }} data-cy="api-key-expiry-info">
    {props.apiKey.expiresAt && props.apiKey.expiresAt < new Date().getTime() ? (
      <Box sx={(theme) => ({ color: theme.palette.warning.main })}>
        <T
          params={{ date: props.apiKey.expiresAt }}
          keyName="api-key-list-item-expired-on"
        />
      </Box>
    ) : (
      <Box sx={(theme) => ({ color: theme.palette.success.main })}>
        {!props.apiKey.expiresAt ? (
          <T keyName="api-key_never_expires" />
        ) : (
          <T
            params={{ date: props.apiKey.expiresAt }}
            keyName="api-key-list-item-expires-at"
          />
        )}
      </Box>
    )}
  </Box>
);

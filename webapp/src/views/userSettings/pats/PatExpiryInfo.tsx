import { components } from 'tg.service/apiSchema.generated';
import { Box } from '@mui/material';
import { T } from '@tolgee/react';

export const PatExpiryInfo = (props: {
  pat: components['schemas']['PatModel'];
}) => (
  <Box sx={{ gridColumn: '1/3' }} data-cy="pat-expiry-info">
    {props.pat.expiresAt && props.pat.expiresAt < new Date().getTime() ? (
      <Box sx={(theme) => ({ color: theme.palette.warning.main })}>
        <T
          params={{ date: props.pat.expiresAt }}
          keyName="pat-list-item-expired-on"
        />
      </Box>
    ) : (
      <Box sx={(theme) => ({ color: theme.palette.success.main })}>
        {!props.pat.expiresAt ? (
          <T keyName="pat_never_expires" />
        ) : (
          <T
            params={{ date: props.pat.expiresAt }}
            keyName="pat-list-item-expires-at"
          />
        )}
      </Box>
    )}
  </Box>
);

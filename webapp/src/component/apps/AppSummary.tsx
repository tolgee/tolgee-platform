import { Box, Typography } from '@mui/material';

import { AppAvatar } from 'tg.component/apps/AppAvatar';

type AppSummaryProps = {
  name: string;
  version: string;
  url: string;
  /** Manifest icon; rendered as the app's logo tile when `appId` is also given. */
  icon?: string | null;
  /** Enables the logo tile (also seeds its generated fallback). */
  appId?: string;
};

export const AppSummary = ({
  name,
  version,
  url,
  icon,
  appId,
}: AppSummaryProps) => (
  <Box display="flex" alignItems="center" gap={1.5} minWidth={0}>
    {appId !== undefined && (
      <AppAvatar icon={icon} name={name} appId={appId} size={40} />
    )}
    <Box minWidth={0}>
      <Typography variant="subtitle1">
        {name}{' '}
        <Typography component="span" variant="body2" color="text.secondary">
          v{version}
        </Typography>
      </Typography>
      <Typography variant="body2" color="text.secondary" noWrap>
        {url}
      </Typography>
    </Box>
  </Box>
);

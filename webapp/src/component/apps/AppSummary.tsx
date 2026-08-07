import { Box, Typography } from '@mui/material';

type AppSummaryProps = {
  name: string;
  version: string;
  url: string;
};

export const AppSummary = ({ name, version, url }: AppSummaryProps) => (
  <Box>
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
);

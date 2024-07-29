import { Box, Typography, Link } from '@mui/material';
import { useTranslate, T } from '@tolgee/react';

export function TolgeeMore() {
  const { t } = useTranslate();
  return (
    <Box>
      <Typography variant="h5">{t('login_more_title')}</Typography>
      <Box mb={3} />
      <Typography variant="body2" fontSize={14}>
        <T
          keyName="login_tolgee_website_link"
          params={{
            link: <Link href="https://tolgee.io" target="_blank" />,
          }}
        />
      </Typography>

      <Typography variant="body2" fontSize={14}>
        <T
          keyName="login_tolgee_documentation_link"
          params={{
            link: <Link href="https://tolgee.io/platform" target="_blank" />,
          }}
        />
      </Typography>
    </Box>
  );
}

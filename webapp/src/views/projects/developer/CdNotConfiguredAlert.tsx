import { Alert, AlertTitle, Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';

export const CdNotConfiguredAlert = () => {
  const { t } = useTranslate();
  return (
    <Box mt={6}>
      <Alert severity="info">
        <AlertTitle>{t('content_delivery_not_configured_title')}</AlertTitle>
        {t('content_delivery_not_configured_message')}
      </Alert>
    </Box>
  );
};

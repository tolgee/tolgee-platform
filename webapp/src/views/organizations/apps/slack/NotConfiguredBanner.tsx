import { Alert } from '@mui/material';
import { useTranslate } from '@tolgee/react';

export const NotConfiguredBanner = () => {
  const { t } = useTranslate();
  return <Alert severity="warning">{t('slack_app_not_configured')}</Alert>;
};

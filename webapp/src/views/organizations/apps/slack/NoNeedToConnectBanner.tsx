import { Alert } from '@mui/material';
import { useTranslate } from '@tolgee/react';

export const NoNeedToConnectBanner = () => {
  const { t } = useTranslate();
  return <Alert severity="info">{t('slack_app_no_need_to_connect')}</Alert>;
};

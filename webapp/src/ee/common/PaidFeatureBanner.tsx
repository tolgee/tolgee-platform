import { Alert, AlertTitle, Box, Button } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFeatureMissingExplanation } from './useFeatureMissingExplanation';
import { Link } from 'react-router-dom';

export const PaidFeatureBanner = () => {
  const { t } = useTranslate();

  const { message, actionTitle, link } = useFeatureMissingExplanation();

  return (
    <Alert
      severity="warning"
      action={
        actionTitle && link ? (
          <Button
            component={Link}
            color="inherit"
            size="small"
            variant="outlined"
            to={link}
          >
            {actionTitle}
          </Button>
        ) : undefined
      }
    >
      <AlertTitle>{t('paid-feature-banner-title')}</AlertTitle>
      {message && <Box>{message}</Box>}
    </Alert>
  );
};

import { Alert, AlertTitle, Box, Button } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFeatureMissingExplanation } from './useFeatureMissingExplanation';
import { Link } from 'react-router-dom';

type Props = {
  customTitle?: string;
  customMessage?: string;
};

export const PaidFeatureBanner = ({ customTitle, customMessage }: Props) => {
  const { t } = useTranslate();

  const { message, actionTitle, link } = useFeatureMissingExplanation();

  const combinedMessage = customMessage ?? message;

  return (
    <Alert
      severity="info"
      action={
        actionTitle && link ? (
          <Button
            component={Link}
            color="info"
            size="small"
            variant="contained"
            disableElevation
            to={link}
            sx={{ whiteSpace: 'nowrap' }}
          >
            {actionTitle}
          </Button>
        ) : undefined
      }
    >
      <AlertTitle>{customTitle ?? t('paid-feature-banner-title')}</AlertTitle>
      {combinedMessage && <Box>{combinedMessage}</Box>}
    </Alert>
  );
};

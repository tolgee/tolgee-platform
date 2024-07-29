import { Alert, AlertTitle, Box, Button } from '@mui/material';
import { useFeatureMissingExplanation } from './useFeatureMissingExplanation';
import { Link } from 'react-router-dom';

type Props = {
  customTitle?: string;
  customMessage?: string;
};

export const PaidFeatureBanner = ({ customMessage, customTitle }: Props) => {
  const { message, actionTitle, link } = useFeatureMissingExplanation();

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
      <AlertTitle sx={{ mb: customMessage ? undefined : 0, pb: 0 }}>
        {customTitle ?? message}
      </AlertTitle>
      {customMessage && <Box>{customMessage}</Box>}
    </Alert>
  );
};

import { Alert, AlertTitle, Box, Button } from '@mui/material';
import { Link } from 'react-router-dom';
import { useFeatureMissingExplanation } from './useFeatureMissingExplanation';

type Props = {
  customTitle?: string;
  customMessage?: string;
};

export const DisabledFeatureBanner = ({
  customMessage,
  customTitle,
}: Props) => {
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

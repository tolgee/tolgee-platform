import { Alert, AlertTitle, Box, Button } from '@mui/material';
import { Link } from 'react-router-dom';
import { useFeatureMissingExplanation } from './useFeatureMissingExplanation';

type Props = {
  customTitle?: string;
  customMessage?: string;
  actionOnBottom?: boolean;
};

export const DisabledFeatureBanner = ({
  customMessage,
  customTitle,
  actionOnBottom,
}: Props) => {
  const { message, actionTitle, link } = useFeatureMissingExplanation();
  const action =
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
    ) : undefined;

  return (
    <Alert severity="info" action={!actionOnBottom ? action : undefined}>
      <AlertTitle sx={{ mb: customMessage ? undefined : 0, pb: 0 }}>
        {customTitle ?? message}
      </AlertTitle>
      {customMessage && <Box>{customMessage}</Box>}
      {actionOnBottom && action && (
        <Box display="flex" mt={1} justifyContent="end">
          {action}
        </Box>
      )}
    </Alert>
  );
};

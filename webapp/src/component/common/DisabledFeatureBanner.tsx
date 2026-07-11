import {
  Alert,
  alertClasses,
  AlertTitle,
  Box,
  Button,
  styled,
} from '@mui/material';
import { Link } from 'react-router-dom';
import { useFeatureMissingExplanation } from './useFeatureMissingExplanation';

const StyledContainer = styled(Box)`
  display: grid;
  container-type: inline-size;
`;

const StyledAlert = styled(Alert)`
  display: grid;
  grid-template-columns: auto 1fr auto;

  @container (max-width: 500px) {
    grid-template-columns: auto 1fr;
    & .${alertClasses.action} {
      grid-column: 1 / -1;
      justify-self: end;
    }
  }
`;

type Props = {
  customTitle?: string;
  customMessage?: string;
};

export const DisabledFeatureBanner = ({
  customMessage,
  customTitle,
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
    <StyledContainer data-cy="disabled-feature-banner">
      <StyledAlert severity="info" action={action}>
        <AlertTitle sx={{ mb: customMessage ? undefined : 0, pb: 0 }}>
          {customTitle ?? message}
        </AlertTitle>
        {customMessage && <Box>{customMessage}</Box>}
      </StyledAlert>
    </StyledContainer>
  );
};

import { styled, Typography } from '@mui/material';
import Box from '@mui/material/Box';

const StyledImage = styled('img')`
  filter: grayscale(50%);
  opacity: 0.3;
  max-width: 100%;
`;

const StyledText = styled('div')`
  opacity: 0.3;
  padding-top: ${({ theme }) => theme.spacing(4)};
`;

type Props = {
  hint?: React.ReactNode;
};

export const SadEmotionMessage: React.FC<Props> = (props) => {
  return (
    <Box
      display="flex"
      justifyContent="center"
      flexDirection="column"
      alignItems="center"
    >
      {props.children && (
        <StyledText>
          <Typography>{props.children}</Typography>
        </StyledText>
      )}
      <StyledImage
        src="/images/sleepingMouse.svg"
        draggable="false"
        width="397px"
        height="300px"
      />
      {props.hint && <Box py={4}>{props.hint}</Box>}
    </Box>
  );
};

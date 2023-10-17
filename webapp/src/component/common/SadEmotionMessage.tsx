import { styled, Typography } from '@mui/material';
import Box from '@mui/material/Box';

const StyledImage = styled('img')`
  filter: grayscale(50%);
  opacity: 0.3;
  max-width: 100%;
`;

const StyledText = styled('div')`
  opacity: 0.8;
  padding-top: ${({ theme }) => theme.spacing(4)};
`;

export type SadEmotionMessageProps = {
  hint?: React.ReactNode;
  height?: string;
};

export const SadEmotionMessage: React.FC<SadEmotionMessageProps> = (props) => {
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
        height={props.height || '300px'}
      />
      {props.hint && <Box py={4}>{props.hint}</Box>}
    </Box>
  );
};

import {
  useMediaQuery,
  useTheme,
  Box,
  Typography,
  styled,
} from '@mui/material';
import { ReactNode } from 'react';

const StyledDangerZone = styled(Box)`
  display: grid;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.error.dark};
  gap: 16px;
`;

const StyledDangerZonePart = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
`;

type DangerAction = {
  description: ReactNode;
  button: ReactNode;
};

type Props = {
  actions: DangerAction[];
};

export const DangerZone: React.FC<Props> = ({ actions }) => {
  const theme = useTheme();
  const isSmOrLower = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <StyledDangerZone p={2}>
      {actions.map((action, i) => (
        <StyledDangerZonePart
          key={i}
          alignItems={isSmOrLower ? 'start' : 'center'}
          flexDirection={isSmOrLower ? 'column' : 'row'}
        >
          <Box flexGrow={1} mr={1}>
            <Typography variant="body1">{action.description}</Typography>
          </Box>
          {action.button}
        </StyledDangerZonePart>
      ))}
    </StyledDangerZone>
  );
};

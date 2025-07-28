import { useMediaQuery, Box, Typography, styled } from '@mui/material';
import { ReactNode } from 'react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

const StyledDangerZone = styled(Box)`
  display: grid;
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.secondary};
  gap: 16px;
  background: ${({ theme }) => theme.palette.tokens.background.danger};
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
  const rightPanelWidth = useGlobalContext((c) => c.layout.rightPanelWidth);
  const isSmOrLower = useMediaQuery(
    `@container (max-width: ${899 + rightPanelWidth}px)`
  );
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

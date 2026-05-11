import React from 'react';
import { Box, Card, CardProps, styled } from '@mui/material';

const StyledCard = styled(Card)`
  display: flex;
  align-items: center;
  padding: ${({ theme }) => theme.spacing(1.5)};
  background-color: ${({ theme }) => theme.palette.tokens.text._states.hover};
`;

const StyledContent = styled('span')`
  flex: 1;
  min-width: 0;
`;

const StyledActions = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  flex-shrink: 0;
  margin-left: ${({ theme }) => theme.spacing(1)};
`;

type Props = CardProps & {
  actions?: React.ReactNode;
};

export const TextBlock: React.FC<Props> = ({
  children,
  actions,
  ...cardProps
}) => {
  return (
    <StyledCard elevation={0} {...cardProps}>
      <StyledContent>{children}</StyledContent>
      {actions && <StyledActions>{actions}</StyledActions>}
    </StyledCard>
  );
};

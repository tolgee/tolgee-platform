import React from 'react';
import { ButtonBase, styled, Typography } from '@mui/material';

const StyledCard = styled(ButtonBase)`
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  padding: 22px;
  background: ${({ theme }) => theme.palette.background.paper};
  border: 1px solid ${({ theme }) => theme.palette.divider};
  border-radius: 10px;
  text-align: left;
  cursor: pointer;
  transition: border-color 140ms, background 140ms, transform 140ms;
  height: 100%;

  &:hover {
    border-color: ${({ theme }) => theme.palette.primary.main};
    background: ${({ theme }) => theme.palette.action.hover};
    transform: translateY(-1px);
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.palette.primary.main};
    outline-offset: 2px;
  }
`;

const StyledIcon = styled('div')`
  width: 44px;
  height: 44px;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.action.hover};
  color: ${({ theme }) => theme.palette.primary.main};
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
`;

type Props = {
  icon: React.ReactNode;
  title: React.ReactNode;
  description: React.ReactNode;
  onClick: () => void;
  dataCy: string;
};

export const EmptyTmWizardCard: React.VFC<Props> = ({
  icon,
  title,
  description,
  onClick,
  dataCy,
}) => {
  return (
    <StyledCard onClick={onClick} data-cy={dataCy}>
      <StyledIcon>{icon}</StyledIcon>
      <Typography
        variant="body1"
        fontWeight={500}
        color="text.primary"
        sx={{ letterSpacing: '0.15px' }}
      >
        {title}
      </Typography>
      <Typography
        variant="body2"
        color="text.secondary"
        sx={{ lineHeight: 1.5 }}
      >
        {description}
      </Typography>
    </StyledCard>
  );
};

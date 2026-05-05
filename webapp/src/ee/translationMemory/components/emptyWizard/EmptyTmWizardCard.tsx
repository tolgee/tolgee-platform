import React from 'react';
import { Button, Card, styled, Typography } from '@mui/material';

const StyledCard = styled(Card)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: ${({ theme }) => theme.spacing(2)};
  border-radius: 20px;
  width: 490px;
  padding: ${({ theme }) => theme.spacing(4)};
  background-color: ${({ theme }) =>
    theme.palette.tokens.background.onDefaultGrey};
`;

const StyledIcon = styled('div')`
  color: ${({ theme }) => theme.palette.primary.light};
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;

  & > svg {
    width: 32px;
    height: 32px;
  }
`;

const StyledDescription = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-bottom: ${({ theme }) => theme.spacing(5)};
`;

type Props = {
  icon: React.ReactNode;
  title: React.ReactNode;
  description: React.ReactNode;
  buttonLabel: React.ReactNode;
  onClick: () => void;
  dataCy: string;
};

export const EmptyTmWizardCard: React.VFC<Props> = ({
  icon,
  title,
  description,
  buttonLabel,
  onClick,
  dataCy,
}) => {
  return (
    <StyledCard elevation={0}>
      <StyledIcon>{icon}</StyledIcon>
      <Typography variant="h4">{title}</Typography>
      <StyledDescription variant="body1">{description}</StyledDescription>
      <Button
        onClick={onClick}
        variant="contained"
        color="primary"
        data-cy={dataCy}
      >
        {buttonLabel}
      </Button>
    </StyledCard>
  );
};

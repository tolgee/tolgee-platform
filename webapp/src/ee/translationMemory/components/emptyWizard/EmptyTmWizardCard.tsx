import React from 'react';
import { Button, Card, styled, Typography } from '@mui/material';

// Sized to fit three cards across a typical content area; falls back to wrapping below
// roughly 900px viewport width. Smaller than the Glossary 2-card variant (490px each) so all
// three options stack horizontally on the default project layout instead of wrapping the
// last one onto its own row.
const StyledCard = styled(Card)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: ${({ theme }) => theme.spacing(2)};
  border-radius: 20px;
  flex: 1 1 280px;
  max-width: 360px;
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

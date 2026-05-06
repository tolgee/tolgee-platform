import React from 'react';
import { Button, Card, styled, Typography } from '@mui/material';

// Width is controlled by the parent grid column (EmptyTmWizard); the card itself just
// arranges its inner content vertically. `justify-content: flex-start` + `margin-top: auto`
// on the button keeps the button anchored to the bottom of the card so the row of CTAs
// lines up across all three cards even when descriptions wrap to different line counts.
const StyledCard = styled(Card)`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  text-align: center;
  gap: ${({ theme }) => theme.spacing(2)};
  border-radius: 20px;
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
`;

const StyledButton = styled(Button)`
  margin-top: auto;
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
      <StyledButton
        onClick={onClick}
        variant="contained"
        color="primary"
        data-cy={dataCy}
      >
        {buttonLabel}
      </StyledButton>
    </StyledCard>
  );
};

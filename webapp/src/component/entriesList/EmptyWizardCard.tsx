import React from 'react';
import { Button, Card, styled, Typography } from '@mui/material';

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

const StyledFooter = styled('div')`
  margin-top: ${({ theme }) => theme.spacing(1)};
`;

type Props = {
  icon: React.ReactNode;
  title: React.ReactNode;
  description: React.ReactNode;
  buttonLabel: React.ReactNode;
  onClick: () => void;
  buttonDisabled?: boolean;
  dataCy: string;
  /** Optional content rendered below the button (e.g. a docs link). */
  footer?: React.ReactNode;
};

export const EmptyWizardCard: React.VFC<Props> = ({
  icon,
  title,
  description,
  buttonLabel,
  onClick,
  buttonDisabled,
  dataCy,
  footer,
}) => {
  return (
    <StyledCard elevation={0}>
      <StyledIcon>{icon}</StyledIcon>
      <Typography variant="h4">{title}</Typography>
      <StyledDescription variant="body1">{description}</StyledDescription>
      <StyledButton
        onClick={onClick}
        disabled={buttonDisabled}
        variant="contained"
        color="primary"
        data-cy={dataCy}
      >
        {buttonLabel}
      </StyledButton>
      {footer && <StyledFooter>{footer}</StyledFooter>}
    </StyledCard>
  );
};

import { styled } from '@mui/material';

const StyledContainer = styled('div')`
  display: grid;
  border: 1px solid ${({ theme }) => theme.palette.divider};
  border-radius: 20px;
  padding: 20px;
`;

const StyledHeader = styled('div')`
  display: grid;
`;

const StyledTitle = styled('div')`
  font-size: 24px;
`;

const StyledSubtitle = styled('div')`
  font-size: 14px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledContent = styled('div')``;

type Props = {
  title: React.ReactNode;
  subtitle?: string;
};

export const BillingSection: React.FC<Props> = ({
  title,
  subtitle,
  children,
}) => {
  return (
    <StyledContainer>
      <StyledHeader>
        <StyledTitle>{title}</StyledTitle>
        <StyledSubtitle>{subtitle}</StyledSubtitle>
      </StyledHeader>
      <StyledContent>{children}</StyledContent>
    </StyledContainer>
  );
};

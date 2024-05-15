import { styled } from '@mui/material';

export const SPLIT_CONTENT_BREAK_POINT = '(max-width: 800px)';
export const FULL_PAGE_BREAK_POINT = '(max-width: 550px)';

const StyledGrid = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 32px 48px;

  @media ${SPLIT_CONTENT_BREAK_POINT} {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto;
  }
`;

const StyledPrimaryPart = styled('div')`
  padding: 60px;
`;

const StyledSecondaryPart = styled('div')`
  padding: 60px;
  background: ${({ theme }) => theme.palette.login.backgroundSecondary};
`;

type Props = {
  primary: React.ReactNode;
  secondary: React.ReactNode;
};

export const SplitContent = ({ primary, secondary }: Props) => {
  return (
    <StyledGrid>
      <StyledPrimaryPart>{primary}</StyledPrimaryPart>

      <StyledSecondaryPart>{secondary}</StyledSecondaryPart>
    </StyledGrid>
  );
};

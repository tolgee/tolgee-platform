import { styled } from '@mui/material';

export const SPLIT_CONTENT_BREAK_POINT = '(max-width: 800px)';

const StyledGrid = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1px 1fr;
  gap: 32px 48px;

  @media ${SPLIT_CONTENT_BREAK_POINT} {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1px auto;
  }
`;

const StyledSpacer = styled('div')`
  display: grid;
  background: lightgrey;
  @media ${SPLIT_CONTENT_BREAK_POINT} {
    margin: 0px -8px;
  }
`;

type Props = {
  left: React.ReactNode;
  right: React.ReactNode;
};

export const SplitContent = ({ left, right }: Props) => {
  return (
    <StyledGrid>
      {left}

      <StyledSpacer />

      {right}
    </StyledGrid>
  );
};

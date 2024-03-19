import { Box, Typography, styled } from '@mui/material';

const StyledItem = styled(Box)`
  display: flex;
  justify-content: space-between;
`;

const StyledItemContent = styled(Typography)``;

type Props = {
  name: React.ReactNode;
  formula: React.ReactNode;
};

export const Shortcut = ({ name, formula }: Props) => {
  return (
    <StyledItem>
      <StyledItemContent
        variant="body2"
        data-cy="translations-shortcuts-command"
      >
        {name}
      </StyledItemContent>
      <StyledItemContent sx={{ whiteSpace: 'nowrap' }}>
        {formula}
      </StyledItemContent>
    </StyledItem>
  );
};

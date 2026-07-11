import { Box, Typography, styled } from '@mui/material';

const StyledItem = styled(Box)`
  display: flex;
  justify-content: space-between;
`;

type Props = {
  name: React.ReactNode;
  formula: React.ReactNode;
};

export const Shortcut = ({ name, formula }: Props) => {
  return (
    <StyledItem>
      <Typography
        component={Box}
        variant="body2"
        data-cy="translations-shortcuts-command"
      >
        {name}
      </Typography>
      <Typography component={Box} variant="body2" sx={{ whiteSpace: 'nowrap' }}>
        {formula}
      </Typography>
    </StyledItem>
  );
};

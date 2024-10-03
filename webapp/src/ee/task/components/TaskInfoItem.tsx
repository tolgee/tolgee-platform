import { Box, styled } from '@mui/material';

const StyledLabel = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
  padding-bottom: 4px;
`;

type Props = {
  label: React.ReactNode;
  value: React.ReactNode;
  'data-cy'?: string;
};

export const TaskInfoItem = ({ label, value, ...other }: Props) => {
  return (
    <Box {...other}>
      <StyledLabel>{label}</StyledLabel>
      <Box>{value}</Box>
    </Box>
  );
};

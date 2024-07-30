import { Box, styled } from '@mui/material';

const StyledLabel = styled(Box)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  label: React.ReactNode;
  value: React.ReactNode;
};

export const TaskInfoItem = ({ label, value }: Props) => {
  return (
    <Box>
      <StyledLabel>{label}</StyledLabel>
      <Box>{value}</Box>
    </Box>
  );
};

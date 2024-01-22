import { Check } from '@mui/icons-material';
import { Box, Typography } from '@mui/material';

type Props = {
  name: React.ReactNode;
};

export function PlanFeature({ name }: Props) {
  return (
    <Box display="flex" gap={0.5} alignItems="center">
      <Check style={{ fontSize: 16 }} />
      <Typography fontSize={14}>{name}</Typography>
    </Box>
  );
}

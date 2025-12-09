import { ChipProps, useTheme } from '@mui/material';
import { BaseChip } from 'tg.component/common/chips/DefaultChip';

export const SuccessChip = (props: ChipProps) => {
  const theme = useTheme();
  return (
    <BaseChip
      color="success"
      style={{
        backgroundColor: theme.palette.tokens.success.main,
      }}
      {...props}
    />
  );
};

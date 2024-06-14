import { Box, SxProps } from '@mui/material';
import {
  IncludedCreadits,
  IncludedSeats,
  IncludedStrings,
} from 'tg.component/billing/IncludedItem';
import { PlanType } from './types';

type Props = {
  includedUsage: PlanType['includedUsage'];
  sx?: SxProps;
  className?: string;
};

export const IncludedUsage = ({ includedUsage, sx, className }: Props) => {
  return (
    <Box display="grid" justifySelf="center" {...{ sx, className }}>
      <IncludedStrings
        className="strings"
        count={includedUsage?.translationSlots ?? -1}
      />
      <IncludedCreadits
        className="mt-credits"
        count={includedUsage?.mtCredits ?? -1}
      />
      <IncludedSeats className="seats" count={includedUsage?.seats ?? -1} />
    </Box>
  );
};

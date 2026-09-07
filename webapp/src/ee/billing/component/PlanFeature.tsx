import { Check } from '@untitled-ui/icons-react';
import { Box, SxProps, Typography } from '@mui/material';
import React from 'react';
import { wrapIf } from 'tg.fixtures/wrapIf';
import { StyledBillingLink } from 'tg.component/billing/Decorations';

type Props = {
  name: React.ReactNode;
  link?: string;
  bold?: boolean;
  title?: React.ReactNode;
  sx?: SxProps;
  className?: string;
  dataCy?: string;
};

export function PlanFeature({
  name,
  link,
  bold,
  title,
  sx,
  className,
  dataCy,
}: Props) {
  let item = wrapIf(bold, name, <b />);

  item = wrapIf(
    link,
    item,
    <StyledBillingLink href={link} target="_blank" rel="noreferrer noopener" />
  );

  return (
    <Box
      display="flex"
      gap={0.5}
      alignItems="center"
      data-cy={dataCy}
      {...{ sx, className }}
    >
      <Check style={{ width: 18, height: 18 }} />
      <Typography sx={{ wordBreak: 'break-word' }} fontSize={14}>
        {item}
      </Typography>
    </Box>
  );
}

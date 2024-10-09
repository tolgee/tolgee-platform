import { Check } from '@untitled-ui/icons-react';
import { Box, SxProps, Tooltip, Typography } from '@mui/material';
import React from 'react';
import { StyledBillingLink } from 'tg.component/billing/Decorations';
import { wrapIf } from 'tg.fixtures/wrapIf';

type Props = {
  name: React.ReactNode;
  link?: string;
  bold?: boolean;
  title?: React.ReactNode;
  sx?: SxProps;
  className?: string;
};

export function PlanFeature({ name, link, bold, title, sx, className }: Props) {
  let item = wrapIf(
    title,
    <span>{name}</span>,
    <Tooltip title={title} disableInteractive>
      <span />
    </Tooltip>
  );

  item = wrapIf(bold, name, <b />);

  item = wrapIf(
    link,
    item,
    <StyledBillingLink href={link} target="_blank" rel="noreferrer noopener" />
  );

  return (
    <Box display="flex" gap={0.5} alignItems="center" {...{ sx, className }}>
      <Check style={{ width: 18, height: 18 }} />
      <Typography sx={{ wordBreak: 'break-word' }} fontSize={14}>
        {item}
      </Typography>
    </Box>
  );
}

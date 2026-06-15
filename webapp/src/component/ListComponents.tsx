import { MenuItem, ListSubheader, styled } from '@mui/material';
import React from 'react';

const StyledMenuItem = styled(MenuItem)`
  height: 40px;
`;

const StyledListSubheader = styled(ListSubheader)`
  line-height: unset;
  padding-top: ${({ theme }) => theme.spacing(2)};
  padding-bottom: ${({ theme }) => theme.spacing(0.5)};
  background: inherit;
  background-image: inherit;
  position: static;
`;

export const CompactMenuItem = React.forwardRef<
  HTMLLIElement,
  React.ComponentProps<typeof MenuItem>
>(function CompactMenuItem(props, ref) {
  return <StyledMenuItem ref={ref} {...props} />;
});

export const CompactListSubheader: React.FC<
  React.PropsWithChildren<React.ComponentProps<typeof ListSubheader>>
> = (props) => {
  return <StyledListSubheader {...props} />;
};

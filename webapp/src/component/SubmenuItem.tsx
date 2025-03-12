import { ArrowRight } from 'tg.component/CustomIcons';
import { ListItemText, MenuItem, MenuItemProps, styled } from '@mui/material';
import React from 'react';

const StyledMenuItem = styled(MenuItem)`
  display: flex;
  justify-content: space-between;
`;

type Props = MenuItemProps & {
  label: React.ReactNode;
};

export const SubmenuItem = React.forwardRef(function SubmenuItem(
  { label, ...other }: Props,
  ref
) {
  return (
    <StyledMenuItem ref={ref as any} {...other}>
      <ListItemText primary={label} />
      <ArrowRight style={{ position: 'relative', left: '6px' }} />
    </StyledMenuItem>
  );
});

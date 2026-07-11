import { ArrowRight } from 'tg.component/CustomIcons';
import {
  ListItemText,
  MenuItem,
  MenuItemProps,
  styled,
  useTheme,
} from '@mui/material';
import React from 'react';

const StyledMenuItem = styled(MenuItem)`
  display: flex;
  justify-content: space-between;
`;

type Props = MenuItemProps & {
  label: React.ReactNode;
  open: boolean;
};

export const SubmenuItem = React.forwardRef(function SubmenuItem(
  { label, open, ...other }: Props,
  ref
) {
  const theme = useTheme();
  return (
    <StyledMenuItem
      ref={ref as any}
      sx={{
        background: open ? theme.palette.tokens.text._states.hover : undefined,
      }}
      data-cy="submenu-item"
      {...other}
    >
      <ListItemText primary={label} />
      <ArrowRight style={{ position: 'relative', left: '6px' }} />
    </StyledMenuItem>
  );
});

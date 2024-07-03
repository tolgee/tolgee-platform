import React, { useState } from 'react';
import { Checkbox, ListItemText, Menu, MenuItem, styled } from '@mui/material';
import { ArrowRight } from 'tg.component/CustomIcons';

import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { decodeFilter, OptionType } from './tools';
import { CompactMenuItem } from 'tg.component/ListComponents';

const StyledDot = styled('div')`
  width: 8px;
  height: 8px;
  border-radius: 4px;
  margin-left: ${({ theme }) => theme.spacing(2)};
`;

type Props = {
  item: OptionType;
  handleToggle: (rawValue: any) => () => void;
  activeFilters: string[];
};

export const SubmenuStates: React.FC<Props> = ({
  item,
  handleToggle,
  activeFilters,
}) => {
  const [menuOpen, setMenuOpen] = useState<HTMLElement | null>(null);

  const handleMenuClick = (e) => {
    if (menuOpen) {
      setMenuOpen(null);
    } else {
      setMenuOpen(e.currentTarget);
    }
  };

  const subFiltersNumber =
    item.submenu?.filter((i) => activeFilters.includes(i.value!))?.length || 0;

  return (
    <>
      <CompactMenuItem
        onClick={handleMenuClick}
        selected={Boolean(menuOpen || subFiltersNumber)}
      >
        <ListItemText
          primary={
            item.label + (subFiltersNumber ? ` (${subFiltersNumber})` : '')
          }
        />
        <ArrowRight />
      </CompactMenuItem>
      <Menu
        anchorOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
        open={Boolean(menuOpen)}
        anchorEl={menuOpen}
        onClose={() => setMenuOpen(null)}
      >
        {item.submenu?.map((item) => {
          const decodedValue = decodeFilter(item.value!);
          const state = (decodedValue.value as string).split(',')[1];
          return (
            <MenuItem
              data-cy="translations-filter-option"
              key={item.value}
              value={item.value!}
              onClick={handleToggle(item.value)}
            >
              <Checkbox
                size="small"
                edge="start"
                checked={activeFilters.includes(item.value!)}
                tabIndex={-1}
                disableRipple
              />
              <ListItemText primary={item.label} />
              <StyledDot
                style={{
                  background: TRANSLATION_STATES[state]?.color,
                }}
              />
            </MenuItem>
          );
        })}
      </Menu>
    </>
  );
};

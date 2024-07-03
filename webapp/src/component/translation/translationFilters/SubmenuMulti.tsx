import React, { useState } from 'react';
import { ListItemText, Popover } from '@mui/material';
import { ArrowRight } from 'tg.component/CustomIcons';

import { SearchSelectMulti } from 'tg.component/searchSelect/SearchSelectMulti';
import { CompactMenuItem } from 'tg.component/ListComponents';
import { OptionType } from './tools';

type Props = {
  item: OptionType;
  handleToggle: (rawValue: any) => () => void;
  activeFilters: string[];
};

export const SubmenuMulti: React.FC<Props> = React.forwardRef(
  function SubmenuMulti({ item, handleToggle, activeFilters }, ref) {
    const [menuOpen, setMenuOpen] = useState<HTMLElement | null>(null);

    const handleMenuClick = (e) => {
      if (menuOpen) {
        setMenuOpen(null);
      } else {
        setMenuOpen(e.currentTarget);
      }
    };

    const subFiltersNumber =
      item.submenu?.filter((i) => activeFilters.includes(i.value!))?.length ||
      0;

    return (
      <>
        <CompactMenuItem
          onClick={handleMenuClick}
          selected={Boolean(menuOpen || subFiltersNumber)}
          ref={ref as any}
        >
          <ListItemText
            primary={
              item.label + (subFiltersNumber ? ` (${subFiltersNumber})` : '')
            }
          />
          <ArrowRight />
        </CompactMenuItem>
        {item.submenu && (
          <Popover
            onKeyDown={(e) => e.stopPropagation()}
            anchorEl={menuOpen}
            open={Boolean(menuOpen)}
            onClose={handleMenuClick}
            anchorOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'left',
            }}
          >
            <SearchSelectMulti
              open={Boolean(menuOpen)}
              onClose={() => setMenuOpen(null)}
              onSelect={(value) => handleToggle(value)()}
              displaySearch={item.submenu.length > 10}
              minWidth={150}
              items={item.submenu.map((i) => ({
                value: i.value!,
                name: i.label,
              }))}
              value={activeFilters}
            />
          </Popover>
        )}
      </>
    );
  }
);

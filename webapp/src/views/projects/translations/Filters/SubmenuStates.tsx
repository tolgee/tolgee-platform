import { useState } from 'react';
import { Checkbox, ListItemText, Menu, MenuItem } from '@material-ui/core';
import { ArrowRight } from '@material-ui/icons';

import { decodeValue, OptionType } from './useAvailableFilters';
import { makeStyles } from '@material-ui/core';
import { translationStates } from 'tg.constants/translationStates';

const useStyles = makeStyles((theme) => ({
  item: {
    height: 50,
  },
  stateDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginLeft: theme.spacing(2),
  },
}));

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
  const classes = useStyles();
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
      <MenuItem
        onClick={handleMenuClick}
        selected={Boolean(menuOpen)}
        className={classes.item}
      >
        <ListItemText
          primary={
            item.label + (subFiltersNumber ? ` (${subFiltersNumber})` : '')
          }
        />
        <ArrowRight />
      </MenuItem>
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
          const decodedValue = decodeValue(item.value!);
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
              <div
                className={classes.stateDot}
                style={{
                  background: translationStates[state]?.color,
                }}
              />
            </MenuItem>
          );
        })}
      </Menu>
    </>
  );
};

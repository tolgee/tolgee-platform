import { useState } from 'react';
import { T } from '@tolgee/react';
import {
  Checkbox,
  ListItemText,
  Menu,
  MenuItem,
  makeStyles,
} from '@material-ui/core';
import { ArrowRight } from '@material-ui/icons';

import { OptionType } from './useAvailableFilters';

const useStyles = makeStyles((theme) => ({
  item: {
    height: 50,
  },
}));

type Props = {
  item: OptionType;
  handleToggle: (rawValue: any) => () => void;
  activeFilters: string[];
};

export const SubmenuTags: React.FC<Props> = ({
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
        selected={Boolean(menuOpen || subFiltersNumber)}
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
        {item.submenu?.length ? (
          item.submenu?.map((item) => {
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
              </MenuItem>
            );
          })
        ) : (
          <MenuItem className={classes.item} disabled>
            <T>translations_filters_tags_empty</T>
          </MenuItem>
        )}
      </Menu>
    </>
  );
};

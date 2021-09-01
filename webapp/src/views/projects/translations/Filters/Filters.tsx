import { useContextSelector } from 'use-context-selector';
import { T } from '@tolgee/react';
import { Clear } from '@material-ui/icons';
import {
  makeStyles,
  Select,
  ListSubheader,
  ListItemText,
  Checkbox,
  MenuItem,
  Typography,
  IconButton,
  Tooltip,
} from '@material-ui/core';

import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import {
  decodeValue,
  NON_EXCLUSIVE_FILTERS,
  useAvailableFilters,
} from './useAvailableFilters';
import { SubmenuStates } from './SubmenuStates';

const useStyles = makeStyles((theme) => ({
  wrapper: {
    display: 'flex',
    alignItems: 'center',
  },
  clearButton: {
    marginRight: theme.spacing(2),
    marginLeft: -theme.spacing(),
  },
  input: {
    minWidth: 70,
    height: 40,
    marginTop: 0,
    marginBottom: 0,
    '& p': {
      minWidth: 100,
    },
    '& div:focus': {
      backgroundColor: 'transparent',
    },
  },
  inputContent: {
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
}));

export const Filters = () => {
  const dispatch = useTranslationsDispatch();
  const filtersObj = useContextSelector(TranslationsContext, (v) => v.filters);
  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (v) => v.selectedLanguages
  );

  const activeFilters: string[] = [];
  Object.entries(filtersObj).forEach(([key, value]) => {
    if (!NON_EXCLUSIVE_FILTERS.includes(key)) {
      if (value) {
        activeFilters.push(
          JSON.stringify({
            filter: key,
            value: value,
          })
        );
      }
    } else {
      (value as string[]).forEach((filterVal) => {
        activeFilters.push(
          JSON.stringify({
            filter: key,
            value: filterVal,
          })
        );
      });
    }
  });

  const classes = useStyles();

  const availableFilters = useAvailableFilters(selectedLanguages);

  const findGroup = (value: string) =>
    availableFilters.find((g) => g.options?.find((o) => o.value === value));

  const findOption = (value: string) =>
    availableFilters
      .map((g) => g.options?.find((o) => o.value === value))
      .filter(Boolean)[0];

  const handleClearFilters = () => {
    dispatch({ type: 'SET_FILTERS', payload: {} });
  };

  const handleToggle = (rawValue) => () => {
    const jsonValue = JSON.parse(rawValue);
    const filterName =
      typeof jsonValue === 'string' ? jsonValue : jsonValue.filter;
    const filterValue = typeof jsonValue === 'string' ? true : jsonValue.value;

    const group = findGroup(rawValue);

    let newFilters = {
      ...filtersObj,
    };

    // remove all filters from new value group
    // so the groups are exclusive
    group?.options?.forEach((o) => {
      if (o.value) newFilters[decodeValue(o.value).filter] = undefined;
      o.submenu?.forEach((so) => {
        if (so.value) newFilters[decodeValue(so.value).filter] = undefined;
      });
    });

    let newValue: any;
    if (NON_EXCLUSIVE_FILTERS.includes(filterName)) {
      if (filtersObj[filterName]?.includes(filterValue)) {
        newValue = filtersObj[filterName].filter((v) => v !== filterValue);
      } else {
        newValue = [...(filtersObj[filterName] || []), filterValue];
      }
    } else {
      newValue =
        filtersObj[filterName] !== filterValue ? filterValue : undefined;
    }
    newFilters = {
      ...newFilters,
      [filterName]: newValue,
    };

    dispatch({ type: 'SET_FILTERS', payload: newFilters });
  };

  const options: any[] = [];

  availableFilters.forEach((group, i) => {
    if (!group.options?.length) {
      return;
    } else {
      options.push(
        <ListSubheader
          key={`${i}.group`}
          disableSticky
          data-cy="translations-filters-subheader"
        >
          {group.name}
        </ListSubheader>
      );

      group.options.forEach((option, i) => {
        if (option.value) {
          options.push(
            <MenuItem
              data-cy="translations-filter-option"
              key={option.value}
              value={option.value}
              onClick={handleToggle(option.value)}
            >
              <Checkbox
                size="small"
                edge="start"
                checked={activeFilters.includes(option.value)}
                tabIndex={-1}
                disableRipple
              />
              <ListItemText primary={option.label} />
            </MenuItem>
          );
        } else if (option.submenu) {
          options.push(
            <SubmenuStates
              key={i}
              data-cy="translations-filter-option"
              item={option}
              handleToggle={handleToggle}
              activeFilters={activeFilters}
            />
          );
        }
      });
    }
  });

  return (
    <div className={classes.wrapper}>
      <Select
        className={classes.input}
        variant="outlined"
        value={activeFilters}
        data-cy="translations-filter-select"
        endAdornment={
          Boolean(activeFilters.length) && (
            <Tooltip title={<T noWrap>translations_filters_heading_clear</T>}>
              <IconButton
                size="small"
                className={classes.clearButton}
                onClick={handleClearFilters}
                data-cy="translations-filter-clear-all"
              >
                <Clear />
              </IconButton>
            </Tooltip>
          )
        }
        renderValue={(value: any) => (
          <Typography
            color={value.length === 0 ? 'textSecondary' : 'textPrimary'}
            variant="body2"
            className={classes.inputContent}
          >
            {value.length === 0 ? (
              <T>translations_filter_placeholder</T>
            ) : value.length === 1 && findOption(value[0])?.label ? (
              findOption(value[0])?.label
            ) : (
              <T parameters={{ filtersNum: String(activeFilters.length) }}>
                translations_filters_text
              </T>
            )}
          </Typography>
        )}
        MenuProps={{
          variant: 'menu',
          getContentAnchorEl: null,
        }}
        margin="dense"
        displayEmpty
        multiple
      >
        {options}
      </Select>
    </div>
  );
};

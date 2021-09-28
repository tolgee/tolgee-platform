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
  NON_EXCLUSIVE_FILTERS,
  toggleFilter,
  useAvailableFilters,
} from './useAvailableFilters';
import { SubmenuStates } from './SubmenuStates';
import { SubmenuTags } from './SubmenuTags';

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
    maxHeight: 40,
    marginTop: 0,
    marginBottom: 0,
    width: 200,
    '& div:focus': {
      backgroundColor: 'transparent',
    },
    '& .MuiSelect-root': {
      paddingRight: 5,
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

  const findOption = (value: string) =>
    availableFilters
      .map((g) => g.options?.find((o) => o.value === value))
      .filter(Boolean)[0];

  const handleClearFilters = () => {
    dispatch({ type: 'SET_FILTERS', payload: {} });
  };

  const handleFilterToggle = (rawValue: string) => () => {
    const newFilters = toggleFilter(filtersObj, availableFilters, rawValue);
    dispatch({ type: 'SET_FILTERS', payload: newFilters });
  };

  const options: any[] = [];

  availableFilters.forEach((group, i) => {
    if (!group.options?.length) {
      return;
    } else {
      if (group.type !== 'tags') {
        options.push(
          <ListSubheader
            key={`${i}.group`}
            disableSticky
            data-cy="translations-filters-subheader"
          >
            {group.name}
          </ListSubheader>
        );
      }

      group.options.forEach((option, i) => {
        if (group.type === undefined) {
          options.push(
            <MenuItem
              data-cy="translations-filter-option"
              key={option.value}
              value={option.value!}
              onClick={handleFilterToggle(option.value!)}
            >
              <Checkbox
                size="small"
                edge="start"
                checked={activeFilters.includes(option.value!)}
                tabIndex={-1}
                disableRipple
              />
              <ListItemText primary={option.label} />
            </MenuItem>
          );
        } else if (group.type === 'states') {
          options.push(
            <SubmenuStates
              key={'states'}
              data-cy="translations-filter-option"
              item={option}
              handleToggle={handleFilterToggle}
              activeFilters={activeFilters}
            />
          );
        } else if (group.type === 'tags') {
          options.push(
            <SubmenuTags
              key={i}
              data-cy="translations-filter-option"
              item={option}
              handleToggle={handleFilterToggle}
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

import { T } from '@tolgee/react';
import { Clear } from '@material-ui/icons';
import {
  makeStyles,
  Select,
  ListItemText,
  Checkbox,
  Typography,
  IconButton,
  Tooltip,
  useTheme,
} from '@material-ui/core';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import {
  NON_EXCLUSIVE_FILTERS,
  toggleFilter,
  useAvailableFilters,
} from './useAvailableFilters';
import { SubmenuStates } from './SubmenuStates';
import { SubmenuTags } from './SubmenuTags';
import { CompactListSubheader, CompactMenuItem } from './FiltersComponents';

const useStyles = makeStyles((theme) => ({
  wrapper: {
    display: 'flex',
    alignItems: 'center',
  },
  input: {
    height: 40,
    marginTop: 0,
    marginBottom: 0,
    display: 'flex',
    alignItems: 'center',
    width: 200,
    '& div:focus': {
      backgroundColor: 'transparent',
    },
    '& .MuiSelect-root': {
      display: 'flex',
      alignItems: 'center',
      overflow: 'hidden',
      position: 'relative',
    },
  },
  inputContent: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    margin: theme.spacing(-1, 0),
    width: '100%',
  },
  inputText: {
    overflow: 'hidden',
    flexShrink: 1,
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  clearButton: {
    margin: theme.spacing(-1, -0.5, -1, -0.25),
  },
}));

export const Filters = () => {
  const dispatch = useTranslationsDispatch();
  const filtersObj = useTranslationsSelector((v) => v.filters);
  const selectedLanguages = useTranslationsSelector((v) => v.selectedLanguages);

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
      (value as unknown as string[]).forEach((filterVal) => {
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
  const theme = useTheme();

  const availableFilters = useAvailableFilters(selectedLanguages);

  const findOption = (value: string) =>
    availableFilters
      .map((g) => g.options?.find((o) => o.value === value))
      .filter(Boolean)[0];

  const handleClearFilters = (e) => {
    dispatch({ type: 'SET_FILTERS', payload: {} });
  };

  const handleFilterToggle = (rawValue: string) => () => {
    const newFilters = toggleFilter(filtersObj, availableFilters, rawValue);
    dispatch({ type: 'SET_FILTERS', payload: newFilters });
  };

  const options: any[] = [];

  availableFilters.forEach((group, i1) => {
    if (!group.options?.length) {
      return;
    } else {
      if (group.type !== 'tags') {
        options.push(
          <CompactListSubheader
            key={i1}
            disableSticky
            data-cy="translations-filters-subheader"
          >
            {group.name}
          </CompactListSubheader>
        );
      }

      group.options.forEach((option, i2) => {
        if (group.type === undefined) {
          options.push(
            <CompactMenuItem
              data-cy="translations-filter-option"
              key={`${i1}.${i2}`}
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
            </CompactMenuItem>
          );
        } else if (group.type === 'states') {
          options.push(
            <SubmenuStates
              key={`${i1}.${i2}`}
              data-cy="translations-filter-option"
              item={option}
              handleToggle={handleFilterToggle}
              activeFilters={activeFilters}
            />
          );
        } else if (group.type === 'tags') {
          options.push(
            <SubmenuTags
              key={`${i1}.${i2}`}
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
        renderValue={(value: any) => (
          <div className={classes.inputContent}>
            <Typography
              style={{
                color:
                  value.length === 0
                    ? theme.palette.grey[500]
                    : theme.palette.text.primary,
              }}
              variant="body2"
              className={classes.inputText}
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
            {Boolean(activeFilters.length) && (
              <Tooltip title={<T noWrap>translations_filters_heading_clear</T>}>
                <IconButton
                  size="small"
                  className={classes.clearButton}
                  onClick={stopAndPrevent(handleClearFilters)}
                  onMouseDown={stopAndPrevent()}
                  data-cy="translations-filter-clear-all"
                >
                  <Clear fontSize="small" />
                </IconButton>
              </Tooltip>
            )}
          </div>
        )}
        MenuProps={{
          variant: 'menu',
          getContentAnchorEl: null,
          anchorOrigin: {
            vertical: 'bottom',
            horizontal: 'left',
          },
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

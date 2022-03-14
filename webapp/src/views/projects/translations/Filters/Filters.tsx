import { T } from '@tolgee/react';
import { Clear } from '@material-ui/icons';
import {
  makeStyles,
  Select,
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
import { useAvailableFilters } from './useAvailableFilters';
import { useActiveFilters } from './useActiveFilters';
import { useFiltersContent } from './useFiltersContent';

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
  const selectedLanguages = useTranslationsSelector((v) => v.selectedLanguages);

  const activeFilters = useActiveFilters();

  const classes = useStyles();
  const theme = useTheme();

  const availableFilters = useAvailableFilters(selectedLanguages);

  const findOption = (value: string) =>
    availableFilters
      .map((g) => g.options?.find((o) => o.value === value))
      .filter(Boolean)[0];

  const filtersContent = useFiltersContent();

  const handleClearFilters = (e) => {
    dispatch({ type: 'SET_FILTERS', payload: {} });
  };

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
        {filtersContent}
      </Select>
    </div>
  );
};

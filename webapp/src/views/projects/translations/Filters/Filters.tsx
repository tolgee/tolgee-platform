import { T } from '@tolgee/react';
import {
  makeStyles,
  Select,
  ListSubheader,
  ListItemText,
  Checkbox,
  MenuItem,
  Typography,
} from '@material-ui/core';
import { useContextSelector } from 'use-context-selector';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { useAvailableFilters } from './useAvailableFilters';

const useStyles = makeStyles({
  input: {
    minWidth: 70,
    height: 40,
    marginTop: 0,
    marginBottom: 0,
    '& p': {
      minWidth: 100,
    },
  },
});

export const Filters = () => {
  const dispatch = useTranslationsDispatch();
  const filtersObj = useContextSelector(TranslationsContext, (v) => v.filters);
  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (v) => v.selectedLanguages
  );
  const activeFilters = Object.entries(filtersObj)
    .filter(
      ([_key, value]) =>
        Boolean(value) &&
        (typeof value !== 'string' || selectedLanguages?.includes(value))
    )
    .map(([key, value]) =>
      JSON.stringify({
        filter: key,
        value: value,
      })
    );

  const classes = useStyles();

  const availableFilters = useAvailableFilters(selectedLanguages);

  const findGroup = (value: string) =>
    availableFilters.find((g) => g.options?.find((o) => o.value === value));

  const findOption = (value: string) =>
    availableFilters
      .map((g) => g.options?.find((o) => o.value === value))
      .filter(Boolean)[0];

  const decodeValue = (value: string) =>
    JSON.parse(value) as { filter: string; value: string | boolean };

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
      newFilters[decodeValue(o.value).filter] = undefined;
    });

    newFilters = {
      ...newFilters,
      [filterName]:
        filtersObj[filterName] !== filterValue ? filterValue : undefined,
    };
    dispatch({ type: 'SET_FILTERS', payload: newFilters });
  };

  const options: any[] = [];

  availableFilters.forEach((group, i) => {
    if (group.options?.length) {
      options.push(
        <ListSubheader key={`${i}.group`} disableSticky>
          {group.name}
        </ListSubheader>
      );

      group.options.forEach((option) => {
        options.push(
          <MenuItem
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
      });
    }
  });

  return (
    <Select
      className={classes.input}
      variant="outlined"
      value={activeFilters}
      renderValue={(value: any) =>
        value.length === 0 ? (
          <Typography color="textSecondary" variant="body2">
            <T>translations_filter_placeholder</T>
          </Typography>
        ) : value.length === 1 && findOption(value[0])?.label ? (
          <Typography color="textPrimary" variant="body2">
            {findOption(value[0])?.label}
          </Typography>
        ) : (
          <Typography color="textPrimary" variant="body2">
            <T parameters={{ filtersNum: String(activeFilters.length) }}>
              translations_filters_text
            </T>
          </Typography>
        )
      }
      MenuProps={{
        variant: 'menu',
      }}
      margin="dense"
      displayEmpty
      multiple
    >
      {options}
    </Select>
  );
};

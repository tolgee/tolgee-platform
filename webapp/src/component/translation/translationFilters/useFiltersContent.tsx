import { Checkbox, ListItemText } from '@mui/material';

import { SubmenuStates } from './SubmenuStates';
import { SubmenuMulti } from './SubmenuMulti';
import { FilterOptions, useAvailableFilters } from './useAvailableFilters';
import { toggleFilter } from './tools';
import {
  CompactListSubheader,
  CompactMenuItem,
} from 'tg.component/ListComponents';
import { FiltersType, LanguageModel } from './tools';
import { getActiveFilters } from './getActiveFilters';

export const useFiltersContent = (
  filtersObj: FiltersType,
  onChange: (value: FiltersType) => void,
  selectedLanguages?: LanguageModel[],
  filterOptions?: FilterOptions
) => {
  const options: any[] = [];

  const activeFilters = getActiveFilters(filtersObj);

  const handleFilterToggle = (rawValue: string) => () => {
    const newFilters = toggleFilter(filtersObj, availableFilters, rawValue);
    onChange(newFilters);
  };

  const { availableFilters, refresh } = useAvailableFilters(
    selectedLanguages,
    filterOptions
  );

  availableFilters.forEach((group, i1) => {
    if (group.options?.length) {
      if (group.type !== 'multi' && group.name) {
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
        } else if (group.type === 'multi') {
          options.push(
            <SubmenuMulti
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
  return { options, refresh };
};

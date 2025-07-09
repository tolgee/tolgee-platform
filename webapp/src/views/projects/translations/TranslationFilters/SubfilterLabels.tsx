import React, { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { SubmenuItem } from 'tg.component/SubmenuItem';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';

import { FiltersInternal, FilterActions, LanguageModel } from './tools';
import { FilterItem } from './FilterItem';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { TranslationLabel } from 'tg.component/TranslationLabel';
import { CompactListSubheader } from 'tg.component/ListComponents';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';
import { useLabelsService } from 'tg.views/projects/translations/context/services/useLabelsService';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from 'tg.views/projects/translations/context/TranslationsContext';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

type LabelModel = components['schemas']['LabelModel'];

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
  selectedLanguages: LanguageModel[];
};

export const SubfilterLabels = ({
  value,
  actions,
  projectId,
  selectedLanguages,
}: Props) => {
  const { isEnabled } = useEnabledFeatures();
  const labelsEnabled = isEnabled('TRANSLATION_LABELS');
  if (!labelsEnabled) {
    return null;
  }
  const allLabels = useTranslationsSelector((c) => c.labels);

  if (allLabels.length === 0) {
    return null;
  }

  const {
    labels: searched,
    totalItems,
    setSearch,
    searchDebounced,
    search,
    loadableList,
  } = useLabelsService({ projectId });

  const labels = searched || allLabels;

  const [expanded, setExpanded] = useState(
    value.filterTranslationLanguage !== undefined
  );
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);

  function toggleFilterLanguage(
    newValue: FiltersInternal['filterTranslationLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterTranslationLanguage:
        newValue === value.filterTranslationLanguage ? undefined : newValue,
    });
  }

  const handleToggleLabel = (id: string) => {
    if (value.filterLabel?.includes(id)) {
      actions.removeFilter('filterLabel', id);
    } else {
      actions.addFilter('filterLabel', id);
    }
  };

  const handleFetchMore = async () => {
    if (loadableList.hasNextPage && !loadableList.isFetching) {
      await loadableList.fetchNextPage();
    }
  };

  function renderItem(
    props: React.HTMLAttributes<HTMLLIElement>,
    item: LabelModel
  ) {
    return (
      <FilterItem
        label={<TranslationLabel label={item} tooltip={item.description} />}
        selected={Boolean(value.filterLabel?.includes(item.id.toString()))}
        onClick={() => handleToggleLabel(item.id.toString())}
      />
    );
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl}
        label={t('translations_filters_heading_labels')}
        onClick={() => setOpen(true)}
        selected={Boolean(getLabelFiltersLength(value))}
        open={open}
      />
      {open && (
        <Menu
          open={open}
          anchorEl={anchorEl.current!}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          onClose={() => {
            setOpen(false);
            setSearch('');
          }}
        >
          <SmoothProgress
            loading={Boolean(
              loadableList.isFetching &&
                (searchDebounced || totalItems === undefined)
            )}
            sx={{ position: 'absolute', top: 0, left: 0, right: 0 }}
          />
          <Box display="grid">
            <InfiniteSearchSelectContent
              open={true}
              items={labels}
              maxWidth={400}
              onSearch={setSearch}
              search={search}
              displaySearch={(totalItems ?? 0) > 10}
              renderOption={renderItem}
              itemKey={(o) => o.name}
              ListboxProps={{
                style: { maxHeight: 400, overflow: 'auto' },
              }}
              searchPlaceholder={t(
                'translations_filters_labels_search_placeholder'
              )}
              onGetMoreData={handleFetchMore}
            />
            <CompactListSubheader>
              <Box display="flex" justifyContent="space-between">
                <Box>{t('translations_filter_languages_select_title')}</Box>
              </Box>
            </CompactListSubheader>
            <FilterItem
              data-cy="translations-filter-apply-no-base"
              label={t('translations_filter_languages_no_base')}
              selected={value.filterTranslationLanguage === undefined}
              onClick={() => toggleFilterLanguage(undefined)}
              exclusive
            />
            {expanded && (
              <>
                <FilterItem
                  data-cy="translations-filter-apply-for-all"
                  label={t('translations_filter_languages_all')}
                  selected={value.filterTranslationLanguage === true}
                  onClick={() => toggleFilterLanguage(true)}
                  exclusive
                />
                {selectedLanguages?.map((lang) => {
                  return (
                    <FilterItem
                      data-cy="translations-filter-apply-for-language"
                      key={lang.id}
                      label={lang.name}
                      selected={value.filterTranslationLanguage === lang.tag}
                      onClick={() => toggleFilterLanguage(lang.tag)}
                      exclusive
                    />
                  );
                })}
              </>
            )}
            <MenuItem
              data-cy="translations-filter-apply-for-expand"
              role="button"
              onClick={() => setExpanded((value) => !value)}
              sx={{
                display: 'flex',
                justifyContent: 'center',
              }}
            >
              {expanded ? <ChevronUp /> : <ChevronDown />}
            </MenuItem>
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getLabelFiltersLength(value: FiltersInternal) {
  return value.filterLabel?.length ?? 0;
}

export function getLabelFiltersName(value: FiltersInternal) {
  if (value.filterLabel?.length) {
    const { fetchLabels } = useTranslationsActions();
    const filterLabels = value.filterLabel?.map((id) => Number(id)) || [];
    const labelId = value.filterLabel[0];
    const label = fetchLabels(filterLabels)?.find(
      (l) => l.id.toString() === labelId
    );
    return label ? <TranslationLabel label={label} /> : null;
  }
}

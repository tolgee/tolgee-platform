import React from 'react';
import { useProject } from 'tg.hooks/useProject';
import { Box, MenuItem, styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';
import { useLabelsService } from 'tg.views/projects/translations/context/services/useLabelsService';
import { useTranslate } from '@tolgee/react';
import { TranslationLabel } from 'tg.component/TranslationLabel';
import { InfiniteSearchSelectContent } from 'tg.component/searchSelect/InfiniteSearchSelectContent';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';

type LabelModel = components['schemas']['LabelModel'];

const StyledOption = styled('div')`
  overflow: hidden;
  white-space: nowrap;
`;

type LabelSelectorProps = {
  onSelect: (label: LabelModel) => void;
  existing?: LabelModel[];
};

export const LabelSelector = ({ existing, onSelect }: LabelSelectorProps) => {
  const { t } = useTranslate();
  const project = useProject();
  const {
    labels: searched,
    loadableList,
    search,
    setSearch,
  } = useLabelsService({
    projectId: project.id,
  });
  const prefetched = useTranslationsSelector((c) => c.labels) || [];

  const labels = searched || prefetched;

  const options = labels.filter((label: LabelModel) => {
    return !(existing && existing.some((l) => l.id === label.id));
  });

  const fetchMore = async () => {
    if (loadableList.hasNextPage && !loadableList.isFetching) {
      await loadableList.fetchNextPage();
    }
  };

  function renderItem(props: any, item: LabelModel) {
    return (
      <MenuItem
        {...props}
        onClick={(e) => {
          e.stopPropagation();
          onSelect?.(item);
          setSearch('');
        }}
      >
        <StyledOption data-cy="label-autocomplete-option">
          <TranslationLabel label={item} tooltip={item.description} />
        </StyledOption>
      </MenuItem>
    );
  }

  return (
    <Box display="grid" className="label-autocomplete">
      <InfiniteSearchSelectContent<LabelModel>
        open={true}
        data-cy="label-selector-autocomplete"
        items={options}
        onSearch={setSearch}
        search={search}
        displaySearch
        ListboxProps={{
          style: { maxHeight: 400, overflow: 'auto' },
        }}
        onGetMoreData={fetchMore}
        searchPlaceholder={t('translations_filters_labels_search_placeholder')}
        searchInputOnClick={(e) => {
          e.stopPropagation();
        }}
        itemKey={(o) => o.name}
        renderOption={renderItem}
      />
    </Box>
  );
};

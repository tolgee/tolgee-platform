import { components } from 'tg.service/apiSchema.generated';
import { useLabels } from 'tg.hooks/useLabels';
import { FiltersType } from 'tg.views/projects/translations/TranslationFilters/tools';
import { useEffect } from 'react';

type LabelModel = components['schemas']['LabelModel'];

export type FilterData = {
  labels?: LabelModel[];
};

export const useFilterData = (value: FiltersType, projectId: number) => {
  const { labels, fetchSelected: fetchSelectedLabels } = useLabels({
    projectId,
  });
  const filterData: FilterData = {};

  useEffect(() => {
    fetchLabels();
  }, [value.filterLabel]);

  filterData.labels = labels;

  const fetchLabels = () => {
    if ((value.filterLabel?.length || 0) > 0) {
      fetchSelectedLabels(value.filterLabel?.map((id) => Number(id)) || []);
    }
  };
  return {
    filterData,
  };
};

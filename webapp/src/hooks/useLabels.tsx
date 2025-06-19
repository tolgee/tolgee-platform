import { useEffect, useState } from 'react';
import {
  invalidateUrlPrefix,
  useApiInfiniteQuery,
  useApiQuery,
} from 'tg.service/http/useQueryApi';
import { useDebounce } from 'use-debounce';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { useRemoveLabel, usePutLabel } from 'tg.service/TranslationHooks';
import { useQueryClient } from 'react-query';
import { useTranslationsService } from 'tg.views/projects/translations/context/services/useTranslationsService';
import {
  AddLabel,
  RemoveLabel,
} from 'tg.views/projects/translations/context/types';

type LabelModel = components['schemas']['LabelModel'];

type Props = {
  projectId?: number;
  translations?: ReturnType<typeof useTranslationsService>;
};

export const useLabels = ({ projectId, translations }: Props) => {
  const [search, setSearch] = useState('');
  const [totalItems, setTotalItems] = useState<number | undefined>(undefined);
  const [searchDebounced] = useDebounce(search, 500);
  const [labels, setLabels] = useState<LabelModel[]>([]);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [enabledSelected, setEnabledSelected] = useState<boolean>(false);
  const putLabel = usePutLabel();
  const deleteLabel = useRemoveLabel();
  const queryClient = useQueryClient();

  projectId = projectId || useProject().id;

  const query = {
    search: searchDebounced,
    size: 20,
  };

  const loadableList = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/labels',
    method: 'get',
    path: {
      projectId,
    },
    query,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      onSuccess(data) {
        if (
          totalItems === undefined &&
          data.pages[0]?.page?.totalElements !== undefined
        ) {
          setTotalItems(data.pages[0].page.totalElements);
        }
      },
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: {
              projectId,
            },
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const loadableSelected = useApiQuery({
    url: '/v2/projects/{projectId}/labels/ids',
    method: 'get',
    path: {
      projectId,
    },
    query: {
      id: selectedIds,
    },
    options: {
      enabled: enabledSelected,
    },
  });

  function fetchSelected(ids: number[]) {
    // get list of ids which are missing in labels
    const missingIds = ids.filter((id) => !labels.find((l) => l.id === id));
    if (missingIds.length === 0) {
      setEnabledSelected(false);
      return;
    }
    setSelectedIds(ids);
    setEnabledSelected(true);
  }

  const addLabel = (data: AddLabel) => {
    if (!translations) {
      return;
    }
    putLabel
      .mutateAsync({
        path: {
          projectId: projectId,
          translationId: data.translationId,
          labelId: data.labelId,
        },
      })
      .then((response: LabelModel) => {
        const previousLabels =
          translations.fixedTranslations?.find(
            (key) => key.keyId === data.keyId
          )?.translations[data.language]?.labels || [];
        translations?.updateTranslation({
          keyId: data.keyId,
          lang: data.language,
          data: {
            labels: [...previousLabels, response],
          },
        });
      })
      .catch((e) => {
        return new Promise(() => {});
      });
  };

  const removeLabel = (data: RemoveLabel) => {
    if (!translations) {
      return;
    }
    deleteLabel
      .mutateAsync({
        path: {
          projectId: projectId,
          translationId: data.translationId,
          labelId: data.labelId,
        },
      })
      .then(() => {
        const previousLabels =
          translations.fixedTranslations?.find(
            (key) => key.keyId === data.keyId
          )?.translations[data.language]?.labels || [];
        translations?.updateTranslation({
          keyId: data.keyId,
          lang: data.language,
          data: {
            labels: previousLabels.filter((l) => l.id !== data.labelId),
          },
        });
        invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/labels');
      })
      .catch((e) => {
        return new Promise(() => {});
      });
  };

  // Merge selected labels into the all labels list, avoiding duplicates
  useEffect(() => {
    const allLabels: LabelModel[] = [];
    // Gather all paginated labels
    if (loadableList.data?.pages) {
      loadableList.data.pages.forEach((page) => {
        if (page._embedded?.labels) {
          allLabels.push(...page._embedded.labels);
        }
      });
    }

    // Add selected labels if not already present
    if (loadableSelected.data) {
      loadableSelected.data.forEach((selected) => {
        if (!allLabels.find((l) => l.id === selected.id)) {
          allLabels.push(selected);
        }
      });
    }
    setLabels(allLabels);
  }, [loadableList.data, loadableSelected.data]);

  return {
    labels,
    loadableList,
    loadableSelected,
    setSearch,
    totalItems,
    search,
    searchDebounced,
    setSelectedIds,
    fetchSelected,
    addLabel,
    removeLabel,
  };
};

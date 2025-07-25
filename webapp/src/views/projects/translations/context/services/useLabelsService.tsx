import { useState } from 'react';
import { useApiInfiniteQuery, useApiQuery } from 'tg.service/http/useQueryApi';
import { useDebounce } from 'use-debounce';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import {
  useRemoveLabel,
  usePutLabel,
  usePutLabelWithoutTranslation,
} from 'tg.service/TranslationHooks';
import { useTranslationsService } from 'tg.views/projects/translations/context/services/useTranslationsService';
import {
  AddLabel,
  RemoveLabel,
} from 'tg.views/projects/translations/context/types';

type LabelModel = components['schemas']['LabelModel'];

type Props = {
  projectId?: number;
  translations?: ReturnType<typeof useTranslationsService>;
  enabled?: boolean;
};

export const useLabelsService = ({
  projectId,
  translations,
  enabled = true,
}: Props) => {
  const [search, setSearch] = useState('');
  const [totalItems, setTotalItems] = useState<number | undefined>(undefined);
  const [searchDebounced] = useDebounce(search, 500);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [enabledSelected, setEnabledSelected] = useState<boolean>(false);
  const putLabel = usePutLabel();
  const deleteLabel = useRemoveLabel();
  const putLabelWithoutTranslation = usePutLabelWithoutTranslation();

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
      enabled,
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

  const labels =
    loadableList.data?.pages.flatMap((p) => p._embedded?.labels ?? []) || [];

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
      enabled: enabled && enabledSelected,
      noGlobalLoading: true,
    },
  });

  const selectedLabels = getSelectedLabels();

  function getSelectedLabels() {
    if (loadableList.isFetched) {
      const selected = labels.filter((l) => selectedIds.includes(l.id));
      if (selected.length === selectedIds.length) {
        return selected;
      }
    }
    if (!enabledSelected && selectedIds.length > 0) {
      setEnabledSelected(true);
    }
    if (loadableSelected.isFetched) {
      return loadableSelected.data;
    }

    return [];
  }

  const addLabel = (data: AddLabel) => {
    if (!translations) {
      return;
    }
    let promise: Promise<LabelModel>;
    if (data.translationId) {
      promise = putLabel.mutateAsync({
        path: {
          projectId: projectId,
          translationId: data.translationId,
          labelId: data.labelId,
        },
      });
    } else {
      promise = putLabelWithoutTranslation.mutateAsync({
        path: {
          projectId: projectId,
        },
        content: {
          'application/json': {
            labelId: data.labelId,
            languageId: data.language.id,
            keyId: data.keyId,
          },
        },
      });
    }
    promise.then((response: LabelModel) => {
      const previousLabels =
        translations.fixedTranslations?.find((key) => key.keyId === data.keyId)
          ?.translations[data.language.tag]?.labels || [];
      translations?.updateTranslation({
        keyId: data.keyId,
        lang: data.language.tag,
        data: {
          labels: [...previousLabels, response].sort((a, b) =>
            a.name.toLowerCase().localeCompare(b.name.toLowerCase())
          ),
        },
      });
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
          )?.translations[data.language.tag]?.labels || [];
        translations?.updateTranslation({
          keyId: data.keyId,
          lang: data.language.tag,
          data: {
            labels: previousLabels.filter((l) => l.id !== data.labelId),
          },
        });
      });
  };

  return {
    labels,
    loadableList,
    setSearch,
    totalItems,
    search,
    searchDebounced,
    addLabel,
    removeLabel,
    selectedLabels,
    setSelectedIds,
    isLoading: putLabel.isLoading || deleteLabel.isLoading,
  };
};

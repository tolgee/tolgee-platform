import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import { useQueryClient } from 'react-query';
import { useTranslate, T } from '@tolgee/react';
import { Box } from '@mui/material';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import {
  invalidateUrlPrefix,
  useApiInfiniteQuery,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useBranchFromUrlPath } from 'tg.component/branching/useBranchFromUrlPath';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { BaseProjectView } from '../../BaseProjectView';
import { TrashBanner } from './TrashBanner';
import { TrashBatchBar } from './TrashBatchBar';
import { TrashControls } from './TrashControls';
import { TrashTable } from './TrashTable';
import { useColumns } from '../useColumns';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslationFilters } from '../TranslationFilters/useTranslationFilters';
import { FiltersInternal, FiltersType } from '../TranslationFilters/tools';

type LanguageModel = components['schemas']['LanguageModel'];
type TrashedKeyWithTranslationsModel =
  components['schemas']['TrashedKeyWithTranslationsModel'];

const PAGE_SIZE = 60;
const TRASHED_COLUMN_WIDTH = 200;

export const TrashPage = () => {
  const { t } = useTranslate();
  const project = useProject();
  const queryClient = useQueryClient();
  const branchName = useBranchFromUrlPath();
  const { satisfiesPermission } = useProjectPermissions();
  const canDelete = satisfiesPermission('keys.delete');
  const canRestore = satisfiesPermission('keys.create');
  const containerRef = useRef<HTMLDivElement>(null);
  const [containerWidth, setContainerWidth] = useState(0);

  const [search, setSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });
  const [filtersJson, setFiltersJson] = useUrlSearchState('filters', {
    defaultVal: '{}',
  });
  const parsedFilters: FiltersInternal = useMemo(() => {
    try {
      return JSON.parse(filtersJson || '{}');
    } catch {
      return {};
    }
  }, [filtersJson]);
  const setFilters = useCallback(
    (f: FiltersInternal) => {
      setFiltersJson(JSON.stringify(f));
    },
    [setFiltersJson]
  );
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<number[]>([]);
  const [order, setOrder] = useUrlSearchState('order', {
    defaultVal: 'deletedAt',
  });

  const TRASH_DEFAULT_ORDER = 'deletedAt';

  const trashSortOptions = useMemo(
    () => [
      { value: 'keyName', label: t('translation_sort_item_key_name_a_to_z') },
      {
        value: 'keyName,desc',
        label: t('translation_sort_item_key_name_z_to_a'),
      },
      {
        value: 'deletedAt,desc',
        label: t('trash_sort_item_last_deleted_on_top'),
      },
      {
        value: 'deletedAt',
        label: t('trash_sort_item_first_deleted_on_top'),
      },
    ],
    [t]
  );

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const observer = new ResizeObserver((entries) => {
      setContainerWidth(entries[0].contentRect.width);
    });
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000, sort: ['name'] },
    options: {
      cacheTime: 0,
    },
  });

  const allLanguages = useMemo(
    () => languagesLoadable.data?._embedded?.languages ?? [],
    [languagesLoadable.data]
  );

  const baseLangTag = useMemo(
    () => allLanguages.find((l) => l.base)?.tag,
    [allLanguages]
  );

  const effectiveLanguages = useMemo(() => {
    if (selectedLanguages.length > 0) return selectedLanguages;
    return baseLangTag ? [baseLangTag] : [];
  }, [selectedLanguages, baseLangTag]);

  const { addFilter, removeFilter, filtersQuery } = useTranslationFilters({
    filters: parsedFilters,
    setFilters,
    selectedLanguages: effectiveLanguages,
    baseLang: baseLangTag,
  });

  const languageCols = useMemo(() => {
    return effectiveLanguages
      .map((tag) => allLanguages.find((l) => l.tag === tag))
      .filter(Boolean) as LanguageModel[];
  }, [effectiveLanguages, allLanguages]);

  // Columns: Key + Trashed + Languages
  const resizableColumns = useMemo(
    () => [null, ...effectiveLanguages],
    [effectiveLanguages]
  );

  const availableWidth = Math.max(0, containerWidth - TRASHED_COLUMN_WIDTH);

  const {
    columnSizes,
    columnSizesPercent: resizablePercent,
    startResize,
    resizeColumn,
    addResizer,
  } = useColumns({
    width: availableWidth,
    initialRatios: resizableColumns.map((_, i) => (i === 0 ? 0.5 : 1)),
    minSize: 300,
  });

  // Build final column sizes: [Key, Trashed(fixed), Lang1, Lang2, ...]
  const finalColumnSizes = useMemo(() => {
    if (!resizablePercent?.length) return [];
    const [keySize, ...langSizes] = resizablePercent;
    return [keySize, `${TRASHED_COLUMN_WIDTH}px`, ...langSizes];
  }, [resizablePercent]);

  const requestQuery = useMemo(
    () => ({
      size: PAGE_SIZE,
      search: search || undefined,
      languages: effectiveLanguages.length > 0 ? effectiveLanguages : undefined,
      branch: branchName || undefined,
      sort: [order],
      ...filtersQuery,
    }),
    [search, effectiveLanguages, branchName, order, filtersQuery]
  );

  const trashLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/keys/trash',
    method: 'get',
    path: { projectId: project.id },
    query: requestQuery,
    options: {
      cacheTime: 0,
      keepPreviousData: true,
      getNextPageParam: (lastPage) => {
        const pageInfo = lastPage.page;
        if (
          pageInfo &&
          pageInfo.number !== undefined &&
          pageInfo.totalPages !== undefined &&
          pageInfo.number < pageInfo.totalPages - 1
        ) {
          return {
            path: { projectId: project.id },
            query: {
              ...requestQuery,
              page: pageInfo.number + 1,
            },
          };
        }
      },
    },
  });

  const trashedKeys = useMemo(
    () =>
      trashLoadable.data?.pages
        .filter(Boolean)
        .flatMap((p) => p._embedded?.keys || []) ?? [],
    [trashLoadable.data]
  ) as TrashedKeyWithTranslationsModel[];

  const totalElements = trashLoadable.data?.pages[0]?.page?.totalElements ?? 0;

  const selectAllMutation = useApiMutation({
    url: '/v2/projects/{projectId}/keys/trash/select-all',
    method: 'get',
  });

  const handleToggleKey = useCallback((keyId: number) => {
    setSelectedKeys((prev) =>
      prev.includes(keyId)
        ? prev.filter((id) => id !== keyId)
        : [...prev, keyId]
    );
  }, []);

  const allSelected =
    totalElements > 0 && selectedKeys.length === totalElements;
  const someSelected = !allSelected && selectedKeys.length > 0;

  const handleSelectAll = useCallback(async () => {
    if (allSelected) {
      setSelectedKeys([]);
    } else {
      const result = await selectAllMutation.mutateAsync({
        path: { projectId: project.id },
        query: {
          search: search || undefined,
          languages:
            effectiveLanguages.length > 0 ? effectiveLanguages : undefined,
          branch: branchName || undefined,
          ...filtersQuery,
        },
      });
      setSelectedKeys(result.ids);
    }
  }, [
    allSelected,
    project.id,
    search,
    effectiveLanguages,
    branchName,
    filtersQuery,
  ]);

  const handleRestore = useCallback(() => {
    setSelectedKeys([]);
    invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/keys/trash');
  }, [queryClient]);

  const handleDelete = useCallback(() => {
    setSelectedKeys([]);
    invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/keys/trash');
  }, [queryClient]);

  const handleBatchFinished = () => {
    setSelectedKeys([]);
    invalidateUrlPrefix(queryClient, '/v2/projects/{projectId}/keys/trash');
  };

  const translationsLink = LINKS.PROJECT_TRANSLATIONS.build({
    [PARAMS.PROJECT_ID]: project.id,
  });
  const trashLink = LINKS.PROJECT_TRANSLATIONS_TRASH.build({
    [PARAMS.PROJECT_ID]: project.id,
  });

  return (
    <BaseProjectView
      windowTitle={t('trash_view_title')}
      navigation={[
        [t('translations_view_title'), translationsLink],
        [t('trash_view_title'), trashLink],
      ]}
      wrapperProps={{ style: { paddingBottom: 0, paddingTop: '3px' } }}
      branching
    >
      <Box my={1} px={0.5}>
        <TrashBanner />
      </Box>

      <TrashControls
        search={search || ''}
        onSearchChange={(val) => {
          setSearch(val);
        }}
        filters={parsedFilters as FiltersType}
        filterActions={{ addFilter, removeFilter, setFilters }}
        languages={allLanguages as LanguageModel[]}
        selectedLanguages={effectiveLanguages}
        onLanguagesChange={setSelectedLanguages}
        order={order}
        onOrderChange={(value) => {
          setOrder(value);
        }}
        defaultOrder={TRASH_DEFAULT_ORDER}
        sortOptions={trashSortOptions}
        totalElements={totalElements}
        allSelected={allSelected}
        someSelected={someSelected}
        onSelectAll={handleSelectAll}
        projectId={project.id}
      />

      <div ref={containerRef} />

      {trashedKeys.length > 0 ? (
        <TrashTable
          trashedKeys={trashedKeys}
          selectedKeys={selectedKeys}
          onToggleKey={handleToggleKey}
          onRestore={handleRestore}
          onDelete={handleDelete}
          canRestore={canRestore}
          canDelete={canDelete}
          languages={languageCols}
          finalColumnSizes={finalColumnSizes}
          columnSizes={columnSizes}
          startResize={startResize}
          resizeColumn={resizeColumn}
          addResizer={addResizer}
          onFilterNamespace={(ns) => addFilter('filterNamespace', ns)}
          totalCount={totalElements}
          isFetchingNextPage={trashLoadable.isFetchingNextPage}
          hasNextPage={!!trashLoadable.hasNextPage}
          fetchNextPage={trashLoadable.fetchNextPage}
          containerWidth={containerWidth}
        />
      ) : (
        <EmptyListMessage loading={trashLoadable.isLoading}>
          <T keyName="trash_empty" />
        </EmptyListMessage>
      )}

      <TrashBatchBar
        selectedKeys={selectedKeys}
        totalCount={totalElements}
        allSelected={allSelected}
        someSelected={someSelected}
        onToggleSelectAll={handleSelectAll}
        onFinished={handleBatchFinished}
        canRestore={canRestore}
        canDelete={canDelete}
      />
    </BaseProjectView>
  );
};

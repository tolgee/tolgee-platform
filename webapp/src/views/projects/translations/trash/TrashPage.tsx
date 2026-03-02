import { useState, useMemo, useCallback, useRef, useEffect } from 'react';
import { useTranslate, T } from '@tolgee/react';
import {
  Badge,
  Box,
  Checkbox,
  IconButton,
  styled,
  Tooltip,
  Typography,
  Pagination,
} from '@mui/material';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useBranchFromUrlPath } from 'tg.component/branching/useBranchFromUrlPath';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { HeaderSearchField } from 'tg.component/layout/HeaderSearchField';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { Sort } from 'tg.component/CustomIcons';
import { TranslationSortMenu } from 'tg.component/translation/translationSort/TranslationSortMenu';
import { BaseProjectView } from '../../BaseProjectView';
import { TrashBanner } from './TrashBanner';
import { TrashBatchBar } from './TrashBatchBar';
import { TrashRow } from './TrashRow';
import { CellLanguage } from '../TranslationsTable/CellLanguage';
import { useColumns } from '../useColumns';
import { ColumnResizer } from '../ColumnResizer';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationFilters } from '../TranslationFilters/TranslationFilters';
import { useTranslationFilters } from '../TranslationFilters/useTranslationFilters';
import { FiltersInternal } from '../TranslationFilters/tools';

type LanguageModel = components['schemas']['LanguageModel'];

const PAGE_SIZE = 20;
const TRASHED_COLUMN_WIDTH = 200;
const HEADER_HEIGHT = 39;
const NAMESPACE_BANNER_SPACING = 14;

const StyledControls = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: start;
  margin-bottom: 8px;
`;

const StyledSpaced = styled('div')`
  display: flex;
  gap: 10px;
  padding: 0px 5px;
`;

const StyledSearchField = styled(HeaderSearchField)`
  width: 200px;
`;

const StyledResultCount = styled('div')`
  display: flex;
  align-items: center;
  padding: 0px 0px 4px 0px;
  margin-left: 3px;
`;

const StyledCount = styled(Typography)`
  margin-left: 8px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledPagination = styled('div')`
  display: flex;
  justify-content: center;
  padding: 16px 0;
`;

const StyledTableContainer = styled('div')`
  position: relative;
  display: grid;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;
  padding-bottom: 100px;
`;

const StyledVerticalScroll = styled('div')`
  overflow-x: auto;
  scrollbar-width: none;
  overflow-y: hidden;
`;

const StyledContent = styled('div')`
  position: relative;
`;

const StyledHeaderRow = styled('div')`
  position: sticky;
  background: ${({ theme }) => theme.palette.background.default};
  top: 0px;
  margin-bottom: -1px;
  display: grid;
  z-index: 1;
`;

const StyledHeaderCell = styled('div')`
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider};
  box-sizing: border-box;
  display: flex;
  flex-grow: 0;
  overflow: hidden;
  align-items: center;
  &.keyCell {
    padding-left: 13px;
  }
  &.trashedCell {
    padding: 8px 12px;
    border-left: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

export const TrashPage = () => {
  const { t } = useTranslate();
  const project = useProject();
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
  const parsedFilters: FiltersInternal = useMemo(
    () => JSON.parse(filtersJson || '{}'),
    [filtersJson]
  );
  const setFilters = useCallback(
    (f: FiltersInternal) => {
      setFiltersJson(JSON.stringify(f));
      setPage(0);
    },
    [setFiltersJson]
  );
  const [page, setPage] = useState(0);
  const [selectedLanguages, setSelectedLanguages] = useState<string[]>([]);
  const [selectedKeys, setSelectedKeys] = useState<number[]>([]);
  const [order, setOrder] = useUrlSearchState('order', {
    defaultVal: 'deletedAt',
  });
  const [anchorSortEl, setAnchorSortEl] = useState<HTMLElement | null>(null);

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
  // We use useColumns for Key + Language columns, then insert Trashed as fixed-width
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

  const trashLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/trash',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page,
      size: PAGE_SIZE,
      search: search || undefined,
      languages: effectiveLanguages.length > 0 ? effectiveLanguages : undefined,
      branch: branchName || undefined,
      sort: [order],
      ...filtersQuery,
    },
  });

  const trashedKeys =
    (trashLoadable.data?._embedded as Record<string, any>)?.keys ?? [];
  const totalElements = trashLoadable.data?.page?.totalElements ?? 0;
  const totalPages = trashLoadable.data?.page?.totalPages ?? 0;

  const handlePageChange = useCallback((_: any, newPage: number) => {
    setPage(newPage - 1);
    setSelectedKeys([]);
  }, []);

  const handleToggleKey = useCallback((keyId: number) => {
    setSelectedKeys((prev) =>
      prev.includes(keyId)
        ? prev.filter((id) => id !== keyId)
        : [...prev, keyId]
    );
  }, []);

  const pageKeyIds = useMemo(
    () => trashedKeys.map((k: any) => k.id as number),
    [trashedKeys]
  );

  const allPageSelected =
    pageKeyIds.length > 0 &&
    pageKeyIds.every((id: number) => selectedKeys.includes(id));
  const somePageSelected =
    pageKeyIds.some((id: number) => selectedKeys.includes(id)) &&
    !allPageSelected;

  const handleSelectAll = useCallback(() => {
    if (allPageSelected) {
      setSelectedKeys([]);
    } else {
      setSelectedKeys(pageKeyIds);
    }
  }, [allPageSelected, pageKeyIds]);

  const handleRestore = useCallback(() => {
    setSelectedKeys([]);
    trashLoadable.refetch();
  }, [trashLoadable.refetch]);

  const handleDelete = useCallback(() => {
    setSelectedKeys([]);
    trashLoadable.refetch();
  }, [trashLoadable.refetch]);

  const handleBatchFinished = useCallback(() => {
    setSelectedKeys([]);
    trashLoadable.refetch();
  }, [trashLoadable.refetch]);

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

      <StyledControls>
        <StyledSpaced>
          <StyledSearchField
            value={search || ''}
            onSearchChange={(val) => {
              setSearch(val);
              setPage(0);
            }}
            label={null}
            variant="outlined"
            placeholder={t('standard_search_label')}
          />
          <TranslationFilters
            value={parsedFilters as any}
            actions={{ addFilter, removeFilter, setFilters }}
            selectedLanguages={languageCols}
            projectId={project.id}
            filterOptions={{ showDeletedBy: true }}
          />
          <Tooltip title={t('translation_controls_sort_tooltip')}>
            <Badge
              color="primary"
              variant="dot"
              badgeContent={order === TRASH_DEFAULT_ORDER ? 0 : 1}
              overlap="circular"
            >
              <IconButton
                onClick={(e) => setAnchorSortEl(e.currentTarget)}
                data-cy="trash-sort-button"
              >
                <Sort />
              </IconButton>
            </Badge>
          </Tooltip>
          <TranslationSortMenu
            value={order}
            onChange={(value) => {
              setOrder(value);
              setPage(0);
            }}
            anchorEl={anchorSortEl}
            onClose={() => setAnchorSortEl(null)}
            options={trashSortOptions}
          />
        </StyledSpaced>

        <StyledSpaced>
          <LanguagesSelect
            onChange={setSelectedLanguages}
            value={effectiveLanguages}
            languages={allLanguages as LanguageModel[]}
            context="trash"
          />
        </StyledSpaced>
      </StyledControls>

      {totalElements > 0 && (
        <StyledResultCount>
          <Tooltip
            title={
              allPageSelected
                ? t('translations_clear_selection')
                : t('translations_select_all')
            }
            disableInteractive
          >
            <Checkbox
              data-cy="trash-select-all-header"
              size="small"
              checked={allPageSelected}
              indeterminate={somePageSelected}
              onChange={handleSelectAll}
            />
          </Tooltip>
          <StyledCount variant="body2">
            <T keyName="trash_key_count" params={{ count: totalElements }} />
          </StyledCount>
        </StyledResultCount>
      )}

      <div ref={containerRef} />

      {trashedKeys.length > 0 ? (
        <StyledTableContainer data-cy="trash-table">
          <StyledVerticalScroll>
            <StyledContent>
              <StyledHeaderRow
                style={{
                  gridTemplateColumns: finalColumnSizes.join(' '),
                  width: `calc(${finalColumnSizes.join(' + ')})`,
                  height:
                    HEADER_HEIGHT +
                    (trashedKeys[0]?.namespace ? NAMESPACE_BANNER_SPACING : 0),
                }}
              >
                <StyledHeaderCell className="keyCell">
                  <T keyName="trash_header_key" />
                </StyledHeaderCell>
                <StyledHeaderCell className="trashedCell">
                  <T keyName="trash_header_trashed" />
                </StyledHeaderCell>
                {languageCols.map((language, i) => (
                  <StyledHeaderCell key={language.tag}>
                    <CellLanguage
                      language={language}
                      onResize={() => startResize(i)}
                    />
                  </StyledHeaderCell>
                ))}
              </StyledHeaderRow>

              {columnSizes.slice(0, -1).map((w, i) => {
                const left =
                  columnSizes.slice(0, i + 1).reduce((a, b) => a + b, 0) +
                  (i >= 0 ? TRASHED_COLUMN_WIDTH : 0);
                return (
                  <ColumnResizer
                    key={i}
                    size={w}
                    left={left}
                    onResize={(size) => resizeColumn(i, size)}
                    passResizeCallback={(callback) => addResizer(i, callback)}
                  />
                );
              })}

              {trashedKeys.map((key: any, index: number) => {
                const prevKey = index > 0 ? trashedKeys[index - 1] : null;
                const showNamespace =
                  key.namespace &&
                  (index === 0 || prevKey?.namespace !== key.namespace);
                return (
                  <TrashRow
                    key={key.id}
                    data={key}
                    selected={selectedKeys.includes(key.id)}
                    onToggle={() => handleToggleKey(key.id)}
                    onRestore={handleRestore}
                    onDelete={handleDelete}
                    canRestore={canRestore}
                    canDelete={canDelete}
                    languages={languageCols}
                    columnSizes={finalColumnSizes}
                    showNamespace={!!showNamespace}
                    onFilterNamespace={(ns) => addFilter('filterNamespace', ns)}
                  />
                );
              })}
            </StyledContent>
          </StyledVerticalScroll>

          {totalPages > 1 && (
            <StyledPagination>
              <Pagination
                count={totalPages}
                page={page + 1}
                onChange={handlePageChange}
                color="primary"
              />
            </StyledPagination>
          )}
        </StyledTableContainer>
      ) : (
        <EmptyListMessage loading={trashLoadable.isLoading}>
          <T keyName="trash_empty" />
        </EmptyListMessage>
      )}

      <TrashBatchBar
        selectedKeys={selectedKeys}
        totalCount={totalElements}
        allPageSelected={allPageSelected}
        somePageSelected={somePageSelected}
        onToggleSelectAll={handleSelectAll}
        onFinished={handleBatchFinished}
        canRestore={canRestore}
        canDelete={canDelete}
      />
    </BaseProjectView>
  );
};

import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { Box, styled, Typography, useTheme } from '@mui/material';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { T, useTranslate } from '@tolgee/react';
import { useResizeObserver } from 'usehooks-ts';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useDebounce } from 'use-debounce';
import { ReactList } from 'tg.component/reactList/ReactList';
import {
  EntryGroup,
  EntryGroupCandidate,
  EntryRowLayout,
  TranslationMemoryEntryRow,
} from 'tg.ee.module/translationMemory/components/content/TranslationMemoryEntryRow';
import { TranslationMemoryCreateEntryDialog } from 'tg.ee.module/translationMemory/views/TranslationMemoryCreateEntryDialog';
import { TranslationMemoryImportDialog } from 'tg.ee.module/translationMemory/components/content/TranslationMemoryImportDialog';
import { CopyFromProjectDialog } from 'tg.ee.module/translationMemory/components/content/CopyFromProjectDialog';
import { useTmExport } from 'tg.ee.module/translationMemory/hooks/useTmExport';
import { components } from 'tg.service/apiSchema.generated';
import { apiV2HttpService } from 'tg.service/http/ApiV2HttpService';
import { useSelectionService } from 'tg.service/useSelectionService';
import { tmPreferencesService } from 'tg.ee.module/translationMemory/services/TmPreferencesService';
import { useIsOrganizationOwnerOrMaintainer } from 'tg.globalContext/helpers';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { ScrollArrows } from 'tg.component/entriesList/ScrollArrows';
import {
  Container as ListContainer,
  Content as ListContent,
  VerticalScroll as ListVerticalScroll,
} from 'tg.component/entriesList/entriesListChrome';
import { TmEntriesToolbar } from './TmEntriesToolbar';
import { TmEntriesListHeader } from './TmEntriesListHeader';
import { TmViewToolbar } from './TmViewToolbar';
import { EmptyTmWizard } from './emptyWizard/EmptyTmWizard';

type TranslationMemoryEntryGroupModel =
  components['schemas']['TranslationMemoryEntryGroupModel'];

type OrganizationLanguageModel =
  components['schemas']['OrganizationLanguageModel'];

const PAGE_SIZE = 50;
const LANGUAGE_SEARCH_DEBOUNCE_MS = 500;

const StyledEmpty = styled('div')`
  padding: ${({ theme }) => theme.spacing(8, 2)};
  text-align: center;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  tmName: string;
  defaultPenalty: number;
  assignedProjectsCount: number;
  search?: string;
  onSearch?: (search: string) => void;
};

/**
 * Approximate row height for the virtualized ReactList. Real heights are measured on render
 * and override the estimate, so being slightly off only shows up as a tiny one-frame jump
 * before scroll position settles. Constants match the row paddings/line-heights in
 * TranslationMemoryEntryRow.styles.ts.
 */
const estimateTmEntryRowHeight = (
  group: EntryGroup | undefined,
  displayLanguageCount: number,
  layout: EntryRowLayout
): number => {
  if (!group) return layout === 'flat' ? 64 : 120;
  // Source area: 12px padding top + 18px line + 12px padding bottom = 42px base, +20 for keys
  const keyLine = group.keyNames.length > 0 ? 20 : 0;
  const sourceLineCount = Math.min(
    Math.max(1, Math.ceil(group.sourceText.length / 60)),
    3
  );
  const sourceArea = 24 + 18 * sourceLineCount + keyLine;
  if (layout === 'flat') {
    // Translation cells share the row height; max line count across them is unknown without
    // inspecting each entry, so assume 1 line which is true for most TM entries. Caller will
    // re-measure.
    return Math.max(48, sourceArea);
  }
  // Stacked mode renders one translation cell per displayed language under the source row.
  return sourceArea + Math.max(displayLanguageCount, 1) * 56;
};

export const TranslationMemoryEntriesList: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  tmName,
  defaultPenalty,
  assignedProjectsCount,
  search,
  onSearch,
}) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const canManage = useIsOrganizationOwnerOrMaintainer();
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [copyFromProjectOpen, setCopyFromProjectOpen] = useState(false);
  const [layoutParam, setLayoutParam] = useUrlSearchState('layout', {
    defaultVal: 'flat',
  });
  const layout: EntryRowLayout = layoutParam === 'stacked' ? 'stacked' : 'flat';
  const { triggerExport, exportLoading } = useTmExport(
    organizationId,
    translationMemoryId,
    tmName
  );
  // Track which cell is being edited: "groupKey|||langTag". Lifted up from the row so
  // virtualization-driven row unmount keeps the editing intent and the row re-enters edit
  // mode when it scrolls back into view.
  const [editingCell, setEditingCell] = useState<string | null>(null);
  // undefined = no preference saved → show all languages (all checkboxes appear checked)
  // [] = user explicitly cleared all → show all languages (no filter sent)
  // ['cs','de'] = user selected specific languages
  const [selectedLanguages, setSelectedLanguages] = useState<
    string[] | undefined
  >(() => tmPreferencesService.getForTm(translationMemoryId));

  // Backend paginates by distinct source text and returns a page of groups. The
  // `targetLanguageTag` filter only narrows which entries come back per group —
  // source rows still appear for every distinct source text on the page even
  // when they have no translation in the currently selected languages.
  const targetLanguageTag =
    selectedLanguages && selectedLanguages.length > 0
      ? selectedLanguages.join(',')
      : undefined;

  const entriesPath = { organizationId, translationMemoryId };
  const entriesQuery = {
    size: PAGE_SIZE,
    search: search || undefined,
    targetLanguageTag,
  };
  const entries = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries',
    method: 'get',
    path: entriesPath,
    query: entriesQuery,
    options: {
      keepPreviousData: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: entriesPath,
            query: { ...entriesQuery, page: lastPage.page!.number! + 1 },
          };
        }
        return null;
      },
    },
  });

  // Backend returns groups directly: one per (source_text, origin) tuple on the page. Flatten
  // every loaded page and map to the frontend EntryGroup shape.
  const groups = useMemo<EntryGroup[]>(
    () =>
      (entries.data?.pages ?? []).flatMap((p) =>
        (p._embedded?.translationMemoryEntryGroups ?? []).map((g) => ({
          sourceText: g.sourceText,
          keyNames: g.keyNames ?? [],
          entries: g.entries ?? [],
          virtualEntries: g.virtualEntries ?? [],
        }))
      ),
    [entries.data]
  );

  // Expand each group into N candidate rows where N = max entries-per-language. Multiple TMX
  // tu units sharing (source, target_lang) but with different target_text — or imported with
  // different tuids — each get their own row instead of being collapsed into one cell.
  // Round-robin: candidate i picks the i-th entry per language; languages with fewer entries
  // produce empty cells on later candidates.
  const candidates = useMemo<EntryGroupCandidate[]>(
    () =>
      groups.flatMap((group) => {
        const byLang = new Map<string, typeof group.entries>();
        group.entries.forEach((e) => {
          const list = byLang.get(e.targetLanguageTag);
          if (list) {
            list.push(e);
          } else {
            byLang.set(e.targetLanguageTag, [e]);
          }
        });
        const candidateCount = Math.max(
          1,
          ...Array.from(byLang.values()).map((arr) => arr.length)
        );
        return Array.from({ length: candidateCount }, (_, i) => {
          const entriesByLang = new Map<
            string,
            (typeof group.entries)[number]
          >();
          byLang.forEach((list, lang) => {
            if (i < list.length) entriesByLang.set(lang, list[i]);
          });
          return {
            group,
            candidateIndex: i,
            isPrimary: i === 0,
            entriesByLang,
          };
        });
      }),
    [groups]
  );

  const totalElements = entries.data?.pages?.[0]?.page?.totalElements ?? 0;

  // Each selectable row is identified by the representative entry ID (the first entry in the
  // group). Virtual rows have no entries array and therefore no ID — they are not selectable
  // for batch delete. The backend endpoint dedupes by (sourceText, is_manual), so multiple
  // selected entries from the same row only trigger one delete.
  const getAllGroupIds = async (): Promise<number[]> => {
    if (totalElements === 0) return [];
    const data = await apiV2HttpService.get<{
      _embedded?: {
        translationMemoryEntryGroups?: TranslationMemoryEntryGroupModel[];
      };
    }>(
      `organizations/${organizationId}/translation-memories/${translationMemoryId}/entries`,
      {
        size: String(totalElements),
        ...(search ? { search } : {}),
        ...(targetLanguageTag ? { targetLanguageTag } : {}),
      }
    );
    return (
      data._embedded?.translationMemoryEntryGroups
        ?.map((g) => g.entries?.[0]?.id)
        .filter((id): id is number => id !== undefined) ?? []
    );
  };

  const selectionService = useSelectionService<number>({
    totalCount: totalElements,
    itemsAll: getAllGroupIds,
  });

  const containerRef = useRef<HTMLDivElement>(null);
  const verticalScrollRef = useRef<HTMLDivElement | null>(null);
  const reactListRef = useRef<ReactList>(null);
  const [tableHeight, setTableHeight] = useState(600);

  // Stretch the scroll area to fill the viewport below the toolbar — same approach Glossary
  // uses. Resize-observer keeps it correct on window/responsive changes.
  const onResize = useCallback(() => {
    const position = verticalScrollRef.current?.getBoundingClientRect();
    if (position) {
      const bottomSpacing = parseInt(theme.spacing(2), 10);
      setTableHeight(window.innerHeight - position.top - bottomSpacing);
    }
  }, [theme]);

  const verticalScrollRefCallback = useCallback(
    (node: HTMLDivElement | null) => {
      verticalScrollRef.current = node;
      onResize();
    },
    [onResize]
  );

  useEffect(() => {
    onResize();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [onResize]);

  useResizeObserver({ ref: verticalScrollRef, onResize });

  // Language selector
  const [langSearch, setLangSearch] = useState('');
  const [langSearchDebounced] = useDebounce(
    langSearch,
    LANGUAGE_SEARCH_DEBOUNCE_MS
  );

  const langQuery = { search: langSearchDebounced, size: 30 };
  const languagesLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{organizationId}/languages',
    method: 'get',
    path: { organizationId },
    query: langQuery,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { id: organizationId },
            query: { ...langQuery, page: lastPage.page!.number! + 1 },
          };
        }
        return null;
      },
    },
  });

  const languages = languagesLoadable.data?.pages.flatMap(
    (p) => p._embedded?.languages ?? []
  );

  const handleFetchMoreLanguages = () => {
    if (languagesLoadable.hasNextPage && !languagesLoadable.isFetching) {
      languagesLoadable.fetchNextPage();
    }
  };

  const updateSelectedLanguages = (langs: string[]) => {
    setSelectedLanguages(langs);
    tmPreferencesService.setForTm(translationMemoryId, langs);
  };

  const isAllSelected = selectedLanguages === undefined;

  const toggleLanguage = (item: OrganizationLanguageModel) => {
    if (item.tag === sourceLanguageTag) return;
    if (isAllSelected) {
      // Switching from "all" to explicit — deselect this one language
      const allTags = (languages ?? [])
        .map((l) => l.tag)
        .filter((t) => t !== sourceLanguageTag && t !== item.tag);
      updateSelectedLanguages(allTags);
    } else if (selectedLanguages.includes(item.tag)) {
      updateSelectedLanguages(selectedLanguages.filter((t) => t !== item.tag));
    } else {
      updateSelectedLanguages([...selectedLanguages, item.tag]);
    }
  };

  // Languages to render as rows in each entry group
  const displayLanguages = useMemo(() => {
    const allNonBase = (languages ?? [])
      .map((l) => l.tag)
      .filter((t) => t !== sourceLanguageTag);
    if (isAllSelected) return allNonBase;
    // Preserve the order from org languages, filtered to selected
    return allNonBase.filter((t) => selectedLanguages.includes(t));
  }, [languages, isAllSelected, selectedLanguages, sourceLanguageTag]);

  const sourceLangName =
    languageInfo[sourceLanguageTag]?.englishName || sourceLanguageTag;

  const onFetchNextPageHint = () => {
    if (!entries.isFetching && entries.hasNextPage) {
      entries.fetchNextPage();
    }
  };

  const renderItem = (index: number) => {
    const candidate = candidates[index];
    const group = candidate.group;
    const isLast = index === candidates.length - 1;
    if (isLast) onFetchNextPageHint();
    const rowKey = `${group.sourceText}|||${candidate.candidateIndex}`;
    const editingLangForRow = editingCell?.startsWith(rowKey + '|||')
      ? editingCell.split('|||').pop()!
      : null;
    // Selection operates on the whole source-text bucket — only show the checkbox on the primary candidate.
    const groupId = candidate.isPrimary
      ? (group.entries?.[0]?.id as number | undefined)
      : undefined;

    return (
      <TranslationMemoryEntryRow
        key={rowKey}
        candidate={candidate}
        sourceLanguageTag={sourceLanguageTag}
        displayLanguages={displayLanguages}
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        editingLang={editingLangForRow}
        canManage={canManage}
        layout={layout}
        selectionService={candidate.isPrimary ? selectionService : undefined}
        groupId={groupId}
        onEditStart={(langTag) => setEditingCell(`${rowKey}|||${langTag}`)}
        onEditEnd={() => {
          setEditingCell(null);
          entries.refetch();
        }}
      />
    );
  };

  const isEmpty = !entries.isLoading && groups.length === 0;
  // The wizard is the empty-state UI for users who can actually populate the TM. Read-only
  // viewers and "your search returned nothing" cases keep the simple message.
  const showEmptyWizard = isEmpty && canManage && !search && !targetLanguageTag;

  return (
    <Box>
      <Typography
        variant="body2"
        color="text.secondary"
        sx={{ mt: -1, mb: 2 }}
        data-cy="tm-content-subtitle"
      >
        {defaultPenalty > 0 ? (
          <T
            keyName="translation_memory_content_subtitle_with_penalty"
            defaultValue="Base language: {language} · {projectCount, plural, one {# project} other {# projects}} · Default penalty: {penalty}%"
            params={{
              language: sourceLangName,
              projectCount: assignedProjectsCount,
              penalty: defaultPenalty,
            }}
          />
        ) : (
          <T
            keyName="translation_memory_content_subtitle"
            defaultValue="Base language: {language}"
            params={{
              language: sourceLangName,
            }}
          />
        )}
      </Typography>

      <TmEntriesToolbar
        search={search}
        onSearch={onSearch}
        totalCount={totalElements}
        layout={layout}
        onLayoutChange={setLayoutParam}
        languages={languages}
        languagesLoadable={languagesLoadable}
        isAllSelected={isAllSelected}
        selectedLanguages={selectedLanguages}
        langSearch={langSearch}
        onLangSearchChange={setLangSearch}
        onFetchMoreLanguages={handleFetchMoreLanguages}
        onToggleLanguage={toggleLanguage}
        onClearLanguages={() => updateSelectedLanguages([])}
        sourceLanguageTag={sourceLanguageTag}
        canManage={canManage}
        onImport={() => setImportDialogOpen(true)}
        onCopyFromProject={() => setCopyFromProjectOpen(true)}
        onExport={triggerExport}
        exportDisabled={exportLoading || totalElements === 0}
        onCreate={() => setCreateDialogOpen(true)}
        hideFilters={showEmptyWizard}
      />

      {createDialogOpen && (
        <TranslationMemoryCreateEntryDialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          onFinished={() => {
            setCreateDialogOpen(false);
            entries.refetch();
          }}
          organizationId={organizationId}
          translationMemoryId={translationMemoryId}
          sourceLanguageTag={sourceLanguageTag}
          availableLanguages={displayLanguages}
        />
      )}

      {importDialogOpen && (
        <TranslationMemoryImportDialog
          open={importDialogOpen}
          onClose={() => setImportDialogOpen(false)}
          onFinished={() => {
            setImportDialogOpen(false);
            entries.refetch();
          }}
          organizationId={organizationId}
          translationMemoryId={translationMemoryId}
          hasExistingEntries={totalElements > 0}
        />
      )}

      {copyFromProjectOpen && (
        <CopyFromProjectDialog
          open={copyFromProjectOpen}
          onClose={() => setCopyFromProjectOpen(false)}
          onFinished={() => {
            setCopyFromProjectOpen(false);
            entries.refetch();
          }}
          organizationId={organizationId}
          translationMemoryId={translationMemoryId}
          sourceLanguageTag={sourceLanguageTag}
        />
      )}

      <ListContainer ref={containerRef} data-cy="tm-entries-table">
        <ScrollArrows
          containerRef={containerRef}
          verticalScrollRef={verticalScrollRef}
          deps={[displayLanguages, layout]}
        />
        <ListVerticalScroll
          ref={verticalScrollRefCallback}
          style={{ height: tableHeight }}
        >
          <ListContent>
            {layout === 'flat' && displayLanguages.length > 0 && !isEmpty && (
              <TmEntriesListHeader
                sourceLanguageTag={sourceLanguageTag}
                displayLanguages={displayLanguages}
                selectionService={selectionService}
                canManage={canManage}
              />
            )}

            {showEmptyWizard ? (
              <EmptyTmWizard
                organizationId={organizationId}
                translationMemoryId={translationMemoryId}
                sourceLanguageTag={sourceLanguageTag}
                availableLanguages={displayLanguages}
                onFinished={() => entries.refetch()}
              />
            ) : isEmpty ? (
              <StyledEmpty>
                {t(
                  'translation_memory_entries_empty',
                  'No entries in this translation memory yet.'
                )}
              </StyledEmpty>
            ) : (
              <ReactList
                ref={reactListRef}
                threshold={800}
                type="variable"
                itemSizeEstimator={(index, cache) =>
                  cache[index] ||
                  estimateTmEntryRowHeight(
                    candidates[index]?.group,
                    displayLanguages.length,
                    layout
                  )
                }
                // Only reserve room for editor expansion while a cell is actually being
                // edited — otherwise we'd render ~300px of phantom scroll space below the
                // last row.
                expansionReserve={editingCell ? 300 : 0}
                // @ts-ignore — third-party type expects JSX element; window/HTMLElement are accepted at runtime
                scrollParentGetter={() => verticalScrollRef.current ?? window}
                length={candidates.length}
                useTranslate3d
                itemRenderer={renderItem}
              />
            )}
          </ListContent>
        </ListVerticalScroll>
      </ListContainer>

      <TmViewToolbar
        leftOffset={verticalScrollRef.current?.getBoundingClientRect?.()?.left}
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        selectionService={selectionService}
      />
    </Box>
  );
};

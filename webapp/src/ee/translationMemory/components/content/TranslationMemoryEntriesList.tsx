import React, { useLayoutEffect, useMemo, useRef, useState } from 'react';
import { Box, Button, styled, Typography } from '@mui/material';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { T, useTranslate } from '@tolgee/react';
import { ReactList } from 'tg.component/reactList/ReactList';
import {
  EntryRowLayout,
  TranslationMemoryEntryRow,
} from 'tg.ee.module/translationMemory/components/content/TranslationMemoryEntryRow';
import { TranslationMemoryCreateEntryDialog } from 'tg.ee.module/translationMemory/views/TranslationMemoryCreateEntryDialog';
import { TranslationMemoryImportDialog } from 'tg.ee.module/translationMemory/components/content/TranslationMemoryImportDialog';
import { useTmExport } from 'tg.ee.module/translationMemory/hooks/useTmExport';
import { useTmEntriesData } from 'tg.ee.module/translationMemory/hooks/useTmEntriesData';
import { useTmLanguageFilter } from 'tg.ee.module/translationMemory/hooks/useTmLanguageFilter';
import { useTmTableHeight } from 'tg.ee.module/translationMemory/hooks/useTmTableHeight';
import { useSelectionService } from 'tg.service/useSelectionService';
import { useIsOrganizationOwnerOrMaintainer } from 'tg.globalContext/helpers';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { ScrollArrows } from 'tg.component/entriesList/ScrollArrows';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import {
  Container as ListContainer,
  Content as ListContent,
  VerticalScroll as ListVerticalScroll,
} from 'tg.component/entriesList/entriesListChrome';
import { TmEntriesToolbar } from './TmEntriesToolbar';
import { TmEntriesListHeader } from './TmEntriesListHeader';
import { TmViewToolbar } from './TmViewToolbar';
import { EmptyTmWizard } from './emptyWizard/EmptyTmWizard';
import { estimateTmEntryRowHeight } from './estimateTmEntryRowHeight';

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
  /** PROJECT-type TMs are bound to a single project and have no assignment editor — the
   *  empty-state wizard's "Sync from projects" card is hidden for them. */
  isProjectTm: boolean;
  search?: string;
  onSearch?: (search: string) => void;
};

export const TranslationMemoryEntriesList: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  tmName,
  defaultPenalty,
  assignedProjectsCount,
  isProjectTm,
  search,
  onSearch,
}) => {
  const { t } = useTranslate();
  const canManage = useIsOrganizationOwnerOrMaintainer();
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [importDialogOpen, setImportDialogOpen] = useState(false);
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

  const {
    languages,
    languagesLoadable,
    selectedLanguages,
    langSearch,
    setLangSearch,
    fetchMoreLanguages,
    toggleLanguage,
    updateSelectedLanguages,
    seedAutoDefault,
    allNonBaseLanguageTags,
    displayLanguages,
    targetLanguageTag,
    createDialogInitialTags,
  } = useTmLanguageFilter({
    organizationId,
    translationMemoryId,
    sourceLanguageTag,
  });

  const { entries, rows, totalElements, getAllGroupIds } = useTmEntriesData({
    organizationId,
    translationMemoryId,
    search,
    targetLanguageTag,
  });

  const firstPageEntryLanguages = useMemo(() => {
    const firstPage =
      entries.data?.pages?.[0]?._embedded?.translationMemoryRows;
    if (!firstPage) return undefined;
    const tags = new Set<string>();
    firstPage.forEach((r) =>
      r.cells.forEach((c) => tags.add(c.targetLanguageTag))
    );
    return [...tags];
  }, [entries.data]);

  useLayoutEffect(() => {
    if (
      selectedLanguages === undefined &&
      firstPageEntryLanguages !== undefined
    ) {
      seedAutoDefault(firstPageEntryLanguages);
    }
  }, [selectedLanguages, firstPageEntryLanguages, seedAutoDefault]);

  const selectionService = useSelectionService<number>({
    totalCount: totalElements,
    itemsAll: getAllGroupIds,
  });

  const containerRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);
  const {
    tableHeight,
    verticalScrollRef,
    refCallback: verticalScrollRefCallback,
  } = useTmTableHeight();

  const sourceLangName =
    languageInfo[sourceLanguageTag]?.englishName || sourceLanguageTag;

  const onFetchNextPageHint = () => {
    if (!entries.isFetching && entries.hasNextPage) {
      entries.fetchNextPage();
    }
  };

  const renderItem = (index: number) => {
    const row = rows[index];
    const isLast = index === rows.length - 1;
    if (isLast) onFetchNextPageHint();
    // Row identity: editable rows use the first cell's entry id; rows mirrored from a project
    // key use (projectId, keyName). Both yield a stable string for React keys / editing state.
    const firstEntryId = row.cells.find(
      (c) => c.entryId !== undefined
    )?.entryId;
    const rowKey = row.editable
      ? `s:${firstEntryId ?? row.sourceText}`
      : `v:${row.projectId}:${row.keyName}:${row.sourceText}`;
    const editingLangForRow = editingCell?.startsWith(rowKey + '|||')
      ? editingCell.split('|||').pop()!
      : null;
    // Only editable rows are selectable for batch delete. groupId is the first cell's entry id.
    const groupId = row.editable ? firstEntryId : undefined;

    return (
      <TranslationMemoryEntryRow
        key={rowKey}
        row={row}
        sourceLanguageTag={sourceLanguageTag}
        displayLanguages={displayLanguages}
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        editingLang={editingLangForRow}
        canManage={canManage}
        layout={layout}
        selectionService={selectionService}
        groupId={groupId}
        onEditStart={(langTag) => setEditingCell(`${rowKey}|||${langTag}`)}
        onEditEnd={() => {
          setEditingCell(null);
          entries.refetch();
        }}
      />
    );
  };

  const isEmpty = !entries.isLoading && rows.length === 0;
  // The wizard is the empty-state UI for users who can populate the TM. Once any project
  // is assigned, hide the "Sync from projects" card — the user has been there.
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
        selectedLanguages={displayLanguages}
        langSearch={langSearch}
        onLangSearchChange={setLangSearch}
        onFetchMoreLanguages={fetchMoreLanguages}
        onToggleLanguage={toggleLanguage}
        onClearLanguages={() => updateSelectedLanguages([])}
        sourceLanguageTag={sourceLanguageTag}
        canManage={canManage}
        onImport={() => setImportDialogOpen(true)}
        onExport={triggerExport}
        exportDisabled={exportLoading || totalElements === 0}
        onCreate={() => setCreateDialogOpen(true)}
        hideFilters={showEmptyWizard}
      />

      {createDialogOpen && (
        <TranslationMemoryCreateEntryDialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          onFinished={(createdLanguageTags) => {
            setCreateDialogOpen(false);
            // A new entry can introduce target languages outside the current filter — add
            // them so the just-created translations stay visible. When nothing is selected
            // yet, the refetched first page (now including the new entry) lets the one-time
            // seed pick them up.
            const current = selectedLanguages;
            const missing = current
              ? createdLanguageTags.filter(
                  (tag) => tag !== sourceLanguageTag && !current.includes(tag)
                )
              : [];
            if (current && missing.length > 0) {
              updateSelectedLanguages([...current, ...missing]);
            } else {
              entries.refetch();
            }
          }}
          organizationId={organizationId}
          translationMemoryId={translationMemoryId}
          sourceLanguageTag={sourceLanguageTag}
          allLanguageTags={allNonBaseLanguageTags}
          initialSelectedTags={createDialogInitialTags}
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
                allLanguageTags={allNonBaseLanguageTags}
                initialSelectedTags={createDialogInitialTags}
                assignedProjectsCount={assignedProjectsCount}
                isProjectTm={isProjectTm}
                onFinished={() => entries.refetch()}
              />
            ) : isEmpty ? (
              search ? (
                <EmptyListMessage
                  hint={
                    onSearch && (
                      <Button onClick={() => onSearch('')} color="primary">
                        <T
                          keyName="translation_memory_entries_nothing_found_action"
                          defaultValue="Clear filters"
                        />
                      </Button>
                    )
                  }
                >
                  <T
                    keyName="translation_memory_entries_nothing_found"
                    defaultValue="No entries found"
                  />
                </EmptyListMessage>
              ) : (
                <StyledEmpty>
                  {t(
                    'translation_memory_entries_empty',
                    'No entries in this translation memory yet.'
                  )}
                </StyledEmpty>
              )
            ) : (
              <ReactList
                ref={reactListRef}
                threshold={800}
                type="variable"
                itemSizeEstimator={(index, cache) =>
                  cache[index] ||
                  estimateTmEntryRowHeight(
                    rows[index],
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
                length={rows.length}
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

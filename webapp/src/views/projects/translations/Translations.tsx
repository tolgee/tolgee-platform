import { useEffect, useMemo } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, Dialog, styled, useMediaQuery } from '@mui/material';
import { Link } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from './context/TranslationsContext';
import { TranslationsTable } from './TranslationsTable/TranslationsTable';
import { TranslationsHeader } from './TranslationHeader/TranslationsHeader';
import { TranslationsList } from './TranslationsList/TranslationsList';
import { useTranslationsShortcuts } from './context/shortcuts/useTranslationsShortcuts';
import { BaseProjectView } from '../BaseProjectView';
import { TranslationsToolbar } from './TranslationsToolbar';
import { BatchOperationsChangeIndicator } from './BatchOperations/BatchOperationsChangeIndicator';
import { FloatingToolsPanel } from './ToolsPanel/FloatingToolsPanel';
import { Prefilter } from './prefilters/Prefilter';
import { TaskDetail } from 'tg.ee/task/components/TaskDetail';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
`;

export const Translations = () => {
  const { setQuickStartOpen, quickStartForceFloating } = useGlobalActions();
  const prefilter = useTranslationsSelector((c) => c.prefilter);
  const quickStartEnabled = useGlobalContext((c) => c.quickStartGuide.enabled);
  const isSmall = useMediaQuery(`@media (max-width: ${800}px)`);
  const { t } = useTranslate();
  const project = useProject();
  const projectPermissions = useProjectPermissions();

  const isLoading = useTranslationsSelector((c) => c.isLoading);
  const isFetching = useTranslationsSelector((c) => c.isFetching);
  const view = useTranslationsSelector((v) => v.view);
  const translations = useTranslationsSelector((c) => c.translations);
  const sidePanelOpen = useTranslationsSelector((c) => c.sidePanelOpen);

  const filtersOrSearchApplied = useTranslationsSelector((c) =>
    Boolean(Object.values(c.filters).filter(Boolean).length || c.urlSearch)
  );

  const memoizedFiltersOrSearchApplied = useMemo(
    () => filtersOrSearchApplied,
    [translations]
  );

  const { setSearchImmediate, setFilters } = useTranslationsActions();

  const { onKey } = useTranslationsShortcuts();

  useEffect(() => {
    document.body?.addEventListener('keydown', onKey);
    return () => document.body?.removeEventListener('keydown', onKey);
  }, [onKey]);

  const translationsEmpty = !translations?.length;

  const canAdd = projectPermissions.satisfiesPermission('keys.edit');

  const [_, setNewDialog] = useUrlSearchState('create', {
    defaultVal: 'false',
  });

  const handleAddTranslation = () => {
    setNewDialog('true');
  };

  const handleClearFilters = () => {
    setSearchImmediate('');
    setFilters({});
  };

  // hide quick start panel
  useEffect(() => {
    if (sidePanelOpen && quickStartEnabled) {
      quickStartForceFloating(true);
      setQuickStartOpen(true);
      return () => {
        quickStartForceFloating(false);
      };
    }
  }, [sidePanelOpen, quickStartEnabled]);

  const [taskDetail, setTaskDetail] = useUrlSearchState('taskDetail');

  const renderPlaceholder = () =>
    memoizedFiltersOrSearchApplied ? (
      <EmptyListMessage
        loading={isLoading || isFetching}
        hint={
          <Button onClick={handleClearFilters} color="primary">
            <T keyName="translations_nothing_found_action" />
          </Button>
        }
      >
        <T keyName="translations_nothing_found" />
      </EmptyListMessage>
    ) : (
      <EmptyListMessage
        loading={isLoading || isFetching}
        hint={
          canAdd && (
            <>
              <Button onClick={handleAddTranslation} color="primary">
                <T keyName="translations_no_translations_action" />
              </Button>
              <Box display="inline" p={1}>
                |
              </Box>
              <Button
                component={Link}
                to={LINKS.PROJECT_INTEGRATE.build({
                  [PARAMS.PROJECT_ID]: project.id,
                })}
                color="primary"
              >
                <T keyName="translations_no_translations_integrate" />
              </Button>
            </>
          )
        }
      >
        <T keyName="translations_no_translations" />
      </EmptyListMessage>
    );

  const toolsPanelOpen = sidePanelOpen && !isSmall;

  return (
    <BaseProjectView
      windowTitle={t('translations_view_title')}
      navigation={[
        [
          t('translations_view_title'),
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
      wrapperProps={{ pb: 0 }}
    >
      <BatchOperationsChangeIndicator />
      {prefilter && <Prefilter prefilter={prefilter} />}
      <TranslationsHeader />
      <StyledContainer>
        {translationsEmpty ? (
          renderPlaceholder()
        ) : view === 'TABLE' ? (
          <TranslationsTable key="table" toolsPanelOpen={toolsPanelOpen} />
        ) : (
          <TranslationsList key="list" toolsPanelOpen={toolsPanelOpen} />
        )}
        {toolsPanelOpen && <FloatingToolsPanel />}
      </StyledContainer>
      <TranslationsToolbar />
      {taskDetail !== undefined && (
        <Dialog
          open={true}
          onClose={() => setTaskDetail(undefined)}
          maxWidth="xl"
        >
          <TaskDetail
            taskNumber={Number(taskDetail)}
            onClose={() => setTaskDetail(undefined)}
            projectId={project.id}
          />
        </Dialog>
      )}
    </BaseProjectView>
  );
};

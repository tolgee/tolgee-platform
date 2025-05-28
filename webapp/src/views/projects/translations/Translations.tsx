import { useEffect, useMemo } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Button, styled } from '@mui/material';
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
import { TranslationsTaskDetail, TaskAllDonePlaceholder } from 'tg.ee';
import { EmptyState } from 'tg.component/common/EmptyState';

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
import { countFilters } from './TranslationFilters/summary';
import { AiPlayground } from './ToolsPanel/AiPlayground';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
`;

export const Translations = () => {
  const { setQuickStartOpen, setQuickStartFloatingForced } = useGlobalActions();
  const prefilter = useTranslationsSelector((c) => c.prefilter);
  const quickStartEnabled = useGlobalContext((c) => c.quickStartGuide.enabled);
  const { t } = useTranslate();
  const project = useProject();
  const projectPermissions = useProjectPermissions();

  const isLoading = useTranslationsSelector((c) => c.isLoading);
  const isFetching = useTranslationsSelector((c) => c.isFetching);
  const view = useTranslationsSelector((v) => v.view);
  const translations = useTranslationsSelector((c) => c.translations);
  const sidePanelWidth = useTranslationsSelector(
    (c) => c.layout.sidePanelWidth
  );
  const mainContentWidth = useTranslationsSelector(
    (c) => c.layout.mainContentWidth
  );

  const filtersOrSearchApplied = useTranslationsSelector((c) =>
    Boolean(countFilters(c.filters) !== 0 || c.urlSearch)
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
    if (sidePanelWidth && quickStartEnabled) {
      setQuickStartFloatingForced(true);
      setQuickStartOpen(true);
      return () => {
        setQuickStartFloatingForced(false);
      };
    }
  }, [sidePanelWidth, quickStartEnabled]);

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
    ) : prefilter?.task && prefilter.taskFilterNotDone ? (
      <EmptyState loading={isLoading || isFetching}>
        <TaskAllDonePlaceholder
          taskNumber={prefilter.task}
          projectId={project.id}
        />
      </EmptyState>
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
      wrapperProps={{ style: { paddingBottom: 0, paddingTop: '3px' } }}
      rightPanelContent={(width) => <AiPlayground width={width} />}
    >
      <BatchOperationsChangeIndicator />
      <TranslationsHeader />
      <StyledContainer>
        {translationsEmpty ? (
          renderPlaceholder()
        ) : view === 'TABLE' ? (
          <TranslationsTable key="table" width={mainContentWidth} />
        ) : (
          <TranslationsList key="list" width={mainContentWidth} />
        )}
        {Boolean(sidePanelWidth) && (
          <FloatingToolsPanel width={sidePanelWidth} />
        )}
      </StyledContainer>
      <TranslationsToolbar />
      <TranslationsTaskDetail />
    </BaseProjectView>
  );
};

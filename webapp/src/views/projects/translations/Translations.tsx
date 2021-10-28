import { useEffect, useMemo } from 'react';
import { useContextSelector } from 'use-context-selector';
import { useTranslate, T } from '@tolgee/react';
import { Box, Button } from '@material-ui/core';
import { Link } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseView } from 'tg.component/layout/BaseView';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { TranslationsTable } from './TranslationsTable/TranslationsTable';
import { TranslationsHeader } from './TranslationsHeader';
import { TranslationsList } from './TranslationsList/TranslationsList';
import { useContextShortcuts } from './context/useContextShortcuts';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

export const Translations = () => {
  const t = useTranslate();
  const project = useProject();
  const projectPermissions = useProjectPermissions();

  const isLoading = useContextSelector(TranslationsContext, (c) => c.isLoading);
  const isFetching = useContextSelector(
    TranslationsContext,
    (c) => c.isFetching
  );
  const view = useContextSelector(TranslationsContext, (v) => v.view);
  const translations = useContextSelector(
    TranslationsContext,
    (c) => c.translations
  );

  const filtersOrSearchApplied = useContextSelector(TranslationsContext, (c) =>
    Boolean(Object.values(c.filters).filter(Boolean).length || c.urlSearch)
  );

  const memoizedFiltersOrSearchApplied = useMemo(
    () => filtersOrSearchApplied,
    [translations]
  );

  const dispatch = useTranslationsDispatch();

  const { onKey } = useContextShortcuts();

  useEffect(() => {
    document.body?.addEventListener('keydown', onKey);
    return () => document.body?.removeEventListener('keydown', onKey);
  }, [onKey]);

  const translationsEmpty = !translations?.length;

  const canAdd = projectPermissions.satisfiesPermission(
    ProjectPermissionType.EDIT
  );

  const [_, setNewDialog] = useUrlSearchState('create', {
    defaultVal: 'false',
  });

  const handleAddTranslation = () => {
    setNewDialog('true');
  };

  const handleClearFilters = () => {
    dispatch({ type: 'SET_SEARCH_IMMEDIATE', payload: '' });
    dispatch({ type: 'SET_FILTERS', payload: {} });
  };

  useGlobalLoading(isFetching || isLoading);

  const renderPlaceholder = () =>
    memoizedFiltersOrSearchApplied ? (
      <EmptyListMessage
        loading={isLoading || isFetching}
        hint={
          <Button onClick={handleClearFilters} color="primary">
            <T>translations_nothing_found_action</T>
          </Button>
        }
      >
        <T>translations_nothing_found</T>
      </EmptyListMessage>
    ) : (
      <EmptyListMessage
        loading={isLoading || isFetching}
        hint={
          canAdd && (
            <>
              <Button onClick={handleAddTranslation} color="primary">
                <T>translations_no_translations_action</T>
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
                <T>translations_no_translations_integrate</T>
              </Button>
            </>
          )
        }
      >
        <T>translations_no_translations</T>
      </EmptyListMessage>
    );

  return (
    <BaseView
      navigation={[
        [
          project.name,
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
        [
          t('translations_view_title'),
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <TranslationsHeader />
      {translationsEmpty ? (
        renderPlaceholder()
      ) : view === 'TABLE' ? (
        <TranslationsTable />
      ) : (
        <TranslationsList />
      )}
    </BaseView>
  );
};

import { useEffect } from 'react';
import { useContextSelector } from 'use-context-selector';
import { useTranslate } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseView } from 'tg.component/layout/BaseView';
import { TranslationsContext } from './context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { TranslationsTable } from './TranslationsTable/TranslationsTable';
import { TranslationsHeader } from './TranslationsHeader';
import { TranslationsList } from './TranslationsList/TranslationsList';
import { useContextShortcuts } from './context/useContextShortcuts';

export const Translations = () => {
  const t = useTranslate();
  const project = useProject();

  const isLoading = useContextSelector(TranslationsContext, (v) => v.isLoading);
  const isFetching = useContextSelector(
    TranslationsContext,
    (v) => v.isFetching
  );
  const view = useContextSelector(TranslationsContext, (v) => v.view);

  const { onKey } = useContextShortcuts();

  useEffect(() => {
    document.body?.addEventListener('keydown', onKey);
    return () => document.body?.removeEventListener('keydown', onKey);
  }, [onKey]);

  return (
    <BaseView
      loading={isLoading || isFetching}
      hideChildrenOnLoading={isLoading && !isFetching}
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
      {view === 'TABLE' ? <TranslationsTable /> : <TranslationsList />}
    </BaseView>
  );
};

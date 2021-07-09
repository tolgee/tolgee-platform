import { useContextSelector } from 'use-context-selector';
import { useTranslate } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseView } from 'tg.component/layout/BaseView';
import { TranslationsContext } from './TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { Navigation } from 'tg.component/navigation/Navigation';
import { TranslationsTable } from './TranslationsTable';
import { TranslationsSearch } from './TranslationsSearch';

export const Translations = () => {
  const t = useTranslate();
  const project = useProject();

  const isLoading = useContextSelector(TranslationsContext, (v) => v.isLoading);
  const isFetching = useContextSelector(
    TranslationsContext,
    (v) => v.isFetching
  );

  return (
    <BaseView
      loading={isLoading || isFetching}
      hideChildrenOnLoading={isLoading && !isFetching}
      navigation={
        <Navigation
          path={[
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
        />
      }
    >
      <TranslationsSearch />
      <TranslationsTable />
    </BaseView>
  );
};

import { T, useTranslate } from '@tolgee/react';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

import { BaseView } from 'tg.component/layout/BaseView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useUrlSearch } from 'tg.hooks/useUrlSearch.ts';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { KeySingle } from './KeySingle/KeySingle';

export const SingleKeyView = () => {
  const t = useTranslate();
  const project = useProject();
  const keyName = useUrlSearch().key as string;

  const translations = useApiQuery({
    url: '/v2/projects/{projectId}/translations',
    method: 'get',
    path: { projectId: project.id },
    query: {
      filterKeyName: keyName as string,
      size: 1,
    },
  });

  const translationExists = Boolean(
    keyName !== undefined ? translations.data?._embedded?.keys?.[0] : undefined
  );

  const defaultSelected = translations.data?.selectedLanguages || [];

  useGlobalLoading(translations.isFetching);

  return !translations.isFetching ? (
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
        [
          translationExists ? (
            <T>single_key_edit_title</T>
          ) : (
            <T>single_key_create_title</T>
          ),
          window.location.pathname + window.location.search,
        ],
      ]}
    >
      <KeySingle
        exists={translationExists}
        defaultSelectedLanguages={defaultSelected}
      />
    </BaseView>
  ) : null;
};

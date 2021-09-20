import { makeStyles } from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';
import { useQueryClient } from 'react-query';
import { useHistory } from 'react-router';

import { LanguagesMenu } from 'tg.component/common/form/LanguagesMenu';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { BaseView } from 'tg.component/layout/BaseView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { queryEncode } from 'tg.hooks/useUrlSearchState';
import { invalidateUrlPrefix } from 'tg.service/http/useQueryApi';
import { useContextSelector } from 'use-context-selector';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { KeyCreateForm } from '../KeyCreateForm';
import { KeyEditForm } from './KeyEditForm';

export type LanguageType = {
  tag: string;
  name: string;
};

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    rowGap: theme.spacing(2),
  },
  languagesMenu: {
    justifySelf: 'end',
  },
  label: {
    fontWeight: 'bold',
  },
}));

type Props = {
  keyName?: string;
};

export const KeySingle: React.FC<Props> = ({ keyName }) => {
  const queryClient = useQueryClient();
  const classes = useStyles();
  const project = useProject();
  const t = useTranslate();

  const dispatch = useTranslationsDispatch();
  const history = useHistory();

  const isFetching = useContextSelector(
    TranslationsContext,
    (c) => c.isFetching
  );
  const translations = useContextSelector(
    TranslationsContext,
    (c) => c.translations
  );
  const selectedLanguages = useContextSelector(
    TranslationsContext,
    (c) => c.selectedLanguages
  )!;
  const allLanguages = useContextSelector(
    TranslationsContext,
    (c) => c.languages
  )!;

  const handleLanguageChange = (languages: string[]) => {
    dispatch({
      type: 'SELECT_LANGUAGES',
      payload: languages,
    });
  };

  const translation = translations?.[0];

  const selectedLanguagesMapped = selectedLanguages?.map((l) => {
    const language = allLanguages?.find(({ tag }) => tag === l);
    return {
      name: language?.name || l,
      tag: l,
    };
  });

  const keyExists = translation && keyName;

  useGlobalLoading(isFetching);

  return allLanguages && selectedLanguages ? (
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
          keyExists ? (
            translation!.keyName
          ) : (
            <T>translation_single_create_title</T>
          ),
          window.location.pathname + window.location.search,
        ],
      ]}
    >
      <div className={classes.container}>
        <div className={classes.languagesMenu}>
          <LanguagesMenu
            languages={allLanguages.map(({ tag, name }) => ({
              value: tag,
              label: name,
            }))}
            onChange={handleLanguageChange}
            value={selectedLanguages}
            context="languages"
          />
        </div>
        {keyExists ? (
          <KeyEditForm />
        ) : (
          <KeyCreateForm
            onSuccess={(data) => {
              // reload translations as new one was created
              invalidateUrlPrefix(
                queryClient,
                '/v2/projects/{projectId}/translations'
              );
              history.push(
                LINKS.PROJECT_TRANSLATIONS_SINGLE.build({
                  [PARAMS.PROJECT_ID]: project.id,
                }) +
                  queryEncode({
                    key: data.name,
                    languages: selectedLanguages,
                  })
              );
            }}
            languages={selectedLanguagesMapped}
            onCancel={() =>
              history.push(
                LINKS.PROJECT_TRANSLATIONS.build({
                  [PARAMS.PROJECT_ID]: project.id,
                })
              )
            }
          />
        )}
      </div>
    </BaseView>
  ) : null;
};

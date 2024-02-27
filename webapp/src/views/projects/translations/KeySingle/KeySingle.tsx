import { styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useQueryClient } from 'react-query';
import { useHistory } from 'react-router-dom';

import { useBottomPanel } from 'tg.component/bottomPanel/BottomPanelContext';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { queryEncode } from 'tg.hooks/useUrlSearchState';
import { invalidateUrlPrefix } from 'tg.service/http/useQueryApi';
import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { KeyCreateForm } from '../KeyCreateForm/KeyCreateForm';
import { KeyEditForm } from './KeyEditForm';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

export type LanguageType = {
  tag: string;
  name: string;
};

const StyledContainer = styled('div')`
  display: grid;
  row-gap: ${({ theme }) => theme.spacing(2)};
`;

const StyledLanguagesMenu = styled('div')`
  justify-self: end;
`;

type Props = {
  keyName?: string;
  keyId?: number;
};

export const KeySingle: React.FC<Props> = ({ keyName, keyId }) => {
  const queryClient = useQueryClient();
  const project = useProject();
  const { t } = useTranslate();

  const { selectLanguages } = useTranslationsActions();
  const history = useHistory();

  const translations = useTranslationsSelector((c) => c.translations);
  const { satisfiesLanguageAccess } = useProjectPermissions();
  const selectedLanguages = useTranslationsSelector(
    (c) => c.selectedLanguages
  )!;
  const languages = useTranslationsSelector((c) => c.languages)?.filter((l) =>
    satisfiesLanguageAccess('translations.edit', l.id)
  );

  const translation = translations?.[0];

  const selectedLanguagesMapped = selectedLanguages
    ?.map((l) => {
      const language = languages?.find(({ tag }) => tag === l);
      return language!;
    })
    .filter(Boolean);

  const { height: bottomPanelHeight } = useBottomPanel();

  const keyExists = translation && (keyName || keyId);

  return languages && selectedLanguages && translations ? (
    <BaseProjectView
      windowTitle={
        keyExists ? translation!.keyName : t('translation_single_create_title')
      }
      navigation={[
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
            <T keyName="translation_single_create_title" />
          ),
          window.location.pathname + window.location.search,
        ],
      ]}
    >
      <StyledContainer style={{ marginBottom: bottomPanelHeight + 20 }}>
        {keyExists ? (
          <>
            <StyledLanguagesMenu>
              <LanguagesSelect
                languages={languages}
                onChange={selectLanguages}
                value={selectedLanguagesMapped.map((l) => l.tag)}
                context="languages"
              />
            </StyledLanguagesMenu>
            <KeyEditForm />
          </>
        ) : (
          <KeyCreateForm
            baseLanguage={project.baseLanguage!.tag}
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
            onCancel={() =>
              history.push(
                LINKS.PROJECT_TRANSLATIONS.build({
                  [PARAMS.PROJECT_ID]: project.id,
                })
              )
            }
          />
        )}
      </StyledContainer>
    </BaseProjectView>
  ) : null;
};

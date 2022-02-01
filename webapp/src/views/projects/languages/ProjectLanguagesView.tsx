import { BaseView } from 'tg.component/layout/BaseView';
import { useTranslate } from '@tolgee/react';
import { ProjectSettingsLanguages } from 'tg.views/projects/languages/ProjectSettingsLanguages';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

export const ProjectLanguagesView = () => {
  const t = useTranslate();

  const project = useProject();

  return (
    <BaseView
      lg={7}
      md={9}
      containerMaxWidth="lg"
      navigation={[
        [
          project.name,
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
        [
          t('project_menu_languages'),
          LINKS.PROJECT_LANGUAGES.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
      windowTitle={t('languages_title', undefined, true)}
    >
      <ProjectSettingsLanguages />
    </BaseView>
  );
};

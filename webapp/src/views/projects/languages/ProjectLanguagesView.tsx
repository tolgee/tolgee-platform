import { useTranslate } from '@tolgee/react';
import { ProjectSettingsLanguages } from 'tg.views/projects/languages/ProjectSettingsLanguages';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { BaseProjectView } from '../BaseProjectView';

export const ProjectLanguagesView = () => {
  const { t } = useTranslate();

  const project = useProject();

  return (
    <BaseProjectView
      lg={7}
      md={9}
      containerMaxWidth="lg"
      windowTitle={t('languages_title')}
      navigation={[
        [
          t('languages_title'),
          LINKS.PROJECT_LANGUAGES.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <ProjectSettingsLanguages />
    </BaseProjectView>
  );
};

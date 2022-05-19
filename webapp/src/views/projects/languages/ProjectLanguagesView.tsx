import { BaseView } from 'tg.component/layout/BaseView';
import { useTranslate } from '@tolgee/react';
import { ProjectSettingsLanguages } from 'tg.views/projects/languages/ProjectSettingsLanguages';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { SmallProjectAvatar } from 'tg.component/navigation/SmallProjectAvatar';

export const ProjectLanguagesView = () => {
  const t = useTranslate();

  const project = useProject();

  return (
    <BaseView
      lg={7}
      md={9}
      containerMaxWidth="lg"
      windowTitle={t('languages_title')}
      navigation={[
        [
          project.name,
          LINKS.PROJECT_DASHBOARD.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
          <SmallProjectAvatar key={0} project={project} />,
        ],
        [
          t('languages_title'),
          LINKS.PROJECT_LANGUAGES.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <ProjectSettingsLanguages />
    </BaseView>
  );
};

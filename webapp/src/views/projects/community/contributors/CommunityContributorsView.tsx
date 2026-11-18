import { useTranslate } from '@tolgee/react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { BaseView } from 'tg.component/layout/BaseView';
import { ProjectLanguagesProvider } from 'tg.hooks/ProjectLanguagesProvider';
import { ContributorsContent } from 'tg.views/projects/community/contributors/ContributorsContent';

export const CommunityContributorsView = () => {
  const { t } = useTranslate();

  return (
    <DashboardPage>
      <BaseView
        windowTitle={t(
          'community_contributions_title',
          'Project contributions'
        )}
        maxWidth="wide"
      >
        <ProjectLanguagesProvider>
          <ContributorsContent />
        </ProjectLanguagesProvider>
      </BaseView>
    </DashboardPage>
  );
};

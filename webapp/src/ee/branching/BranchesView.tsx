import { useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { BranchesList } from 'tg.ee.module/branching/components/BranchesList';

export const BranchesView = () => {
  const { t } = useTranslate();
  const project = useProject();

  return (
    <BaseProjectView
      maxWidth={700}
      windowTitle={t('branches_title')}
      navigation={[
        [
          t('branches_title'),
          LINKS.PROJECT_BRANCHES.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <BranchesList />
    </BaseProjectView>
  );
};

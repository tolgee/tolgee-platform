import { useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { BaseProjectView } from 'tg.views/projects/BaseProjectView';
import { BranchesList } from 'tg.ee.module/branching/components/BranchesList';
import { BranchMergesList } from 'tg.ee.module/branching/components/merge/BranchMergesList';
import { Box, Tab, Tabs, styled } from '@mui/material';
import { Link, useRouteMatch } from 'react-router-dom';

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
  overflow: visible;
  & * {
    overflow: visible;
  }
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const BranchesView = () => {
  const { t } = useTranslate();
  const project = useProject();
  const mergesMatch = useRouteMatch(LINKS.PROJECT_BRANCHES_MERGES.template);
  const currentTab = mergesMatch?.isExact ? 'merges' : 'branches';

  return (
    <BaseProjectView
      maxWidth={900}
      windowTitle={t('branches_title')}
      title={t('branches_title')}
      navigation={[
        [
          t('branches_title'),
          LINKS.PROJECT_BRANCHES.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <StyledTabWrapper>
        <StyledTabs value={currentTab}>
          <Tab
            value="branches"
            component={Link}
            to={LINKS.PROJECT_BRANCHES.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            label={t('branches_title')}
            data-cy="branches-tab-branches"
          />
          <Tab
            value="merges"
            component={Link}
            to={LINKS.PROJECT_BRANCHES_MERGES.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            label={t('branch_merges_tab_label')}
            data-cy="branches-tab-merges"
            sx={{ overflow: 'visible' }}
          />
        </StyledTabs>
      </StyledTabWrapper>

      {currentTab === 'merges' ? <BranchMergesList /> : <BranchesList />}
    </BaseProjectView>
  );
};

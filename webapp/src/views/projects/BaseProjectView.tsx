import { Box } from '@mui/material';
import { useHistory } from 'react-router-dom';
import React from 'react';

import { BaseView, BaseViewProps } from 'tg.component/layout/BaseView';
import { NavigationItem } from 'tg.component/navigation/Navigation';
import { SmallProjectAvatar } from 'tg.component/navigation/SmallProjectAvatar';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { BatchOperationsSummary } from './translations/BatchOperations/OperationsSummary/OperationsSummary';
import { CriticalUsageCircle } from 'tg.ee';
import { ProjectPage } from './ProjectPage';
import { GlobalBranchSelector } from 'tg.component/branching/GlobalBranchSelector';
import { useBranchLinks } from 'tg.component/branching/useBranchLinks';

type Props = BaseViewProps & {
  rightPanelContent?: (width: number) => React.ReactNode;
  branching?: boolean;
};

export const BaseProjectView: React.FC<Props> = ({
  navigation,
  rightPanelContent,
  branching,
  ...otherProps
}) => {
  const project = useProject() as ReturnType<typeof useProject> | undefined;
  const { withBranchLink, withBranchUrl } = useBranchLinks();
  const history = useHistory();

  const handleOrganizationChange = () => {
    history.push(LINKS.PROJECTS.build());
  };

  const prefixNavigation: NavigationItem[] = [
    [<OrganizationSwitch key={0} onSelect={handleOrganizationChange} />],
  ];

  if (project) {
    prefixNavigation.push([
      project.name,
      withBranchLink(LINKS.PROJECT_DASHBOARD, {
        [PARAMS.PROJECT_ID]: project.id,
      }),
      <SmallProjectAvatar key="avatar" project={project} />,
      branching ? <GlobalBranchSelector key="branch" /> : undefined,
    ]);
  }

  return (
    <ProjectPage rightPanelContent={rightPanelContent}>
      <BaseView
        {...otherProps}
        navigation={[
          ...prefixNavigation,
          ...(navigation || []).map(
            ([name, url, icon, suffix]) =>
              [name, withBranchUrl(url), icon, suffix] as NavigationItem
          ),
        ]}
        navigationRight={
          <Box display="grid" gridAutoFlow="column" gap={1}>
            <BatchOperationsSummary />
            <CriticalUsageCircle />
          </Box>
        }
      />
    </ProjectPage>
  );
};

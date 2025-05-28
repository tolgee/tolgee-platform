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

type Props = BaseViewProps & {
  rightPanelContent?: (width: number) => React.ReactNode;
};

export const BaseProjectView: React.FC<Props> = ({
  navigation,
  rightPanelContent,
  ...otherProps
}) => {
  const project = useProject() as ReturnType<typeof useProject> | undefined;

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
      LINKS.PROJECT_DASHBOARD.build({
        [PARAMS.PROJECT_ID]: project.id,
      }),
      <SmallProjectAvatar key={0} project={project} />,
    ]);
  }

  return (
    <ProjectPage rightPanelContent={rightPanelContent}>
      <BaseView
        {...otherProps}
        navigation={[...prefixNavigation, ...(navigation || [])]}
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

import { FunctionComponent } from 'react';
import { styled } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useProject } from 'tg.hooks/useProject';

import { ProjectMenu } from './projectMenu/ProjectMenu';

const StyledContent = styled('div')`
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  position: relative;
  max-width: 100%;
`;

export const ProjectPage: FunctionComponent = ({ children }) => {
  const project = useProject();

  const isAdminAccess = project.computedPermission.origin === 'SERVER_ADMIN';

  return (
    <DashboardPage
      isAdminAccess={isAdminAccess}
      fixedContent={<ProjectMenu id={project.id} />}
    >
      <StyledContent>{children}</StyledContent>
    </DashboardPage>
  );
};

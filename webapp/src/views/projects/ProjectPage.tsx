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
  contain: size;
`;

interface Props {
  topBarAutoHide?: boolean;
}

export const ProjectPage: FunctionComponent<Props> = ({
  topBarAutoHide,
  children,
}) => {
  const project = useProject();

  const isAdminAccess = project.computedPermission.origin === 'SERVER_ADMIN';

  return (
    <DashboardPage
      topBarAutoHide={topBarAutoHide}
      isAdminAccess={isAdminAccess}
    >
      <ProjectMenu id={project.id} />
      <StyledContent>{children}</StyledContent>
    </DashboardPage>
  );
};

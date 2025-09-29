import React from 'react';
import { styled } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useProject } from 'tg.hooks/useProject';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';

import { ProjectMenu } from './projectMenu/ProjectMenu';

const StyledContent = styled('div')`
  display: grid;
  max-width: 100%;
`;

type Props = {
  rightPanelContent?: (width: number) => React.ReactNode;
};

export const ProjectPage: React.FC<Props> = ({
  children,
  rightPanelContent,
}) => {
  const project = useProject();

  const isAdminAccess =
    project.computedPermission.origin === 'SERVER_ADMIN' ||
    project.computedPermission.origin === 'SERVER_SUPPORTER';

  return (
    <DashboardPage
      isAdminAccess={isAdminAccess}
      fixedContent={<ProjectMenu />}
      rightPanelContent={rightPanelContent}
    >
      <React.Suspense fallback={<FullPageLoading />}>
        <StyledContent>{children}</StyledContent>
      </React.Suspense>
    </DashboardPage>
  );
};

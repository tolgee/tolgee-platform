import * as React from 'react';
import {FunctionComponent} from 'react';

import {DashboardPage} from '../../layout/DashboardPage';
import {useProject} from '../../../hooks/useProject';
import {ProjectMenu} from './ProjectMenu';

interface Props {
  fullWidth?: boolean;
}

export const ProjectPage: FunctionComponent<Props> = (props) => {
  const project = useProject();

  return (
    <DashboardPage
      fullWidth={props.fullWidth}
      projectName={project.name}
      sideMenuItems={<ProjectMenu id={project.id} />}
    >
      {props.children}
    </DashboardPage>
  );
};

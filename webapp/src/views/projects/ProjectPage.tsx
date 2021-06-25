import * as React from 'react';
import { FunctionComponent } from 'react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useProject } from 'tg.hooks/useProject';
import { ProjectMenu } from './projectMenu/ProjectMenu';
import { makeStyles } from '@material-ui/core';

const useStyle = makeStyles({
  content: {
    height: '100%',
    overflowY: 'auto',
    flexGrow: 1,
  },
});

interface Props {
  fullWidth?: boolean;
}

export const ProjectPage: FunctionComponent<Props> = (props) => {
  const project = useProject();
  const classes = useStyle();

  return (
    <DashboardPage fullWidth={props.fullWidth} projectName={project.name}>
      <ProjectMenu id={project.id} />
      <div className={classes.content}>{props.children}</div>
    </DashboardPage>
  );
};

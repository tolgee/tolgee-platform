import { FunctionComponent } from 'react';
import { makeStyles } from '@material-ui/core';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useProject } from 'tg.hooks/useProject';

import { ProjectMenu } from './projectMenu/ProjectMenu';

const useStyle = makeStyles({
  content: {
    flexGrow: 1,
    overflowX: 'hidden',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'stretch',
    position: 'relative',
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

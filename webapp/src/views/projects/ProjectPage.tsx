import { FunctionComponent } from 'react';
import { makeStyles } from '@material-ui/core';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useProject } from 'tg.hooks/useProject';

import { ProjectMenu } from './projectMenu/ProjectMenu';

const useStyle = makeStyles({
  content: {
    flexGrow: 1,
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'stretch',
    position: 'relative',
    contain: 'size',
  },
});

interface Props {
  topBarAutoHide?: boolean;
}

export const ProjectPage: FunctionComponent<Props> = ({
  topBarAutoHide,
  children,
}) => {
  const project = useProject();
  const classes = useStyle();

  return (
    <DashboardPage topBarAutoHide={topBarAutoHide}>
      <ProjectMenu id={project.id} />
      <div className={classes.content}>{children}</div>
    </DashboardPage>
  );
};

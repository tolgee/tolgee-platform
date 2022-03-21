import { FunctionComponent } from 'react';
import makeStyles from '@mui/styles/makeStyles';
import { Theme } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useProject } from 'tg.hooks/useProject';

import { ProjectMenu } from './projectMenu/ProjectMenu';

const useStyle = makeStyles<Theme>({
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

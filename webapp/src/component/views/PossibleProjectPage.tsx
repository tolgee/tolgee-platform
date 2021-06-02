import * as React from 'react';
import {FunctionComponent} from 'react';
import {useSelector} from 'react-redux';
import {AppState} from '../../store';
import {ProjectPage} from './projects/ProjectPage';
import {DashboardPage} from '../layout/DashboardPage';

export const PossibleProjectPage: FunctionComponent = (props) => {
  let project = useSelector(
    (state: AppState) => state.projects.loadables.project.loaded
  );

  return project ? (
    <ProjectPage {...props} />
  ) : (
    <DashboardPage {...props} />
  );
};

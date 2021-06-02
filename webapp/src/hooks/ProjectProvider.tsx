import * as React from 'react';
import {FunctionComponent, useEffect} from 'react';
import {container} from 'tsyringe';
import {ProjectActions} from '../store/project/ProjectActions';
import {useSelector} from 'react-redux';
import {AppState} from '../store';
import {GlobalError} from '../error/GlobalError';
import {FullPageLoading} from '../component/common/FullPageLoading';

const projectActions = container.resolve(ProjectActions);

export const ProjectProvider: FunctionComponent<{ id: number }> = (
  props
) => {
  let projectDTOLoadable = useSelector(
    (state: AppState) => state.projects.loadables.project
  );

  const isLoading = projectDTOLoadable.loading;
  const init =
    !projectDTOLoadable.data && !projectDTOLoadable.error && !isLoading;
  const idChanged =
    projectDTOLoadable.dispatchParams &&
    projectDTOLoadable.dispatchParams[0] !== props.id;

  useEffect(() => {
    if (init || idChanged) {
      projectActions.loadableActions.project.dispatch(props.id);
    }
  }, []);

  if (projectDTOLoadable.loading || init || idChanged) {
    return <FullPageLoading />;
  }

  if (projectDTOLoadable.data) {
    return <>{props.children}</>;
  }

  throw new GlobalError(
    'Unexpected error occurred',
    projectDTOLoadable.error?.code || 'Loadable error'
  );
};

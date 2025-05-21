import { useEffect } from 'react';
import { useHistory, useLocation } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useProject } from 'tg.hooks/useProject';
import { ProjectPage } from 'tg.views/projects/ProjectPage';

export const ActivityDetailRedirect = () => {
  const project = useProject();
  const history = useHistory();
  const location = useLocation();

  useEffect(() => {
    const queryParameters = new URLSearchParams(location.search);
    const activityId = queryParameters.get('activity');
    if (project && activityId !== null) {
      const fullPath = [
        LINKS.PROJECT_DASHBOARD.build({
          [PARAMS.PROJECT_ID]: project.id,
        }),
        `?activity=${activityId}`,
      ]
        .filter(Boolean)
        .join('');
      history.replace(fullPath);
    }
  }, [project]);
  return (
    <ProjectPage>
      <BoxLoading />
    </ProjectPage>
  );
};

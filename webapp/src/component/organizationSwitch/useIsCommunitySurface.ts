import { useRouteMatch } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';

export const useIsCommunitySurface = () =>
  Boolean(useRouteMatch(LINKS.COMMUNITY_PROJECTS.template));

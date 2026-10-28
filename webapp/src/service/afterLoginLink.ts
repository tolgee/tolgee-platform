import { matchPath } from 'react-router-dom';

import { LINKS } from 'tg.constants/links';

// paths which shouldn't be stored, so we avoid loops
const PATHS_NOT_TO_REMEMBER = [LINKS.PROJECTS, LINKS.LOGIN, LINKS.ROOT];

export const shouldRememberAfterLogin = (location: string) =>
  !PATHS_NOT_TO_REMEMBER.some((link) =>
    matchPath(location, {
      path: link.template,
      exact: true,
      strict: false,
    })
  );

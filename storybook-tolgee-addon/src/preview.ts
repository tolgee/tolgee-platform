import type { ProjectAnnotations, Renderer } from 'storybook/internal/types';

import { GLOBAL_KEY } from './constants';
import { TolgeeAddonGlobals } from './types';

export const initialGlobals: ProjectAnnotations<Renderer>['initialGlobals'] &
  TolgeeAddonGlobals = {
  [GLOBAL_KEY]: undefined,
};

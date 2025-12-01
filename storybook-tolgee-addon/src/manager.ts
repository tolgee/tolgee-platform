import { addons, types } from 'storybook/manager-api';

import { ADDON_ID, TOOL_ID, PARAM_KEY } from './constants';
import { MenuSwitcher } from './menuSwitcher';

addons.register(ADDON_ID, () => {
  addons.add(TOOL_ID, {
    title: 'Languages',
    type: types.TOOL,
    match: ({ viewMode, tabId }) =>
      !!(viewMode && viewMode.match(/^(story|docs)$/)) && !tabId,
    render: MenuSwitcher,
    paramKey: PARAM_KEY,
  });
});

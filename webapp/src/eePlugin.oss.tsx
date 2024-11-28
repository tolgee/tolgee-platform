import { FC } from 'react';
import { PluginType } from './plugin/PluginType';

const NotIncludedInOss: FC = () => {
  return <div>Not included in OSS</div>;
};

const Empty: FC = () => {
  return null;
};

export const eePlugin: PluginType = {
  ee: {
    activity: {
      TaskReference: NotIncludedInOss,
    },
    PermissionsAdvanced: NotIncludedInOss,
    billing: {
      billingMenuItems: [],
      GlobalLimitPopover: Empty,
    },
    organization: {
      apps: Empty,
      Usage: Empty,
    },
    routes: {
      Root: Empty,
      Administration: Empty,
      Organization: Empty,
      Project: Empty,
    },
    tasks: {
      useUserTaskCount: () => 0,
      TranslationTaskIndicator: NotIncludedInOss,
      PrefilterTask: NotIncludedInOss,
      TranslationsTaskDetail: NotIncludedInOss,
    },

    useAddBatchOperations: () => () => [],
    translationPanelAdder: () => [],
  },
};

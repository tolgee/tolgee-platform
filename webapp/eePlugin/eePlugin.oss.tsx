import { FC } from 'react';
import { EePluginType } from '../src/plugin/EePluginType';

const NotIncludedInOss: FC = () => {
  return <div>Not included in OSS</div>;
};

const Empty: FC = () => {
  return null;
};

export const eePlugin: EePluginType = {
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
    useAddDeveloperViewItems: () => (existingItems) => existingItems,
    useAddBatchOperations: () => (existingItems) => existingItems,
    translationPanelAdder: (existingItems) => existingItems,
  },
};

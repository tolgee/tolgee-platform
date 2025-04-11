import React from 'react';
import type { BillingMenuItemsProps } from './EeModuleType';

const NotIncludedInOss =
  (name: string): ((props?: any) => any) =>
  // eslint-disable-next-line react/display-name
  () => {
    return <div>Not included in OSS ({name})</div>;
  };

const Empty: (props?: any) => any = () => {
  return null;
};

export const TaskReference = NotIncludedInOss('TaskReference');
export const PermissionsAdvancedEe = NotIncludedInOss('PermissionsAdvancedEe');
export const GlobalLimitPopover = Empty;
export const billingMenuItems = [] as React.FC<BillingMenuItemsProps>[];
export const apps = [] as React.FC[];
export const Usage = Empty;
export const AgencyLabel = NotIncludedInOss('AgencyLabel');
export const OrderTranslationsDialog = Empty;
export const TaskItem = Empty;
export const TaskFilterPopover = Empty;
export const TaskAllDonePlaceholder = Empty;

export const routes = {
  Root: Empty,
  Administration: Empty,
  Organization: Empty,
  Project: Empty,
};
export const useUserTaskCount = () => 0;
export const TranslationTaskIndicator = NotIncludedInOss(
  'TranslationTaskIndicator'
);
export const PrefilterTask = NotIncludedInOss('PrefilterTask');
export const PrefilterTaskShowDoneSwitch = NotIncludedInOss(
  'PrefilterTaskShowDoneSwitch'
);
export const TranslationsTaskDetail = Empty;

export const useAddDeveloperViewItems = () => (existingItems) => existingItems;
export const useAddBatchOperations = () => (existingItems) => existingItems;
export const translationPanelAdder = (existingItems) => existingItems;
export const useAddProjectMenuItems = () => (existingItems) => existingItems;
export const useAddUserMenuItems = () => (existingItems) => existingItems;
export const useAddAdministrationMenuItems = () => (existingItems) =>
  existingItems;
export const TrialAnnouncement = Empty;
export const TrialChip = Empty;
export const TaskInfoMessage = Empty;
export const AiPrompt = Empty;

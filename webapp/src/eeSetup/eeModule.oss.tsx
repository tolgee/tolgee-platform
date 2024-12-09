import React from 'react';

const NotIncludedInOss =
  (name: string): React.FC<any> =>
  // eslint-disable-next-line react/display-name
  () => {
    return <div>Not included in OSS ({name})</div>;
  };

const Empty: React.FC<any> = () => {
  return null;
};

export const TaskReference = NotIncludedInOss('TaskReference');
export const PermissionsAdvancedEe = NotIncludedInOss('PermissionsAdvancedEe');
export const GlobalLimitPopover = Empty;
export const billingMenuItems = [];
export const apps = [];
export const Usage = Empty;
export const routes = {
  Root: Empty,
  Administration: Empty,
  Organization: Empty,
  SpecificOrganization: Empty,
  Project: Empty,
};
export const useUserTaskCount = () => 0;
export const TranslationTaskIndicator = NotIncludedInOss(
  'TranslationTaskIndicator'
);
export const PrefilterTask = NotIncludedInOss('PrefilterTask');
export const TranslationsTaskDetail = Empty;

export const useAddDeveloperViewItems = () => (existingItems) => existingItems;
export const useAddBatchOperations = () => (existingItems) => existingItems;
export const translationPanelAdder = (existingItems) => existingItems;
export const useAddProjectMenuItems = () => (existingItems) => existingItems;
export const useAddUserMenuItems = () => (existingItems) => existingItems;
export const useAddAdministrationMenuItems = () => (existingItems) =>
  existingItems;

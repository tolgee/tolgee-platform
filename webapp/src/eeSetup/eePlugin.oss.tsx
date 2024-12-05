import React from 'react';

const NotIncludedInOss: React.FC = () => {
  return <div>Not included in OSS</div>;
};

const Empty: React.FC = () => {
  return null;
};

export const TaskReference = NotIncludedInOss;
export const PermissionsAdvancedEe = NotIncludedInOss;
export const GlobalLimitPopover = Empty;
export const billingMenuItems = [];
export const apps = [];
export const Usage = Empty;
export const routes = {
  Root: Empty,
  Administration: Empty,
  Organization: Empty,
  Project: Empty,
};
export const useUserTaskCount = () => 0;
export const TranslationTaskIndicator = NotIncludedInOss;
export const PrefilterTask = NotIncludedInOss;
export const TranslationsTaskDetail = NotIncludedInOss;

export const useAddDeveloperViewItems = () => (existingItems) => existingItems;
export const useAddBatchOperations = () => (existingItems) => existingItems;
export const translationPanelAdder = (existingItems) => existingItems;
export const useAddProjectMenuItems = () => (existingItems) => existingItems;
export const useAddUserMenuItems = () => (existingItems) => existingItems;
export const useAddAdministrationMenuItems = () => (existingItems) =>
  existingItems;

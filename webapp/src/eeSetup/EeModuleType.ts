import {
  HierarchyItem,
  LanguageModel,
  PermissionAdvancedState,
} from 'tg.component/PermissionsSettings/types';
import { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { BatchOperationAdder } from 'tg.views/projects/translations/BatchOperations/operations';
import { addPanel } from 'tg.views/projects/translations/ToolsPanel/panelsList';
import { DeveloperViewItemsAdder } from 'tg.views/projects/developer/developerViewItems';
import { UserMenuItemsAdder } from 'tg.component/security/UserMenu/UserMenuItems';
import { ProjectMenuItemsAdder } from 'tg.views/projects/projectMenu/ProjectMenu';
import { AdministrationMenuItemsAdder } from 'tg.views/administration/components/BaseAdministrationView';

export interface EeModuleType {
  PermissionsAdvancedEe: FC<PermissionsAdvancedEeProps>;
  billingMenuItems: FC<BillingMenuItemsProps>[];
  GlobalLimitPopover: FC;
  TaskReference: FC<TaskReferenceProps>;
  apps: FC[];
  Usage: FC;
  useUserTaskCount: () => number;
  TranslationTaskIndicator: FC<TranslationTaskIndicatorProps>;
  PrefilterTask: FC<PrefilterTaskProps>;
  TranslationsTaskDetail: FC;
  routes: {
    Root: FC;
    Administration: FC;
    Organization: FC;
    SpecificOrganization: FC;
    Project: FC;
  };
  useAddBatchOperations: () => BatchOperationAdder;
  translationPanelAdder: ReturnType<typeof addPanel>;
  useAddDeveloperViewItems: () => DeveloperViewItemsAdder;
  useAddUserMenuItems: () => UserMenuItemsAdder;
  useAddProjectMenuItems: () => ProjectMenuItemsAdder;
  useAddAdministrationMenuItems: () => AdministrationMenuItemsAdder;
}

export type PermissionsAdvancedEeProps = {
  dependencies: HierarchyItem;
  state: PermissionAdvancedState;
  onChange: (value: PermissionAdvancedState) => void;
  allLangs?: LanguageModel[];
};

export type BillingMenuItemsProps = {
  onClose: () => void;
};

export type TaskModel = components['schemas']['TaskModel'];

export type TaskReferenceData = {
  type: 'task';
  name: string;
  taskType: TaskModel['type'];
  number: number;
};

export type TaskReferenceProps = {
  data: TaskReferenceData;
};

export type TranslationTaskIndicatorProps = {
  task?: components['schemas']['KeyTaskViewModel'];
};

export type PrefilterTaskProps = {
  taskNumber: number;
};

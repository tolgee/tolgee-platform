import React from 'react';
import {
  BillingMenuItemsProps,
  GlossaryTermHighlightModel,
  GlossaryTermHighlightsProps,
  GlossaryTermPreviewProps,
  QaLanguageStatsProps,
  QaBadgeProps,
  SubfilterQaChecksProps,
} from './EeModuleType';
import type { FiltersInternal } from 'tg.views/projects/translations/TranslationFilters/tools';

const NotIncludedInOss = (name: string): ((props?: any) => any) => {
  function NotIncludedInOss(props: any, ref: any) {
    return (
      <div {...props} ref={ref}>
        Not included in OSS ({name})
      </div>
    );
  }
  return React.forwardRef(NotIncludedInOss);
};

const Empty: (props?: any) => any = () => {
  return null;
};

export const TaskReference = NotIncludedInOss('TaskReference');
export const BranchReference = NotIncludedInOss('BranchReference');
export const PermissionsAdvancedEe = NotIncludedInOss('PermissionsAdvancedEe');
export const GlobalLimitPopover = Empty;
export const billingMenuItems = [] as React.FC<BillingMenuItemsProps>[];
export const apps = [] as React.FC[];
export const AgencyLabel = NotIncludedInOss('AgencyLabel');
export const OrderTranslationsDialog = Empty;
export const TaskItem = Empty;
export const TaskFilterPopover = Empty;
export const TaskAllDonePlaceholder = Empty;
export const QaBadge = (_props: QaBadgeProps) => Empty() as JSX.Element;
export const QaLanguageStats = (_props: QaLanguageStatsProps) =>
  Empty() as JSX.Element;
export const QaCheckItem = Empty;
export const SubfilterQaChecks = (_props: SubfilterQaChecksProps) =>
  Empty() as JSX.Element;
export const getQaChecksFiltersLength = (_value: FiltersInternal) => 0;
export const getQaChecksFiltersName = (
  _value: FiltersInternal
): JSX.Element | undefined => undefined;

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
export const glossaryPanelAdder = (existingItems) => existingItems;
export const qaChecksPanelAdder = (existingItems) => existingItems;
export const useAddProjectMenuItems = () => (existingItems) => existingItems;
export const useAddUserMenuItems = () => (existingItems) => existingItems;
export const useAddAdministrationMenuItems = () => (existingItems) =>
  existingItems;
export const TrialAnnouncement = Empty;
export const TrialChip = Empty;
export const TaskInfoMessage = Empty;

export const CriticalUsageCircle = Empty;
export const AiPrompt = Empty;
export const AiContextData = Empty;
export const AiPromptsList = Empty;

export const useGlossaryTermHighlights = (
  props: GlossaryTermHighlightsProps
): GlossaryTermHighlightModel[] => [];

export const GlossaryTermPreview: React.VFC<GlossaryTermPreviewProps> =
  NotIncludedInOss('Glossaries');
export const useAddProjectSettingsTabs =
  (projectId: number) => (existingItems) =>
    existingItems;

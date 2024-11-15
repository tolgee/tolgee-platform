import { components } from 'tg.service/apiSchema.generated';

export type LanguageModel = components['schemas']['LanguageModel'];
export type LanguageConfigItemModel =
  components['schemas']['LanguageConfigItemModel'];
export type LanguageInfoModel = components['schemas']['LanguageInfoModel'];
export type ServiceType =
  LanguageConfigItemModel['enabledServicesInfo'][number]['serviceType'];
export type FormalityType =
  LanguageConfigItemModel['enabledServicesInfo'][number]['formality'];
export type MachineTranslationLanguagePropsDto =
  components['schemas']['MachineTranslationLanguagePropsDto'];
export type AutoTranslationSettingsDto =
  components['schemas']['AutoTranslationSettingsDto'];

export type MtServiceInfo = components['schemas']['MtServiceInfo'];

export type AutoTranslationSettings = {
  enableForImport: boolean;
  usingMachineTranslation: boolean;
  usingTranslationMemory: boolean;
};

export type LanguageCombinedSetting = {
  id: number | null;
  info: LanguageInfoModel | undefined;
  mtSettings: LanguageConfigItemModel | undefined;
  autoSettings: AutoTranslationSettingsDto | undefined;
  language: LanguageModel | undefined;
};

export type RowData = {
  inheritedFromDefault: boolean;
  settings: LanguageCombinedSetting;
  onChange: OnMtChange;
};

export type OnMtChange = (
  langInfo: LanguageInfoModel,
  value: OnRowChangeData | null
) => Promise<void>;

export type OnRowChangeData = {
  machineTranslation: MachineTranslationLanguagePropsDto;
  autoTranslation: AutoTranslationSettingsDto;
};

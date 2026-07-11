import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

export type LanguageConfigItemModel =
  components['schemas']['LanguageConfigItemModel'];
export type ServiceType =
  LanguageConfigItemModel['enabledServicesInfo'][number]['serviceType'];

export const useServiceName = () => {
  const { t } = useTranslate();
  return (service: ServiceType) => {
    switch (service) {
      case 'GOOGLE':
        return 'Google';
      case 'DEEPL':
        return 'DeepL';
      case 'AZURE':
        return 'Azure Cognitive';
      case 'BAIDU':
        return 'Baidu';
      case 'AWS':
        return 'Amazon Translate';
      case 'PROMPT':
        return t('ai_translator');
      default:
        return service;
    }
  };
};

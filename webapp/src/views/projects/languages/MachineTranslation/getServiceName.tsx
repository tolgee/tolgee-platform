import { ServiceType } from './types';

export const getServiceName = (service: ServiceType) => {
  switch (service) {
    case 'GOOGLE':
      return 'Google';
    case 'DEEPL':
      return 'DeepL';
    case 'AZURE':
      return 'Azure Cognitive';
    case 'BAIDU':
      return 'Baidu';
    case 'TOLGEE':
      return 'Tolgee';
    case 'AWS':
      return 'Amazon Translate';
    case 'OPENAI':
      return 'OpenAI';
    default:
      return service;
  }
};

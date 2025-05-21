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
    case 'AWS':
      return 'Amazon Translate';
    case 'PROMPT':
      return 'LLM Prompt';
    default:
      return service;
  }
};

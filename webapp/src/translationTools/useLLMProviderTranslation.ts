import { useTranslate } from '@tolgee/react';
import { assertUnreachable } from 'tg.fixtures/assertUnreachable';
import { components } from 'tg.service/apiSchema.generated';

export type LLMProviderModel = components['schemas']['LLMProviderModel'];
export type LLMProviderType = LLMProviderModel['type'];

export const useLLMProviderTranslation = () => {
  const { t } = useTranslate();
  return (type: LLMProviderType) => {
    switch (type) {
      case 'OPENAI':
        return t('llm_provider_type_openai');
      case 'OPENAI_AZURE':
        return t('llm_provider_type_openai_azure');
      default:
        return assertUnreachable(type);
    }
  };
};

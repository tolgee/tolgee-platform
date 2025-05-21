import { useTranslate } from '@tolgee/react';
import { assertUnreachable } from 'tg.fixtures/assertUnreachable';
import { components } from 'tg.service/apiSchema.generated';

export type LlmProviderModel = components['schemas']['LlmProviderModel'];
export type LlmProviderType = LlmProviderModel['type'];

export const useLLMProviderTranslation = () => {
  const { t } = useTranslate();
  return (type: LlmProviderType) => {
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

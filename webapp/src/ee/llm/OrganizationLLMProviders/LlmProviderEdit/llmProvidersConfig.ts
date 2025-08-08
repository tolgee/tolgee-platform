import { TranslateFunction } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import * as Yup from 'yup';

export type LlmProviderModel = components['schemas']['LlmProviderModel'];
export type LlmProviderType = Exclude<LlmProviderModel['type'], 'TOLGEE'>;
type LlmProviderRequest = components['schemas']['LlmProviderRequest'];

export type ProviderOptions = {
  label: string;
  hint?: string;
  enum?: (string | undefined)[];
  optional?: boolean;
  defaultValue?: string;
};

type ProvidersConfig = Record<
  LlmProviderType,
  Partial<Record<keyof LlmProviderModel, Partial<ProviderOptions>>>
>;

export const llmProvidersDefaults = (
  t: TranslateFunction
): Partial<Record<keyof LlmProviderModel, ProviderOptions>> => ({
  name: {
    label: t('llm_provider_form_name'),
    hint: t('llm_provider_form_name_hint'),
  },
  apiUrl: { label: t('llm_provider_form_api_url') },
  apiKey: { label: t('llm_provider_form_api_key') },
  model: { label: t('llm_provider_form_model') },
  format: { label: t('llm_provider_form_format') },
  deployment: { label: t('llm_provider_form_deployment') },
});

export const llmProvidersConfig = (t: TranslateFunction): ProvidersConfig => {
  const reasoningEffort: Partial<ProviderOptions> = {
    label: t('llm_provider_form_openai_reasoning_effort'),
    hint: t('llm_provider_form_openai_reasoning_effort_hint'),
    optional: true,
    enum: [undefined, 'minimal', 'low', 'medium', 'high'],
    defaultValue: undefined,
  };
  return {
    OPENAI: {
      name: {},
      apiUrl: {
        defaultValue: 'https://api.openai.com',
      },
      apiKey: {
        optional: true,
      },
      model: {
        hint: t('llm_provider_form_openai_model_hint'),
      },
      format: {
        hint: t('llm_provider_form_openai_format_hint'),
        optional: true,
        enum: [undefined, 'json_schema'],
        defaultValue: 'json_schema',
      },
      reasoningEffort,
    },
    OPENAI_AZURE: {
      name: {},
      apiUrl: {},
      apiKey: {},
      deployment: {
        hint: t('llm_provider_form_azure_openai_deployment_hint'),
      },
      format: {
        hint: t('llm_provider_form_openai_format_hint'),
        optional: true,
        enum: [undefined, 'json_schema'],
        defaultValue: 'json_schema',
      },
      reasoningEffort,
    },
    ANTHROPIC: {
      name: {},
      apiUrl: {
        defaultValue: 'https://api.anthropic.com',
      },
      apiKey: {},
      model: {
        hint: t('llm_provider_form_anthropic_model_hint'),
      },
    },
    GOOGLE_AI: {
      name: {},
      apiUrl: {
        defaultValue: 'https://generativelanguage.googleapis.com',
      },
      apiKey: {},
      model: {
        hint: t('llm_provider_form_google_ai_model_hint'),
      },
    },
  };
};

export const getValidationSchema = (
  type: LlmProviderType,
  t: TranslateFunction
) => {
  const fields: Record<string, Yup.AnySchema> = {};
  Object.entries(llmProvidersConfig(t)[type]).forEach(([name, o]) => {
    const options: ProviderOptions = { ...llmProvidersDefaults(t)[name], ...o };
    let field: Yup.AnySchema = Yup.string();
    if (!options.optional) {
      field = field.required();
    }
    fields[name] = field;
  });
  return Yup.object({
    type: Yup.string(),
    ...fields,
  });
};

export const getInitialValues = (
  type: LlmProviderType,
  t: TranslateFunction,
  existingData?: LlmProviderModel
) => {
  const result: LlmProviderRequest = {
    type,
    name: '',
    apiUrl: '',
    priority: undefined,
  };
  if (existingData?.type === type) {
    Object.entries(existingData).forEach(([name, value]) => {
      result[name] = value ?? undefined;
    });
  } else {
    Object.entries(llmProvidersConfig(t)[type]).forEach(([name, o]) => {
      const options: ProviderOptions = {
        ...llmProvidersDefaults(t)[name],
        ...o,
      };
      result[name] = options.enum
        ? options.defaultValue
        : options.defaultValue ?? '';
    });
  }
  return result;
};

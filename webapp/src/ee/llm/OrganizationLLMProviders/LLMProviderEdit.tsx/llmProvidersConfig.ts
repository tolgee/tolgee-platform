import { TranslateFunction } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import * as Yup from 'yup';

export type LLMProviderModel = components['schemas']['LLMProviderModel'];
export type LLMProviderType = LLMProviderModel['type'];
type LLMProviderRequest = components['schemas']['LLMProviderRequest'];

export type ProviderOptions = {
  label: string;
  hint?: string;
  enum?: (string | undefined)[];
  optional?: boolean;
  defaultValue?: string;
};

type ProvidersConfig = Record<
  LLMProviderType,
  Partial<Record<keyof LLMProviderModel, Partial<ProviderOptions>>>
>;

export const llmProvidersDefaults = (
  t: TranslateFunction
): Partial<Record<keyof LLMProviderModel, ProviderOptions>> => ({
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
  return {
    OPENAI: {
      name: {},
      apiUrl: {},
      apiKey: {
        optional: true,
      },
      model: {},
      format: {
        hint: t('llm_provider_form_openai_format_hint'),
        optional: true,
        enum: [undefined, 'json_object', 'json_schema'],
        defaultValue: 'json_object',
      },
    },
    OPENAI_AZURE: {
      name: {},
      apiUrl: {},
      apiKey: {},
      deployment: {},
      format: {
        hint: t('llm_provider_form_openai_format_hint'),
        optional: true,
        enum: [undefined, 'json_object'],
      },
    },
  };
};

export const getValidationSchema = (
  type: LLMProviderType,
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
  type: LLMProviderType,
  t: TranslateFunction,
  existingData?: LLMProviderModel
) => {
  const result: LLMProviderRequest = {
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
      result[name] = options.defaultValue ?? '';
    });
  }
  return result;
};

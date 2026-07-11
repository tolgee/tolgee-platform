import { useEffect, useMemo, useState } from 'react';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { EditorError } from 'tg.component/editor/utils/codemirrorError';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useOpenedPrompt } from './useOpenedPrompt';
import { useProject } from 'tg.hooks/useProject';
import { PromptItem } from './PromptLoadMenu';
import { usePreventPageLeave } from 'tg.hooks/usePreventPageLeave';
import { confirmation } from 'tg.hooks/confirmation';
import { T } from '@tolgee/react';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { components } from 'tg.service/apiSchema.generated';
import { DeletableKeyWithTranslationsModelType } from 'tg.views/projects/translations/context/types';
import { usePromptUrlState } from 'tg.views/projects/translations/useUrlPromptState';

type LanguageModel = components['schemas']['LanguageModel'];
type BasicPromptOption = NonNullable<
  components['schemas']['PromptRunDto']['basicPromptOptions']
>[number];

const DEFAULT_BASIC_OPTIONS: BasicPromptOption[] = [
  'KEY_NAME',
  'KEY_DESCRIPTION',
  'KEY_CONTEXT',
  'LANGUAGE_NOTES',
  'PROJECT_DESCRIPTION',
  'TM_SUGGESTIONS',
  'GLOSSARY',
  'SCREENSHOT',
];

type Props = {
  language: LanguageModel;
  keyData: DeletableKeyWithTranslationsModelType;
};

export const usePromptState = ({ language, keyData }: Props) => {
  const { isEnabled } = useEnabledFeatures();
  const project = useProject();

  const { setOpenPrompt, setAiPlayground } = usePromptUrlState();

  const [options, setOptions] = useState<BasicPromptOption[]>(
    DEFAULT_BASIC_OPTIONS
  );

  const [value, setValue] = useState('');

  function getPromptType(prompt: PromptItem) {
    if (!prompt.id) {
      return 'basic';
    }
    return typeof prompt.template === 'string' ? 'advanced' : 'basic';
  }

  const [provider, setProvider] = useState<string>('default');
  const [errors, setErrors] = useState<EditorError[]>();

  const runLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/prompts/run',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/ai-playground-result',
  });

  const openPromptLoadable = useOpenedPrompt({
    projectId: project.id,
  });

  useEffect(() => {
    setErrors(undefined);
  }, [value]);

  const openPromptData = openPromptLoadable.data;

  const [tab, setTab] = useUrlSearchState('playgroundTab', {
    history: false,
    cleanup: true,
    defaultVal: openPromptData ? getPromptType(openPromptData) : undefined,
  });

  const unsavedChanges = useMemo(() => {
    if (openPromptData) {
      if (openPromptData.providerName !== provider) {
        return true;
      }
      const type = getPromptType(openPromptData);
      if (type !== tab) {
        return true;
      }
      if (type === 'advanced' && openPromptData.template !== value) {
        return true;
      }
      if (type === 'basic') {
        const lastOptions = new Set(openPromptData.basicPromptOptions);
        const newOptions = new Set(options);
        return !(
          lastOptions.size === newOptions.size &&
          [...lastOptions].every((x) => newOptions.has(x))
        );
      }

      return false;
    }
    return undefined;
  }, [openPromptData, value, options, provider, tab]);

  const preventLeave = unsavedChanges === true && tab === 'advanced';

  usePreventPageLeave(preventLeave);
  function confirmUnsaved(callback: () => void) {
    if (preventLeave) {
      confirmation({
        title: <T keyName="ai_playground_unsaved_changes_title" />,
        message: <T keyName="ai_playground_unsaved_changes_message" />,
        confirmButtonText: (
          <T keyName="ai_playground_unsaved_changes_continue" />
        ),
        onConfirm() {
          callback();
        },
      });
    } else {
      callback();
    }
  }

  useEffect(() => {
    if (openPromptLoadable.data) {
      updateForm(openPromptLoadable.data, false);
    }
  }, [openPromptLoadable.data]);

  function updateForm(item: PromptItem, overrideTab: boolean) {
    setOpenPrompt(item.id);
    setProvider(item.providerName);
    setValue(item.template || openPromptLoadable.defaultTemplate || '');
    setOptions(item.basicPromptOptions ?? DEFAULT_BASIC_OPTIONS);
    if (!tab || overrideTab) {
      if (item.id === undefined) {
        setTab('basic');
      } else {
        setTab(getPromptType(item));
      }
    }
  }

  const featureEnabled =
    tab === 'advanced' ? isEnabled('AI_PROMPT_CUSTOMIZATION') : true;

  const canBeRenamed =
    typeof openPromptData?.id === 'number' &&
    (isEnabled('AI_PROMPT_CUSTOMIZATION') ||
      getPromptType(openPromptData) === 'basic');

  const providersLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/llm-providers/all-available',
    method: 'get',
    path: {
      organizationId: project.organizationOwner!.id,
    },
  });

  const cellSelected = Boolean(keyData && language);

  const promptVariables = useApiQuery({
    url: '/v2/projects/{projectId}/prompts/get-variables',
    method: 'get',
    path: {
      projectId: project.id,
    },
    query: {
      keyId: keyData?.keyId,
      targetLanguageId: language?.id,
    },
  });

  function handleTestPrompt() {
    if (!cellSelected) {
      return;
    }
    runLoadable.mutate(
      {
        path: {
          projectId: project.id,
        },
        content: {
          'application/json': {
            template: tab === 'advanced' ? value : undefined,
            keyId: keyData.keyId,
            targetLanguageId: language.id,
            provider,
            basicPromptOptions: tab === 'basic' ? options : undefined,
          },
        },
      },
      {
        onError(e) {
          if (e.code === 'llm_template_parsing_error' && e.params) {
            setErrors([
              {
                message: e.params[0],
                line: e.params[1],
                column: e.params[2] + 1,
              },
            ]);
          }
          e.handleError?.();
        },
      }
    );
  }

  const usage = runLoadable.data?.usage;

  return {
    usage,
    openPromptData,
    variables: promptVariables.data,
    providers: providersLoadable.data,
    handleTestPrompt,
    canBeRenamed,
    featureEnabled,
    confirmUnsaved,
    setAiPlayground,
    setOpenPrompt,
    updateForm,
    provider,
    setProvider,
    errors,
    tab,
    setTab,
    value,
    setValue,
    cellSelected,
    options,
    setOptions,
    runData: runLoadable.data,
    runIsLoading: runLoadable.isLoading,
    unsavedChanges,
    isLoading:
      openPromptLoadable.isLoading ||
      promptVariables.isLoading ||
      providersLoadable.isLoading,
  };
};

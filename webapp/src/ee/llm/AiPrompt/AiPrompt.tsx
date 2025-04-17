import { useEffect, useState } from 'react';
import {
  Box,
  Button,
  IconButton,
  MenuItem,
  Portal,
  Select,
  styled,
  TextField,
  Typography,
} from '@mui/material';
import { useQueryClient } from 'react-query';
import { ChevronDown, ChevronUp, Send03 } from '@untitled-ui/icons-react';

import {
  invalidateUrlPrefix,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';
import { EditorHandlebars } from 'tg.component/editor/EditorHandlebars';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { FieldLabel } from 'tg.component/FormField';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { confirmation } from 'tg.hooks/confirmation';

import { AiResult } from './AiResult';
import { PromptLoadMenu } from './PromptLoadMenu';
import { PromptSaveMenu } from './PromptSaveMenu';
import { EditorError } from 'tg.component/editor/utils/codemirrorError';
import { useTranslationsActions } from 'tg.views/projects/translations/context/TranslationsContext';
import { BatchJobModel } from 'tg.views/projects/translations/BatchOperations/types';
import { BatchOperationDialog } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchOperationDialog';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { DeletableKeyWithTranslationsModelType } from 'tg.views/projects/translations/context/types';

type ProjectModel = components['schemas']['ProjectModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledActionsWrapper = styled('div')`
  padding: 8px;
  display: flex;
  gap: 8px;
  justify-content: space-between;
  align-items: end;
  position: fixed;
  bottom: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  z-index: ${({ theme }) => theme.zIndex.appBar};
`;

const StyledTextField = styled(TextField)`
  flex-grow: 1;
  opacity: 0.5;
  &:focus-within {
    opacity: 1;
  }
  &:focus-within .icon-button {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledHeader = styled('div')`
  display: grid;
  margin: 20px;
  gap: 16px;
`;

const StyledTitle = styled('div')`
  font-size: 20px;
  font-weight: 400;
`;

type Props = {
  width: number;
  project: ProjectModel;
  language: LanguageModel;
  keyData: DeletableKeyWithTranslationsModelType;
};

export const AiPrompt: React.FC<Props> = (props) => {
  const projectId = props.project.id;
  const { t } = useTranslate();
  const { getAllIds, setEdit, refetchTranslations } = useTranslationsActions();
  const [lastOpenPrompt, setLastOpenPrompt] = useUrlSearchState(
    `aiPlaygroundLastPrompt-${projectId}`,
    { defaultVal: undefined }
  );
  const [runningOperation, setRunningOperation] = useState<BatchJobModel>();
  const queryClient = useQueryClient();
  const [value, setValue] = useLocalStorageState<string>({
    key: `aiPlaygroundLastValue-${projectId}`,
    initial: 'Hi translate from {{source}} to {{target}}',
  });
  const [expanded, setExpanded] = useLocalStorageState({
    key: 'aiPlaygroundExpanded',
    initial: undefined,
  });
  const [provider, setProvider] = useLocalStorageState<string>({
    key: `aiPlaygroundProvider-${projectId}`,
    initial: 'default',
  });
  const [errors, setErrors] = useState<EditorError[]>();

  const promptLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/prompts/run',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/ai-playground-result',
  });

  useEffect(() => {
    setErrors(undefined);
  }, [value]);

  const lastPrompt = useApiQuery({
    url: '/v2/projects/{projectId}/prompts/{promptId}',
    method: 'get',
    path: {
      projectId,
      promptId: Number(lastOpenPrompt)!,
    },
    options: {
      enabled: Boolean(lastOpenPrompt),
    },
  });

  const providersLoadable = useApiQuery({
    url: '/v2/organizations/{organizationId}/llm-providers/all-available',
    method: 'get',
    path: {
      organizationId: props.project.organizationOwner!.id,
    },
  });

  const mtTranslate = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/ai-playground-translate',
    method: 'post',
  });

  const handleRunBatch = async () => {
    const allIds = await getAllIds();
    confirmation({
      title: `Run for ${allIds.length} keys?`,
      onConfirm() {
        setEdit(undefined);
        mtTranslate
          .mutateAsync({
            content: {
              'application/json': {
                keyIds: allIds,
                targetLanguageIds: [props.language.id],
                llmPrompt: {
                  name: '',
                  template: value,
                  providerName: provider,
                },
              },
            },
            path: {
              projectId: props.project.id,
            },
          })
          .then((data) => {
            setRunningOperation(data);
          });
      },
    });
  };

  const cellSelected = Boolean(props.keyData && props.language);

  const lastPromptName =
    lastPrompt.data?.name ?? t('ai_prompt_default_prompt_name');

  const promptVariables = useApiQuery({
    url: '/v2/projects/{projectId}/prompts/get-variables',
    method: 'get',
    path: {
      projectId: props.project.id,
    },
    query: {
      keyId: props.keyData?.keyId,
      targetLanguageId: props.language?.id,
    },
  });

  function handleTestPrompt() {
    if (!cellSelected) {
      return;
    }
    promptLoadable.mutate(
      {
        path: {
          projectId: props.project.id,
        },
        content: {
          'application/json': {
            template: value,
            keyId: props.keyData.keyId,
            targetLanguageId: props.language.id,
            provider,
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

  const usage = promptLoadable.data?.usage;

  return (
    <Box display="grid">
      <StyledHeader>
        <StyledTitle>{lastPromptName}</StyledTitle>

        <Select
          size="small"
          value={provider}
          onChange={(e) => setProvider(e.target.value)}
        >
          {providersLoadable.data?.items.map((i) => (
            <MenuItem key={i.name} value={i.name}>
              {i.name}
            </MenuItem>
          ))}
        </Select>
      </StyledHeader>
      <Box sx={{ margin: '8px' }}>
        <FieldLabel>Prompt</FieldLabel>
        <EditorWrapper onKeyDown={stopBubble()}>
          <EditorHandlebars
            minHeight={100}
            value={value}
            onChange={setValue}
            unknownVariableMessage={
              cellSelected
                ? 'Unknown variable'
                : 'Select translation to see the value'
            }
            shortcuts={[
              {
                key: 'Mod-Enter',
                run: () => (handleTestPrompt(), true),
              },
            ]}
            availableVariables={promptVariables.data?.data}
            errors={errors}
          />
        </EditorWrapper>
      </Box>

      <Box
        sx={{ margin: '8px', display: 'flex', gap: 1, justifyContent: 'end' }}
      >
        <Button
          size="small"
          color="secondary"
          onClick={handleRunBatch}
          disabled={!cellSelected || promptLoadable.isLoading}
        >
          Batch
        </Button>
        <IconButton
          color="primary"
          onClick={handleTestPrompt}
          disabled={!cellSelected || promptLoadable.isLoading}
          size="medium"
        >
          {promptLoadable.isLoading ? (
            <SpinnerProgress size={22} />
          ) : (
            <Send03 width={22} height={22} />
          )}
        </IconButton>
      </Box>

      <Box sx={{ margin: '8px', display: 'grid' }}>
        <AiResult
          raw={promptLoadable.data?.result}
          json={promptLoadable.data?.parsedJson}
          isPlural={props.keyData?.keyIsPlural}
          locale={props.language?.tag}
        />

        <Typography variant="caption" minHeight={20}>
          {usage?.inputTokens && (
            <>
              {`tokens: ${usage.inputTokens + (usage.outputTokens ?? 0)}`}
              {`, credits: ${promptLoadable.data!.price! / 100}`}
              {typeof usage.cachedTokens === 'number' &&
                `, cached: ${usage.cachedTokens}`}
            </>
          )}
        </Typography>
      </Box>

      {Boolean(expanded) && (
        <Box sx={{ margin: '8px', display: 'grid' }}>
          <FieldLabel>Rendered prompt</FieldLabel>
          <StyledTextField
            multiline
            variant="outlined"
            size="small"
            value={promptLoadable.data?.prompt}
            onChange={(e) => e.preventDefault()}
            data-cy="translations-comments-output"
            InputProps={{
              sx: {
                padding: '8px 4px 8px 12px',
                borderRadius: '8px',
              },
            }}
          />
        </Box>
      )}

      <Box display="flex" justifyContent="center">
        <IconButton
          onClick={() => setExpanded((val) => (val ? undefined : 'true'))}
        >
          {expanded ? <ChevronUp /> : <ChevronDown />}
        </IconButton>
      </Box>

      <Portal>
        <StyledActionsWrapper style={{ width: props.width, right: 22 }}>
          <Box display="flex" gap={1}>
            <PromptLoadMenu
              projectId={props.project.id}
              onSelect={(item) => {
                setLastOpenPrompt(
                  item.id !== undefined ? String(item.id) : undefined
                );
                setProvider(item.providerName);
                setValue(item.template);
              }}
            />
            <PromptSaveMenu
              projectId={props.project.id}
              data={{ template: value, providerName: provider }}
            />
          </Box>
        </StyledActionsWrapper>
      </Portal>

      {runningOperation && (
        <BatchOperationDialog
          operation={runningOperation}
          onClose={() => setRunningOperation(undefined)}
          onFinished={() => {
            refetchTranslations();
            setRunningOperation(undefined);
            invalidateUrlPrefix(queryClient, '');
          }}
        />
      )}
    </Box>
  );
};

import { Alert, Box, IconButton, Link } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { X } from '@untitled-ui/icons-react';

import { EditorHandlebars } from 'tg.component/editor/EditorHandlebars';
import { EditorWrapper } from 'tg.component/editor/EditorWrapper';
import { EditorError } from 'tg.component/editor/utils/codemirrorError';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';

import { Label } from './Label';

type PromptVariable = components['schemas']['PromptVariableDto'];

type Props = {
  value: string;
  onChange: (value: string) => void;
  cellSelected: boolean;
  onRun: () => void;
  availableVariables: PromptVariable[] | undefined;
  errors?: EditorError[];
};

export const TabAdvanced = ({
  value,
  onChange,
  cellSelected,
  onRun,
  availableVariables,
  errors,
}: Props) => {
  const { t } = useTranslate();
  const [hideTip, setHideTip] = useLocalStorageState({
    key: 'aiPlaygroundHidePromptTip',
    initial: undefined,
  });

  return (
    <Box sx={{ margin: '20px 20px' }}>
      <Label
        rightContent={
          <Link href="https://docs.tolgee.io" target="_blank">
            {t('ai_prompt_learn_more')}
          </Link>
        }
      >
        {t('ai_prompt_label')}
      </Label>
      {!hideTip && (
        <Alert
          severity="info"
          icon={false}
          sx={{ mb: 1 }}
          action={
            <IconButton size="small" onClick={() => setHideTip('true')}>
              <X width={20} height={20} />
            </IconButton>
          }
        >
          {t('ai_playground_prompt_tip')}
        </Alert>
      )}
      <EditorWrapper onKeyDown={stopBubble()}>
        <EditorHandlebars
          minHeight={100}
          value={value}
          onChange={onChange}
          unknownVariableMessage={
            cellSelected
              ? t('ai_prompt_editor_unknown_variable')
              : t('ai_prompt_editor_select_translation')
          }
          shortcuts={[
            {
              key: 'Mod-Enter',
              run: () => (onRun(), true),
            },
          ]}
          availableVariables={availableVariables}
          errors={errors}
        />
      </EditorWrapper>
    </Box>
  );
};

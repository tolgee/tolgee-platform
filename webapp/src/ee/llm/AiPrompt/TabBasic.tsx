import {
  Alert,
  Box,
  FormControlLabel,
  IconButton,
  Switch,
  Tooltip,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Edit01, InfoCircle, X } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { Label } from './Label';
import { useState } from 'react';
import { AiProjectDescriptionDialog } from '../AiContextData/AiProjectDescriptionDialog';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

export type BasicPromptOption = NonNullable<
  components['schemas']['PromptRunDto']['basicPromptOptions']
>[number];

type PromptItem = {
  id: BasicPromptOption;
  label: string;
  hint: string;
  onEdit?: (value: boolean) => void;
};

type Props = {
  value: BasicPromptOption[];
  onChange: (value: BasicPromptOption[]) => void;
};

export const TabBasic = ({ value, onChange }: Props) => {
  const { isEnabled } = useEnabledFeatures();
  const { t } = useTranslate();
  const project = useProject();
  const [projectDescription, setProjectDescription] = useState(false);
  const history = useHistory();

  const { satisfiesPermission } = useProjectPermissions();

  const canCustomize =
    satisfiesPermission('prompts.edit') && isEnabled('AI_PROMPT_CUSTOMIZATION');

  const basicPromptItems: PromptItem[] = [
    {
      id: 'KEY_NAME',
      label: t('ai_prompt_item_key_name'),
      hint: t('ai_prompt_item_key_name_hint'),
    },
    {
      id: 'KEY_DESCRIPTION',
      label: t('ai_prompt_item_key_description'),
      hint: t('ai_prompt_item_key_description_hint'),
    },
    {
      id: 'PROJECT_DESCRIPTION',
      label: t('ai_prompt_item_project_description'),
      hint: t('ai_prompt_item_project_description_hint'),
      onEdit: canCustomize ? () => setProjectDescription(true) : undefined,
    },
    {
      id: 'LANGUAGE_NOTES',
      label: t('ai_prompt_item_language_notes'),
      hint: t('ai_prompt_item_language_notes_hint'),
      onEdit: canCustomize
        ? () =>
            history.push(
              LINKS.PROJECT_CONTEXT_DATA.build({
                [PARAMS.PROJECT_ID]: project.id,
              })
            )
        : undefined,
    },
    {
      id: 'TM_SUGGESTIONS',
      label: t('ai_prompt_item_tm_suggestions'),
      hint: t('ai_prompt_item_tm_suggestions_hint'),
    },
    {
      id: 'KEY_CONTEXT',
      label: t('ai_prompt_item_key_context'),
      hint: t('ai_prompt_item_key_context_hint'),
    },
    {
      id: 'GLOSSARY',
      label: t('ai_prompt_item_glossary'),
      hint: t('ai_prompt_item_glossary_hint'),
    },
    {
      id: 'SCREENSHOT',
      label: t('ai_prompt_item_screenshot'),
      hint: t('ai_prompt_item_screenshot_hint'),
    },
  ];

  const theme = useTheme();
  const [hideTip, setHideTip] = useLocalStorageState({
    key: 'aiPlaygroundHideBasicTip',
    initial: undefined,
  });

  const descriptionLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/ai-prompt-customization',
    method: 'get',
    path: { projectId: project.id },
  });

  return (
    <Box sx={{ margin: '20px' }}>
      <Label>{t('ai_prompt_basic_label')}</Label>
      {!hideTip && (
        <Alert
          severity="info"
          icon={false}
          action={
            <IconButton size="small" onClick={() => setHideTip('true')}>
              <X width={20} height={20} />
            </IconButton>
          }
        >
          {t('ai_playground_basic_tip')}
        </Alert>
      )}
      <Box display="grid" sx={{ gap: '20px', pt: 2 }}>
        {basicPromptItems.map(({ id, label, hint, onEdit }) => {
          const checked = value.includes(id);
          return (
            <Box
              key={id}
              display="flex"
              justifyContent="space-between"
              alignItems="center"
              marginRight={1}
            >
              <FormControlLabel
                data-cy="prompt-basic-option"
                data-cy-option={id}
                control={<Switch size="small" />}
                label={label}
                sx={{ marginLeft: 0, gap: 0.5 }}
                checked={checked}
                onChange={() => {
                  if (checked) {
                    onChange(value.filter((i) => i !== id));
                  } else {
                    onChange([...value, id]);
                  }
                }}
              />
              <Box display="flex" alignItems="center" gap={0.5} my={-1}>
                {onEdit && (
                  <IconButton
                    onClick={() => onEdit(true)}
                    data-cy="prompt-basic-option-edit"
                    data-cy-id={id}
                  >
                    <Edit01 width={18} height={18} />
                  </IconButton>
                )}
                <Tooltip title={hint} disableInteractive>
                  <Box display="flex">
                    <InfoCircle
                      width={20}
                      height={20}
                      color={theme.palette.tokens.icon.secondary}
                    />
                  </Box>
                </Tooltip>
              </Box>
            </Box>
          );
        })}
      </Box>
      {projectDescription && descriptionLoadable.isSuccess && (
        <AiProjectDescriptionDialog
          onClose={() => setProjectDescription(false)}
          currentValue={descriptionLoadable.data?.description || ''}
        />
      )}
    </Box>
  );
};

import { ToggleButton, ToggleButtonGroup } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';

export type PromptType = components['schemas']['PromptModel']['type'];

type Props = {
  value: PromptType;
  locked: boolean;
  onChange: (type: PromptType) => void;
};

export const PromptTypeToggle = ({ value, locked, onChange }: Props) => {
  const { t } = useTranslate();

  return (
    <ToggleButtonGroup
      size="small"
      exclusive
      value={value}
      onChange={(_, newType) => {
        if (newType && !locked) {
          onChange(newType);
        }
      }}
      data-cy="ai-prompt-type-switch"
    >
      <ToggleButton
        value="TRANSLATE"
        disabled={locked}
        data-cy="ai-prompt-type-translate"
      >
        {t('ai_prompt_type_translate', 'Translate')}
      </ToggleButton>
      <ToggleButton
        value="REVIEW"
        disabled={locked}
        data-cy="ai-prompt-type-review"
      >
        {t('ai_prompt_type_review', 'Review')}
      </ToggleButton>
    </ToggleButtonGroup>
  );
};

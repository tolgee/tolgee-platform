import { ReactNode } from 'react';
import { styled, ToggleButton, ToggleButtonGroup } from '@mui/material';
import { useTranslate } from '@tolgee/react';

type PlaygroundType = 'translate' | 'review';

const StyledTypeRow = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

type Props = {
  value: PlaygroundType;
  onChange: (value: PlaygroundType) => void;
  trailing?: ReactNode;
};

export const PromptTypeSwitch = ({ value, onChange, trailing }: Props) => {
  const { t } = useTranslate();

  return (
    <StyledTypeRow>
      <ToggleButtonGroup
        size="small"
        exclusive
        value={value}
        onChange={(_, val: PlaygroundType | null) => {
          if (val !== null) {
            onChange(val);
          }
        }}
        sx={{
          '& .MuiToggleButton-root': {
            fontSize: 13,
            px: 2,
            py: 0.5,
          },
        }}
        data-cy="ai-prompt-type-switch"
      >
        <ToggleButton
          value="translate"
          data-cy="ai-prompt-type-switch-translate"
        >
          {t('ai_prompt_type_translate', 'Translate')}
        </ToggleButton>
        <ToggleButton value="review" data-cy="ai-prompt-type-switch-review">
          {t('ai_prompt_type_review', 'Review')}
        </ToggleButton>
      </ToggleButtonGroup>
      {trailing}
    </StyledTypeRow>
  );
};

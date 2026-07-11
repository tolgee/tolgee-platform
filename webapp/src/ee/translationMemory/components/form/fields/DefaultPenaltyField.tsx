import { Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { LabelHint } from 'tg.component/common/LabelHint';

type Props = {
  disabled: boolean;
};

/**
 * Numeric "default penalty" input on the TM create/edit form. Subtracts percentage points
 * from match scores for every project using this TM, unless overridden per-assignment.
 */
export const DefaultPenaltyField = ({ disabled }: Props) => {
  const { t } = useTranslate();
  return (
    <TextField
      name="defaultPenalty"
      label={
        <LabelHint
          title={
            <T
              keyName="translation_memory_settings_default_penalty_hint"
              defaultValue="Lowers match score by this many points. Applied to every project using this TM unless overridden below."
            />
          }
        >
          {t('translation_memory_settings_default_penalty', 'Default penalty')}
        </LabelHint>
      }
      size="small"
      disabled={disabled}
      sx={{ width: 200 }}
      minHeight={false}
      inputProps={{
        inputMode: 'numeric',
        'data-cy': 'tm-settings-default-penalty',
      }}
      InputProps={{
        endAdornment: (
          <Typography variant="body2" color="text.secondary">
            %
          </Typography>
        ),
      }}
    />
  );
};

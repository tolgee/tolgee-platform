import {
  Box,
  Button,
  Checkbox,
  FormControlLabel,
  IconButton,
  MenuItem,
  Select,
  TextField,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';
import { XClose } from '@untitled-ui/icons-react';

import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { FieldLabel } from 'tg.component/FormField';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RuleLanguageOverrides = () => {
  const { t } = useTranslate();
  const languages = useProjectLanguages();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  const selectableLanguages = languages.filter((language) => !language.base);
  const usedIds = values.overrides.map((override) => override.languageId);
  const allLanguagesUsed = selectableLanguages.every((language) =>
    usedIds.includes(language.id)
  );

  function handleAdd() {
    const nextLanguage = selectableLanguages.find(
      (language) => !usedIds.includes(language.id)
    );
    if (!nextLanguage) {
      return;
    }
    setFieldValue('overrides', [
      ...values.overrides,
      { languageId: nextLanguage.id, threshold: 85, never: false },
    ]);
  }

  function handleRemove(index: number) {
    setFieldValue(
      'overrides',
      values.overrides.filter((_, i) => i !== index)
    );
  }

  function handleChange(index: number, field: string, value: unknown) {
    setFieldValue(`overrides[${index}].${field}`, value);
  }

  return (
    <Box display="grid" gap={1}>
      <FieldLabel>
        {t('ai_review_rule_form_overrides_label', 'Language overrides')}
      </FieldLabel>
      <Box display="grid" gap={1}>
        {values.overrides.map((override, index) => (
          <Box
            key={index}
            display="grid"
            gridTemplateColumns="160px 1fr auto auto"
            gap={1}
            alignItems="center"
            data-cy="ai-review-rule-form-override-row"
          >
            <Select
              size="small"
              value={override.languageId}
              onChange={(e) =>
                handleChange(index, 'languageId', e.target.value)
              }
              SelectDisplayProps={
                {
                  'data-cy': 'ai-review-rule-form-override-language',
                } as any
              }
            >
              {selectableLanguages
                .filter(
                  (language) =>
                    language.id === override.languageId ||
                    !usedIds.includes(language.id)
                )
                .map((language) => (
                  <MenuItem key={language.id} value={language.id}>
                    {language.name}
                  </MenuItem>
                ))}
            </Select>
            <Box display="grid">
              <TextField
                size="small"
                type="number"
                value={override.threshold}
                disabled={override.never}
                onChange={(e) =>
                  handleChange(index, 'threshold', e.target.value)
                }
                data-cy="ai-review-rule-form-override-threshold"
              />
              {values.never && !override.never && (
                <Typography
                  variant="caption"
                  color="textSecondary"
                  data-cy="ai-review-rule-form-override-instead"
                >
                  {t(
                    'ai_review_rule_form_override_instead',
                    'instead of Never'
                  )}
                </Typography>
              )}
            </Box>
            <FormControlLabel
              control={
                <Checkbox
                  checked={override.never}
                  onChange={(_, checked) =>
                    handleChange(index, 'never', checked)
                  }
                  data-cy="ai-review-rule-form-override-never"
                />
              }
              label={t('ai_review_rule_form_never_label', 'Never auto-review')}
            />
            <IconButton
              size="small"
              onClick={() => handleRemove(index)}
              data-cy="ai-review-rule-form-override-remove"
            >
              <XClose width={18} height={18} />
            </IconButton>
          </Box>
        ))}
        <Button
          onClick={handleAdd}
          disabled={allLanguagesUsed}
          sx={{ justifySelf: 'start' }}
          data-cy="ai-review-rule-form-override-add"
        >
          {t('ai_review_rule_form_override_add', 'Add override')}
        </Button>
      </Box>
    </Box>
  );
};

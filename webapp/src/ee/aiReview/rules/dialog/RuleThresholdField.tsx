import { useState } from 'react';
import { Box, FormHelperText, Slider, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { FieldLabel } from 'tg.component/FormField';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import { SegmentedControl } from 'tg.component/common/SegmentedControl';

const PRESETS = [70, 85, 95];

const StyledSliderBox = styled(Box)`
  padding: ${({ theme }) => theme.spacing(0, 1)};
`;

export const RuleThresholdField = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();
  const threshold = Number(values.threshold === '' ? 85 : values.threshold);
  const [custom, setCustom] = useState(() => !PRESETS.includes(threshold));

  const selected = custom ? 'custom' : String(threshold);

  function handlePick(value: string) {
    if (value === 'custom') {
      setCustom(true);
      return;
    }
    setCustom(false);
    setFieldValue('threshold', Number(value));
  }

  return (
    <Box display="grid" gap={1}>
      <FieldLabel>
        {t('ai_review_rule_form_threshold_label', 'Threshold')}
      </FieldLabel>
      <SegmentedControl
        dataCy="ai-review-rule-form-threshold-presets"
        value={selected}
        disabled={values.never}
        onChange={handlePick}
        options={[
          {
            value: '70',
            title: t('ai_review_rule_form_threshold_lenient', 'Lenient'),
            subtitle: '70+',
            dataCy: 'ai-review-rule-form-threshold-preset',
          },
          {
            value: '85',
            title: t('ai_review_rule_form_threshold_balanced', 'Balanced'),
            subtitle: '85+',
            dataCy: 'ai-review-rule-form-threshold-preset',
          },
          {
            value: '95',
            title: t('ai_review_rule_form_threshold_strict', 'Strict'),
            subtitle: '95+',
            dataCy: 'ai-review-rule-form-threshold-preset',
          },
          {
            value: 'custom',
            title: t('ai_review_rule_form_threshold_custom', 'Custom'),
            subtitle: custom ? `${threshold}+` : '–',
            dataCy: 'ai-review-rule-form-threshold-preset',
          },
        ]}
      />
      {custom && (
        <StyledSliderBox data-cy="ai-review-rule-form-threshold-slider">
          <Slider
            size="small"
            min={0}
            max={100}
            value={threshold}
            disabled={values.never}
            marks={[
              { value: 0, label: '0' },
              { value: 50, label: '50' },
              { value: 100, label: '100' },
            ]}
            onChange={(_, value) => setFieldValue('threshold', value)}
          />
        </StyledSliderBox>
      )}
      <FormHelperText
        sx={{ marginTop: -0.5 }}
        data-cy="ai-review-rule-form-threshold-hint"
      >
        {t(
          'ai_review_rule_form_threshold_hint',
          'Translations scoring {threshold} or more are marked as reviewed automatically.',
          { threshold: String(threshold) }
        )}
      </FormHelperText>
    </Box>
  );
};

import { Box, Link, Slider, TextField } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { FieldLabel } from 'tg.component/FormField';
import { LabelHint } from 'tg.component/common/LabelHint';
import { DOCS_LINKS } from 'tg.constants/docLinks';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

const MARKS = [0, 25, 50, 75, 100].map((value) => ({
  value,
  label: String(value),
}));

const clamp = (value: number) => Math.min(100, Math.max(0, Math.round(value)));

export const RuleThresholdField = () => {
  const { t } = useTranslate();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();
  const threshold = values.threshold === '' ? 0 : Number(values.threshold);

  return (
    <Box display="grid" gap={0.5}>
      <FieldLabel>
        <LabelHint
          disableInteractive={false}
          title={
            <>
              {t(
                'ai_review_rule_form_threshold_info_scoring',
                'AI scores every translation from 0 to 100. At {threshold}, translations scoring {threshold}+ switch to reviewed.',
                { threshold: String(threshold) }
              )}{' '}
              <Link
                target="_blank"
                rel="noopener noreferrer"
                href={DOCS_LINKS.aiReview}
                color="primary"
                data-cy="ai-review-rule-form-threshold-docs"
              >
                {t('ai_review_rule_form_threshold_docs', 'Learn more')}
              </Link>
            </>
          }
        >
          {t('ai_review_rule_form_threshold_label', 'Threshold')}
        </LabelHint>
      </FieldLabel>
      <Box display="flex" alignItems="flex-start" gap={2} mt={-1.5}>
        <Box flexGrow={1} px={1} data-cy="ai-review-rule-form-threshold-slider">
          <Slider
            size="small"
            min={0}
            max={100}
            step={5}
            marks={MARKS}
            value={threshold}
            valueLabelDisplay="auto"
            sx={(theme) => ({
              '&.MuiSlider-marked': { marginBottom: 1.5 },
              '& .MuiSlider-markLabel': {
                top: 20,
                fontSize: theme.typography.caption.fontSize,
                color: theme.palette.text.secondary,
              },
            })}
            onChange={(_, value) => setFieldValue('threshold', value as number)}
            aria-label={t('ai_review_rule_form_threshold_label', 'Threshold')}
          />
        </Box>
        <TextField
          size="small"
          type="number"
          value={values.threshold}
          onChange={(e) => setFieldValue('threshold', e.target.value)}
          onBlur={() =>
            setFieldValue('threshold', clamp(Number(values.threshold) || 0))
          }
          inputProps={{
            min: 0,
            max: 100,
            step: 1,
            'aria-label': t(
              'ai_review_rule_form_threshold_custom_label',
              'Minimum score'
            ),
            'data-cy': 'ai-review-rule-form-threshold-input',
          }}
          sx={{
            width: 56,
            flexShrink: 0,
            // Centred on the slider track, not on the track and its labels.
            mt: '1px',
            // The theme makes outlined inputs transparent; this one sits on the grey panel.
            '& .MuiInputBase-root.MuiOutlinedInput-root': {
              backgroundColor: (theme) =>
                theme.palette.tokens.background['paper-1'],
            },
            '& .MuiInputBase-input': {
              py: '4px',
              px: 1,
              textAlign: 'center',
              fontSize: 14,
            },
          }}
        />
      </Box>
    </Box>
  );
};

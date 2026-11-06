import { useState } from 'react';
import { Box, Checkbox, FormControlLabel, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { useProject } from 'tg.hooks/useProject';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import {
  hasThresholdOverride,
  RuleFormValues,
  ruleNeverFullyApplies,
} from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import { RuleLanguageOverrides } from 'tg.ee.module/aiReview/rules/dialog/RuleLanguageOverrides';
import { RulePromptSelect } from 'tg.ee.module/aiReview/rules/dialog/RulePromptSelect';
import { RuleSummarySection } from 'tg.ee.module/aiReview/rules/dialog/RuleSummarySection';

export const RuleAdvancedSection = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();
  const [expanded, setExpanded] = useState(
    values.overrides.length > 0 || values.promptId !== ''
  );
  const fullNever = ruleNeverFullyApplies(values);
  const overrideHintVisible = values.never && hasThresholdOverride(values);

  const promptsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: { projectId: project.id },
    query: { type: 'REVIEW', size: 1000 },
  });
  const prompts = promptsLoadable.data?._embedded?.prompts ?? [];
  const promptName =
    values.promptId === ''
      ? t('ai_review_rule_form_prompt_default', 'Default review prompt')
      : prompts.find((prompt) => prompt.id === values.promptId)?.name ?? '';

  const overridesPart = values.overrides.length
    ? t(
        'ai_review_rule_summary_overrides',
        '{count, plural, one {# language override} other {# language overrides}}',
        { count: values.overrides.length }
      )
    : t('ai_review_rule_summary_no_overrides', 'No language overrides');
  const qaPart = !project.useQaChecks
    ? undefined
    : values.skipOpenQaIssues
    ? t('ai_review_rule_summary_skips_qa', 'Skips open QA issues')
    : t(
        'ai_review_rule_summary_reviews_qa',
        'Reviews even with open QA issues'
      );

  return (
    <RuleSummarySection
      label={t('ai_review_rule_form_advanced_title', 'Advanced')}
      primary={promptName}
      secondaryLines={[[overridesPart, qaPart].filter(Boolean).join(' • ')]}
      expanded={expanded}
      onToggle={() => setExpanded(!expanded)}
      dataCy="ai-review-rule-form-advanced-toggle"
    >
      <Box display="grid" gap={2.5}>
        <RuleLanguageOverrides />
        <RulePromptSelect />
        {overrideHintVisible && (
          <Typography
            variant="caption"
            color="textSecondary"
            data-cy="ai-review-rule-form-override-hint"
          >
            {t(
              'ai_review_rule_form_override_only_hint',
              'Applies only to languages with an override'
            )}
          </Typography>
        )}
        {project.useQaChecks && (
          <FormControlLabel
            sx={{ alignItems: 'flex-start', marginLeft: 0, gap: 1 }}
            control={
              <Checkbox
                sx={{ padding: 0, marginTop: '1px' }}
                checked={values.skipOpenQaIssues}
                disabled={fullNever}
                onChange={(_, checked) =>
                  setFieldValue('skipOpenQaIssues', checked)
                }
                data-cy="ai-review-rule-form-skip-qa"
              />
            }
            label={
              <Box>
                <Typography>
                  {t(
                    'ai_review_rule_form_skip_qa_label',
                    "Don't auto-review translations with open QA issues"
                  )}
                </Typography>
                <Typography variant="body2" color="textSecondary">
                  {t(
                    'ai_review_rule_form_skip_qa_hint',
                    'They stay unreviewed until the QA issue is resolved.'
                  )}
                </Typography>
              </Box>
            }
          />
        )}
      </Box>
    </RuleSummarySection>
  );
};

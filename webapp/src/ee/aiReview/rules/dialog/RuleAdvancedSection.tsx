import { useState } from 'react';
import { Box, Collapse, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import {
  hasThresholdOverride,
  RuleFormValues,
} from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import { RuleLanguageOverrides } from 'tg.ee.module/aiReview/rules/dialog/RuleLanguageOverrides';
import { RuleSectionToggle } from 'tg.ee.module/aiReview/rules/dialog/RuleSectionToggle';
import { RulePromptSelect } from 'tg.ee.module/aiReview/rules/dialog/RulePromptSelect';

export const RuleAdvancedSection = () => {
  const { t } = useTranslate();
  const { values } = useFormikContext<RuleFormValues>();
  const [expanded, setExpanded] = useState(
    values.overrides.length > 0 || values.promptId !== ''
  );
  const overrideHintVisible = values.never && hasThresholdOverride(values);

  return (
    <Box display="grid" gap={1}>
      <RuleSectionToggle
        title={t('ai_review_rule_form_advanced_title', 'Advanced')}
        expanded={expanded}
        onToggle={() => setExpanded(!expanded)}
        dataCy="ai-review-rule-form-advanced-toggle"
      />
      <Collapse in={expanded}>
        <Box display="grid" gap={2.5} paddingTop={0.5}>
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
        </Box>
      </Collapse>
    </Box>
  );
};

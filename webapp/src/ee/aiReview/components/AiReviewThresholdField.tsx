import { useRef, useState } from 'react';
import { Box, Button, Collapse, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { ChevronDown, ChevronRight } from '@untitled-ui/icons-react';

import { TextField } from 'tg.component/common/TextField';
import { components } from 'tg.service/apiSchema.generated';
import { AiReviewLanguageThreshold } from 'tg.ee.module/aiReview/utils/types';
import { AiReviewLanguageOverrides } from 'tg.ee.module/aiReview/components/AiReviewLanguageOverrides';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledSection = styled('div')`
  display: grid;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledThresholdRow = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
`;

const StyledHint = styled(Typography)`
  font-size: 13px;
`;

const StyledOverridesToggle = styled(Button)`
  justify-self: start;
  padding: 2px ${({ theme }) => theme.spacing(0.5)};
  margin-left: -4px;
  color: ${({ theme }) => theme.palette.text.primary};
  font-weight: 500;
  font-size: 14px;
  text-transform: none;
`;

export function parseThreshold(value: string): number | undefined {
  const parsed = parseInt(value, 10);
  if (Number.isNaN(parsed) || parsed < 0 || parsed > 100) {
    return undefined;
  }
  return parsed;
}

export function getEffectiveOverrides(
  overrides: AiReviewLanguageThreshold[],
  languages: LanguageModel[]
) {
  return overrides.filter(
    (o) =>
      languages.some((l) => l.id === o.languageId) &&
      (o.never || o.threshold !== undefined)
  );
}

type Props = {
  threshold: string;
  initialThreshold: number;
  onThresholdChange: (value: string) => void;
  overrides: AiReviewLanguageThreshold[];
  onOverridesChange: (value: AiReviewLanguageThreshold[]) => void;
  overrideLanguages: LanguageModel[];
};

export const AiReviewThresholdField = ({
  threshold,
  initialThreshold,
  onThresholdChange,
  overrides,
  onOverridesChange,
  overrideLanguages,
}: Props) => {
  const { t } = useTranslate();
  const [overridesOpen, setOverridesOpen] = useState(false);

  const parsedThreshold = parseThreshold(threshold);
  const thresholdValid = parsedThreshold !== undefined;
  const lastValidThreshold = useRef(initialThreshold);
  if (parsedThreshold !== undefined) {
    lastValidThreshold.current = parsedThreshold;
  }
  const displayedThreshold = lastValidThreshold.current;
  const effectiveOverridesCount = getEffectiveOverrides(
    overrides,
    overrideLanguages
  ).length;

  return (
    <StyledSection>
      <StyledThresholdRow>
        <TextField
          label={t('ai_review_rule_threshold_label', 'Threshold')}
          type="number"
          value={threshold}
          onChange={(e) => onThresholdChange(e.target.value)}
          inputProps={{ min: 0, max: 100 }}
          error={!thresholdValid}
          minHeight={false}
          sx={{ width: 120 }}
          data-cy="ai-review-rule-threshold-field"
        />
      </StyledThresholdRow>
      <StyledHint
        data-cy="ai-review-rule-threshold-hint"
        color={thresholdValid ? 'text.secondary' : 'error'}
      >
        {thresholdValid
          ? t(
              'ai_review_rule_threshold_hint',
              'Translations scoring {threshold} or more are marked as reviewed automatically.',
              { threshold: displayedThreshold }
            )
          : t(
              'ai_review_rule_threshold_invalid',
              'Enter a number from 0 to 100.'
            )}
      </StyledHint>
      <StyledOverridesToggle
        size="small"
        variant="text"
        onClick={() => setOverridesOpen((v) => !v)}
        startIcon={
          overridesOpen ? (
            <ChevronDown width={18} height={18} />
          ) : (
            <ChevronRight width={18} height={18} />
          )
        }
        data-cy="ai-review-rule-language-overrides-toggle"
      >
        {effectiveOverridesCount
          ? t(
              'ai_review_rule_language_overrides_count',
              'Language overrides ({count})',
              { count: effectiveOverridesCount }
            )
          : t('ai_review_rule_language_overrides', 'Language overrides')}
      </StyledOverridesToggle>
      <Collapse in={overridesOpen} unmountOnExit>
        <Box pt={1} pb={1}>
          <AiReviewLanguageOverrides
            languages={overrideLanguages}
            value={overrides}
            defaultThreshold={displayedThreshold}
            onChange={onOverridesChange}
          />
        </Box>
      </Collapse>
    </StyledSection>
  );
};

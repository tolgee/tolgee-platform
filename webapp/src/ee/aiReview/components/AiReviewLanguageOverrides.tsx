import {
  Box,
  MenuItem,
  Select,
  styled,
  TextField,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { LanguageItem } from 'tg.component/languages/LanguageItem';
import { components } from 'tg.service/apiSchema.generated';
import { AiReviewLanguageThreshold } from 'tg.ee.module/aiReview/utils/types';

type LanguageModel = components['schemas']['LanguageModel'];

type OverrideMode = 'default' | 'custom' | 'never';

const StyledList = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1, 1.5)};
`;

type Props = {
  languages: LanguageModel[];
  value: AiReviewLanguageThreshold[];
  defaultThreshold: number;
  onChange: (value: AiReviewLanguageThreshold[]) => void;
};

export const AiReviewLanguageOverrides = ({
  languages,
  value,
  defaultThreshold,
  onChange,
}: Props) => {
  const { t } = useTranslate();

  function getMode(languageId: number): OverrideMode {
    const override = value.find((o) => o.languageId === languageId);
    if (!override) {
      return 'default';
    }
    return override.never ? 'never' : 'custom';
  }

  function setOverride(
    languageId: number,
    override: AiReviewLanguageThreshold | undefined
  ) {
    const rest = value.filter((o) => o.languageId !== languageId);
    onChange(override ? [...rest, override] : rest);
  }

  function handleModeChange(languageId: number, mode: OverrideMode) {
    switch (mode) {
      case 'default':
        return setOverride(languageId, undefined);
      case 'never':
        return setOverride(languageId, { languageId, never: true });
      case 'custom':
        return setOverride(languageId, {
          languageId,
          never: false,
          threshold: defaultThreshold,
        });
    }
  }

  if (!languages.length) {
    return (
      <Typography variant="body2" color="text.secondary">
        {t(
          'ai_review_rule_overrides_no_languages',
          'No languages to override.'
        )}
      </Typography>
    );
  }

  return (
    <StyledList>
      {languages.map((language) => {
        const mode = getMode(language.id);
        const override = value.find((o) => o.languageId === language.id);
        return (
          <Box
            key={language.id}
            display="contents"
            data-cy="ai-review-rule-language-override"
            data-cy-language={language.tag}
          >
            <Box sx={{ minWidth: 0, fontSize: 14 }}>
              <LanguageItem language={language} />
            </Box>
            <Select
              size="small"
              value={mode}
              onChange={(e) =>
                handleModeChange(language.id, e.target.value as OverrideMode)
              }
              sx={{ minWidth: 150 }}
              data-cy="ai-review-rule-language-override-mode"
            >
              <MenuItem
                value="default"
                data-cy="ai-review-rule-language-override-option"
                data-cy-override-mode="default"
              >
                {t('ai_review_rule_override_default', 'Default ({threshold})', {
                  threshold: defaultThreshold,
                })}
              </MenuItem>
              <MenuItem
                value="custom"
                data-cy="ai-review-rule-language-override-option"
                data-cy-override-mode="custom"
              >
                {t('ai_review_rule_override_custom', 'Custom threshold')}
              </MenuItem>
              <MenuItem
                value="never"
                data-cy="ai-review-rule-language-override-option"
                data-cy-override-mode="never"
              >
                {t('ai_review_rule_override_never', 'Never')}
              </MenuItem>
            </Select>
            <Box width={80}>
              {mode === 'custom' && (
                <TextField
                  size="small"
                  type="number"
                  value={override?.threshold ?? ''}
                  inputProps={{ min: 0, max: 100 }}
                  onChange={(e) => {
                    const parsed = parseInt(e.target.value, 10);
                    setOverride(language.id, {
                      languageId: language.id,
                      never: false,
                      threshold: Number.isNaN(parsed)
                        ? undefined
                        : Math.max(0, Math.min(100, parsed)),
                    });
                  }}
                  data-cy="ai-review-rule-language-override-threshold"
                />
              )}
            </Box>
          </Box>
        );
      })}
    </StyledList>
  );
};

import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { StyledInputLabel } from 'tg.component/common/TextField';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationFilters } from 'tg.views/projects/translations/TranslationFilters/TranslationFilters';
import {
  FiltersInternal,
  FiltersType,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import { useTranslationFilters } from 'tg.views/projects/translations/TranslationFilters/useTranslationFilters';

type LanguageModel = components['schemas']['LanguageModel'];

export type AiReviewScopeValue = {
  filters: FiltersInternal;
  languageTags: string[];
};

const StyledFields = styled('div')`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${({ theme }) => theme.spacing(1.5)};
  align-items: start;

  & .field {
    display: grid;
    gap: ${({ theme }) => theme.spacing(0.5)};
    min-width: 0;
  }

  & .field > div,
  & .field .MuiInputBase-root {
    width: 100%;
  }
`;

type Props = {
  projectId: number;
  languages: LanguageModel[];
  value: AiReviewScopeValue;
  onChange: (value: AiReviewScopeValue) => void;
};

export const AiReviewScopeField = ({
  projectId,
  languages,
  value,
  onChange,
}: Props) => {
  const { t } = useTranslate();

  const { filtersQuery: _, ...actions } = useTranslationFilters({
    filters: value.filters,
    setFilters: (filters) => onChange({ ...value, filters }),
  });

  return (
    <StyledFields>
      <Box className="field" data-cy="ai-review-rule-scope-keys">
        <StyledInputLabel>
          {t('ai_review_rule_scope_keys_label', 'Keys')}
        </StyledInputLabel>
        <TranslationFilters
          value={value.filters as FiltersType}
          actions={actions}
          selectedLanguages={[]}
          projectId={projectId}
          filterOptions={{ scopeOnly: true, keyRelatedOnly: true }}
          placeholder={t('ai_review_rule_scope_all_keys', 'All keys')}
        />
      </Box>
      <Box className="field" data-cy="ai-review-rule-scope-languages">
        <StyledInputLabel>
          {t('ai_review_rule_scope_languages_label', 'Languages')}
        </StyledInputLabel>
        <LanguagesSelect
          languages={languages}
          value={value.languageTags}
          onChange={(languageTags) => onChange({ ...value, languageTags })}
          context="ai-review-rule"
          enableEmpty
          placeholder={t('ai_review_rule_scope_all_languages', 'All languages')}
        />
      </Box>
    </StyledFields>
  );
};

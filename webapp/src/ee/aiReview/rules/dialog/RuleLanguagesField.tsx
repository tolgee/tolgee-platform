import { Box } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { FieldLabel } from 'tg.component/FormField';
import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RuleLanguagesField = () => {
  const { t } = useTranslate();
  const languages = useProjectLanguages();
  const nonBaseLanguages = languages.filter((language) => !language.base);
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  return (
    <Box
      display="grid"
      gap={0.5}
      justifyItems="start"
      data-cy="ai-review-rule-form-languages"
    >
      <FieldLabel>
        {t('ai_review_rule_form_languages_label', 'Languages')}
      </FieldLabel>
      <LanguagesSelect
        enableEmpty
        context="ai-review-rule"
        languages={nonBaseLanguages}
        value={nonBaseLanguages
          .filter((language) => values.languageIds.includes(language.id))
          .map((language) => language.tag)}
        onChange={(tags) =>
          setFieldValue(
            'languageIds',
            nonBaseLanguages
              .filter((language) => tags.includes(language.tag))
              .map((language) => language.id)
          )
        }
        placeholder={t('ai_review_rule_form_languages_all', 'All languages')}
      />
    </Box>
  );
};

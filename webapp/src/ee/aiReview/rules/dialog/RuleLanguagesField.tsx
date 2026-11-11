import {
  Box,
  Checkbox,
  Divider,
  ListItemText,
  MenuItem,
  Select,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useFormikContext } from 'formik';

import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { FieldLabel } from 'tg.component/FormField';
import { useLanguagesContent } from 'tg.component/common/form/LanguagesSelect/useLanguagesContent';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';

export const RuleLanguagesField = () => {
  const { t } = useTranslate();
  const languages = useProjectLanguages();
  const nonBaseLanguages = languages.filter((language) => !language.base);
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();

  const allTags = nonBaseLanguages.map((language) => language.tag);
  const isAll = !values.noLanguages && values.languageIds.length === 0;
  const selectedTags = values.noLanguages
    ? []
    : isAll
    ? allTags
    : nonBaseLanguages
        .filter((language) => values.languageIds.includes(language.id))
        .map((language) => language.tag);

  function setSelection(tags: string[]) {
    const everything = allTags.every((tag) => tags.includes(tag));
    setFieldValue('noLanguages', tags.length === 0);
    setFieldValue(
      'languageIds',
      everything
        ? []
        : nonBaseLanguages
            .filter((language) => tags.includes(language.tag))
            .map((language) => language.id)
    );
  }

  const languageItems = useLanguagesContent({
    languages: nonBaseLanguages,
    value: selectedTags,
    onChange: setSelection,
    disabledLanguages: undefined,
    enableEmpty: true,
    context: 'ai-review-rule',
  });

  return (
    // minWidth 0 keeps a long selection from widening the column.
    <Box
      display="grid"
      gap={0.5}
      minWidth={0}
      data-cy="ai-review-rule-form-languages"
    >
      <FieldLabel>
        {t('ai_review_rule_form_languages_label', 'Languages')}
      </FieldLabel>
      <Select
        multiple
        size="small"
        displayEmpty
        sx={{ width: '100%', minWidth: 0 }}
        value={selectedTags}
        renderValue={() => (
          <Typography
            noWrap
            color={values.noLanguages ? 'text.secondary' : 'text.primary'}
          >
            {values.noLanguages
              ? t('ai_review_rule_form_languages_none', 'Select languages')
              : isAll
              ? t('ai_review_rule_form_languages_all', 'All languages')
              : selectedTags.join(', ')}
          </Typography>
        )}
        MenuProps={{ PaperProps: { sx: { minWidth: 200, maxWidth: 280 } } }}
        data-cy="ai-review-rule-form-languages-select"
      >
        <MenuItem
          onClick={() => setSelection(isAll ? [] : allTags)}
          data-cy="ai-review-rule-form-languages-all"
        >
          <Checkbox
            size="small"
            checked={isAll}
            indeterminate={!isAll && selectedTags.length > 0}
          />
          <ListItemText
            primary={t('ai_review_rule_form_languages_all', 'All languages')}
          />
        </MenuItem>
        <Divider />
        {languageItems}
      </Select>
    </Box>
  );
};

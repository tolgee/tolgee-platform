import { Field } from 'formik';
import { Checkbox, ListItemText, MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { Select } from 'tg.component/common/Select';
import { components } from 'tg.service/apiSchema.generated';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  languages: LanguageModel[] | undefined;
  className?: string;
};

export const LanguageSelector: React.FC<React.PropsWithChildren<Props>> = ({
  languages,
  className,
}) => {
  const { t } = useTranslate();

  return (
    <Field name="languages">
      {({ field, meta }) => {
        return (
          <div className={className}>
            <Select
              {...field}
              label={t('export_translations_languages_label')}
              error={meta.error}
              minHeight={false}
              shrinkable
              data-cy="export-language-selector"
              renderValue={(values: string[]) => values.join(', ')}
              multiple
            >
              {languages?.map((lang) => (
                <MenuItem
                  key={lang.id}
                  value={lang.tag}
                  data-cy="export-language-selector-item"
                  data-cy-language={lang.tag}
                >
                  <Checkbox checked={field.value.includes(lang.tag)} />
                  <ListItemText primary={lang.name} />
                </MenuItem>
              ))}
            </Select>
          </div>
        );
      }}
    </Field>
  );
};

import React, { FC, ReactNode, useEffect } from 'react';
import { MenuItem } from '@mui/material';
import { useFormikContext } from 'formik';

import { Select } from 'tg.component/common/form/fields/Select';
import { LanguageValue } from 'tg.component/languages/LanguageValue';
import { components } from 'tg.service/apiSchema.generated';

export const BaseLanguageSelect: FC<{
  languages: Partial<components['schemas']['LanguageModel']>[];
  label?: ReactNode;
  name: string;
  valueKey?: keyof components['schemas']['LanguageModel'];
}> = (props) => {
  const availableLanguages = props.languages.filter((l) => !!l);
  const context = useFormikContext();
  const value = context.getFieldProps(props.name).value;
  const valueKey = props.valueKey || 'id';

  useEffect(() => {
    if (value) {
      if (availableLanguages.findIndex((l) => l![valueKey] === value) === -1) {
        context.setFieldValue(props.name, '');
      }
    } else {
      const firstLanguage = availableLanguages[0];
      if (firstLanguage) {
        context.setFieldValue(props.name, firstLanguage[valueKey]);
      }
    }
  }, [value, availableLanguages]);

  return (
    <Select
      data-cy="base-language-select"
      variant="standard"
      name={props.name}
      label={props.label}
      renderValue={(v) => {
        const language = availableLanguages.find(
          (lang) => lang![valueKey] === v
        );
        return language && <LanguageValue language={language} />;
      }}
    >
      {availableLanguages.map((l, index) => (
        <MenuItem key={index} value={l![valueKey] as string | number}>
          <LanguageValue language={l!} />
        </MenuItem>
      ))}
    </Select>
  );
};

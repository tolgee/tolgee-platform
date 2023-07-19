import React from 'react';
import { Checkbox, MenuItem, Select, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Field, getIn } from 'formik';

import { components } from 'tg.service/apiSchema.generated';
import { LanguageItem } from '../LanguageItem';
import { useProviderImg } from 'tg.views/projects/translations/TranslationTools/useProviderImg';
import { TABLE_CENTERED, TABLE_FIRST_CELL } from '../tableStyles';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledDefaultCellSelector = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: ${({ theme }) => theme.spacing(1, 2, 1, 1)};
  min-width: 150px;
`;

const StyledProviderImg = styled('img')`
  width: 13px;
  margin-right: ${({ theme }) => theme.spacing(1)};
`;

type Props = {
  lang: LanguageModel | null;
  providers: string[];
  disabled?: boolean;
};

export const LanguageRow: React.FC<Props> = ({ lang, providers, disabled }) => {
  const { t } = useTranslate();

  const getProviderImg = useProviderImg();

  const getProviderName = (provider) => {
    switch (provider) {
      case 'default':
        return t('project_languages_default_provider_short', 'Default');
      case 'none':
        return t('project_languages_primary_none', 'None');
      case 'GOOGLE':
        return 'Google';
      case 'DEEPL':
        return 'DeepL';
      case 'AZURE':
        return 'Azure Cognitive';
      case 'BAIDU':
        return 'Baidu';
      case 'TOLGEE':
        return 'Tolgee';
      default:
        return provider;
    }
  };

  const languagePath = lang ? `languages.${lang.tag}` : 'default';

  return (
    <>
      <div className={TABLE_FIRST_CELL}>
        {lang ? (
          <LanguageItem language={lang} />
        ) : (
          t('project_languages_default_providers', 'Default providers')
        )}
      </div>
      {providers.map((provider) => (
        <div key={provider} className={TABLE_CENTERED}>
          <Field name={`${languagePath}.enabled`}>
            {({ field, form }) => {
              const isDefault =
                getIn(form.values, `${languagePath}.primary`) === 'default';
              return (
                <Checkbox
                  checked={field.value?.includes(provider)}
                  disabled={isDefault || disabled}
                  onChange={(_, checked) => {
                    form.setFieldValue(
                      `${languagePath}.enabled`,
                      checked
                        ? [...field.value, provider]
                        : field.value.filter((i) => i !== provider)
                    );
                  }}
                />
              );
            }}
          </Field>
        </div>
      ))}
      <StyledDefaultCellSelector>
        <Field name={`${languagePath}.primary`}>
          {({ field }) => {
            const langProviders = lang ? ['default', ...providers] : providers;

            return (
              <Select
                size="small"
                variant="outlined"
                name={field.name}
                value={field.value}
                onChange={field.onChange}
                disabled={disabled}
              >
                {[...langProviders, 'none'].map((provider) => {
                  const img = getProviderImg(provider, false);
                  return (
                    provider && (
                      <MenuItem key={provider} value={provider}>
                        {img && <StyledProviderImg src={img} />}
                        {getProviderName(provider)}
                      </MenuItem>
                    )
                  );
                })}
              </Select>
            );
          }}
        </Field>
      </StyledDefaultCellSelector>
    </>
  );
};

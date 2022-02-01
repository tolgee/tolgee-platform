import React from 'react';
import { Checkbox, makeStyles, MenuItem, Select } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';
import { Field, getIn } from 'formik';

import { components } from 'tg.service/apiSchema.generated';
import { LanguageItem } from '../LanguageItem';
import { getProviderImg } from 'tg.views/projects/translations/TranslationTools/getProviderImg';
import { useTableStyles } from '../tableStyles';

type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles((theme) => ({
  defaultCellSelector: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'stretch',
    padding: theme.spacing(1, 2, 1, 1),
    minWidth: 150,
  },
  centered: {
    display: 'flex',
    justifySelf: 'stretch',
    justifyContent: 'center',
  },
  providerImg: {
    width: 13,
    marginRight: theme.spacing(1),
  },
}));

type Props = {
  lang: LanguageModel | null;
  providers: string[];
  disabled?: boolean;
};

export const LanguageRow: React.FC<Props> = ({ lang, providers, disabled }) => {
  const classes = useStyles();
  const tableClasses = useTableStyles();
  const t = useTranslate();

  const getProviderName = (provider) => {
    switch (provider) {
      case 'default':
        return t({
          key: 'project_languages_default_provider_short',
          defaultValue: 'Default',
        });
      case 'none':
        return t({
          key: 'project_languages_primary_none',
          defaultValue: 'None',
        });
      case 'GOOGLE':
        return 'Google';
      default:
        return provider;
    }
  };

  const languagePath = lang ? `languages.${lang.tag}` : 'default';

  return (
    <>
      <div className={tableClasses.firstCell}>
        {lang ? (
          <LanguageItem language={lang} />
        ) : (
          t({
            key: 'project_languages_default_providers',
            defaultValue: 'Default providers',
          })
        )}
      </div>
      {providers.map((provider) => (
        <div key={provider} className={classes.centered}>
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
      <div className={classes.defaultCellSelector}>
        <Field name={`${languagePath}.primary`}>
          {({ field }) => {
            const langProviders = lang ? ['default', ...providers] : providers;

            return (
              <Select
                margin="dense"
                variant="outlined"
                name={field.name}
                value={field.value}
                onChange={field.onChange}
                disabled={disabled}
              >
                {[...langProviders, 'none'].map((provider) => {
                  const img = getProviderImg(provider);
                  return (
                    provider && (
                      <MenuItem key={provider} value={provider}>
                        {img && (
                          <img src={img} className={classes.providerImg} />
                        )}
                        {getProviderName(provider)}
                      </MenuItem>
                    )
                  );
                })}
              </Select>
            );
          }}
        </Field>
      </div>
    </>
  );
};

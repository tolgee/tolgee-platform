import { useEffect, useRef, useState } from 'react';
import { Box, makeStyles, Typography } from '@material-ui/core';
import { Formik, FormikProps } from 'formik';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useTableStyles } from '../tableStyles';
import { ExpandLess, ExpandMore } from '@material-ui/icons';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useMachineTranslationSettings } from './useMachineTranslationSettings';
import { SettingsForm } from './SettingsForm';

type MachineTranslationLanguagePropsDto =
  components['schemas']['MachineTranslationLanguagePropsDto'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
  },
  toggle: {
    display: 'flex',
    justifyContent: 'center',
    gridColumn: '1 / -1',
    cursor: 'pointer',
    background: theme.palette.extraLightBackground.main,
    transition: 'background 0.1s ease-in-out',
    '&:active, &:hover': {
      background: theme.palette.lightBackground.main,
    },
  },
  hint: {
    color: theme.palette.text.secondary,
  },
  loading: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
  },
}));

export const MachineTranslation = () => {
  const classes = useStyles();
  const tableClasses = useTableStyles();
  const formRef = useRef<FormikProps<any>>();
  const [expanded, setExpanded] = useState(false);
  const t = useTranslate();
  const [formInstance, setFormInstance] = useState(0);

  const {
    settings,
    languages,
    creditBalance,
    updateSettings,
    providers,
    baseSetting,
    applyUpdate,
  } = useMachineTranslationSettings({
    // completely reset form (by creating new instance)
    onReset: () => setFormInstance((i) => i + 1),
  });

  const isFetching =
    settings.isFetching || languages.isFetching || creditBalance.isFetching;

  useGlobalLoading(isFetching);

  const isUpdating = updateSettings.isLoading;

  const formatLangSettings = (
    lang: string | null,
    enabled: string[],
    primary: string | null | undefined
  ): MachineTranslationLanguagePropsDto | null => {
    const targetLanguageId = lang
      ? languages.data?._embedded?.languages?.find((l) => l.tag === lang)?.id
      : null;

    if (primary === 'default') {
      return null;
    }

    return {
      targetLanguageId: targetLanguageId as any,
      primaryService: (primary === 'none' ? null : primary) as any,
      enabledServices: enabled as any,
    };
  };

  const langDefaults: Record<
    string,
    { enabled: string[]; primary?: string | null }
  > = {};

  languages.data?._embedded?.languages?.forEach((lang) => {
    const config = settings.data?._embedded?.languageConfigs?.find(
      (langSettings) => langSettings.targetLanguageId === lang.id
    );

    langDefaults[lang.tag] = {
      enabled: config?.enabledServices || baseSetting?.enabledServices || [],
      primary: config ? config.primaryService || 'none' : 'default',
    };
  });

  const initialValues = {
    default: {
      enabled: baseSetting?.enabledServices,
      primary: baseSetting?.primaryService || 'none',
    },
    languages: langDefaults,
  };

  const submit = (values: typeof initialValues) => {
    const languageSettings = Object.entries(values.languages).map(
      ([lang, value]) => formatLangSettings(lang, value.enabled, value.primary)
    );

    languageSettings.push(
      formatLangSettings(null, values.default.enabled!, values.default.primary)
    );

    applyUpdate({
      settings: languageSettings.filter(
        Boolean
      ) as MachineTranslationLanguagePropsDto[],
    });
  };

  const gridTemplateColumns = `1fr ${providers
    .map(() => 'auto')
    .join(' ')} auto`;

  useEffect(() => {
    formRef.current?.resetForm();
  }, [settings.data]);

  useEffect(() => {
    if (Number(settings.data?._embedded?.languageConfigs?.length) > 1) {
      setExpanded(true);
    }
  }, [settings.data]);

  return (
    <>
      {settings.data && languages.data && creditBalance.data && (
        <div className={classes.container}>
          <div className={tableClasses.table} style={{ gridTemplateColumns }}>
            <Formik
              key={formInstance}
              initialValues={initialValues}
              enableReinitialize={true}
              onSubmit={() => {}}
              validate={submit}
              validateOnChange
            >
              {(form) => {
                formRef.current = form;
                return (
                  <SettingsForm
                    providers={providers}
                    expanded={expanded}
                    languages={languages.data}
                  />
                );
              }}
            </Formik>
            <div
              className={classes.toggle}
              role="button"
              onClick={() => setExpanded((expanded) => !expanded)}
            >
              {expanded ? <ExpandLess /> : <ExpandMore />}
            </div>
            <div className={classes.loading}>
              <SmoothProgress loading={isUpdating} />
            </div>
          </div>
          <Box my={1} display="flex" flexDirection="column">
            {creditBalance.data && (
              <Typography variant="body1">
                {t('project_languages_credit_balance', {
                  balance: String(creditBalance.data.creditBalance / 100),
                })}
              </Typography>
            )}
            <Typography variant="caption" className={classes.hint}>
              {t('project_languages_credit_balance_hint')}
            </Typography>
          </Box>
        </div>
      )}
    </>
  );
};

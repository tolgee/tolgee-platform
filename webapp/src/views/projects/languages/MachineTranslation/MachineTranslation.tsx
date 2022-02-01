import { useEffect, useRef, useState } from 'react';
import { Box, makeStyles, Tooltip, Typography } from '@material-ui/core';
import { Formik, FormikProps } from 'formik';
import clsx from 'clsx';
import { useTranslate } from '@tolgee/react';

import {
  useApiQuery,
  useApiMutation,
  matchUrlPrefix,
} from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useConfig } from 'tg.hooks/useConfig';
import { LanguageRow } from './LanguageRow';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useTableStyles } from '../tableStyles';
import { ExpandLess, ExpandMore, Help } from '@material-ui/icons';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useQueryClient } from 'react-query';

type SetMachineTranslationSettingsDto =
  components['schemas']['SetMachineTranslationSettingsDto'];

type MachineTranslationLanguagePropsDto =
  components['schemas']['MachineTranslationLanguagePropsDto'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
  },
  primaryProvider: {
    display: 'flex',
    gap: 4,
    alignItems: 'center',
  },
  helpIcon: {
    fontSize: 15,
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
  const queryClient = useQueryClient();
  const config = useConfig();
  const classes = useStyles();
  const tableClasses = useTableStyles();
  const t = useTranslate();
  const project = useProject();
  const formRef = useRef<FormikProps<any>>();
  const [expanded, setExpanded] = useState(false);
  const [formInstance, setFormInstance] = useState(0);

  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
    options: {
      onSuccess() {
        setFormInstance((i) => i + 1);
      },
    },
  });

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/machine-translation-service-settings',
    method: 'get',
    path: { projectId: project.id },
  });

  const creditBalance = useApiQuery({
    url: '/v2/projects/{projectId}/machine-translation-credit-balance',
    method: 'get',
    path: { projectId: project.id },
  });

  const updateSettings = useApiMutation({
    url: '/v2/projects/{projectId}/machine-translation-service-settings',
    method: 'put',
  });

  const lastUpdateRef = useRef<Promise<any>>();
  const applyUpdate = (data: SetMachineTranslationSettingsDto) => {
    const promise = updateSettings
      .mutateAsync({
        path: { projectId: project.id },
        content: { 'application/json': data },
      })
      .then((data) => {
        if (lastUpdateRef.current === promise) {
          queryClient.setQueriesData(
            matchUrlPrefix(
              '/v2/projects/{projectId}/machine-translation-service-settings'
            ),
            {
              _version: Math.random(), // force new instance
              ...data,
            }
          );
        }
      })
      .catch(() => {
        setFormInstance((i) => i + 1);
      });
    lastUpdateRef.current = promise;
  };

  const baseSetting = settings.data?._embedded?.languageConfigs?.find(
    (item) => item.targetLanguageId === null
  );

  const providers = Object.entries(config.machineTranslationServices.services)
    .map(([service, value]) => (value.enabled ? service : undefined))
    .filter(Boolean) as string[];

  const gridTemplateColumns = `1fr ${providers
    .map(() => 'auto')
    .join(' ')} auto`;

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
              validate={(values) => {
                const languageSettings = Object.entries(values.languages).map(
                  ([lang, value]) =>
                    formatLangSettings(lang, value.enabled, value.primary)
                );

                languageSettings.push(
                  formatLangSettings(
                    null,
                    values.default.enabled!,
                    values.default.primary
                  )
                );

                applyUpdate({
                  settings: languageSettings.filter(
                    Boolean
                  ) as MachineTranslationLanguagePropsDto[],
                });
              }}
              validateOnChange
            >
              {(form) => {
                formRef.current = form;
                return (
                  <>
                    <div className={tableClasses.topRow} />
                    {providers.map((provider) => (
                      <div
                        key={provider}
                        className={clsx(
                          tableClasses.topRow,
                          tableClasses.centered
                        )}
                      >
                        {provider}
                      </div>
                    ))}
                    <div
                      className={clsx(
                        tableClasses.topRow,
                        tableClasses.centered
                      )}
                    >
                      <Tooltip
                        title={t('project_languages_primary_provider_hint')}
                      >
                        <Box className={classes.primaryProvider}>
                          <div>
                            {t({
                              key: 'project_languages_primary_provider',
                              defaultValue: 'Primary',
                            })}
                          </div>
                          <Help className={classes.helpIcon} />
                        </Box>
                      </Tooltip>
                    </div>
                    <LanguageRow lang={null} providers={providers} />

                    {expanded && (
                      <>
                        <div className={tableClasses.divider} />
                        {languages.data?._embedded?.languages
                          ?.filter(({ base }) => !base)
                          .map((lang) => (
                            <LanguageRow
                              key={lang.id}
                              lang={lang}
                              providers={providers}
                            />
                          ))}
                      </>
                    )}
                  </>
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

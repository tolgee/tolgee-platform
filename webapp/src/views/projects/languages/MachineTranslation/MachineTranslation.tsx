import { useEffect, useMemo, useRef, useState } from 'react';
import { Box, makeStyles, Tooltip } from '@material-ui/core';
import { Formik } from 'formik';
import clsx from 'clsx';
import { useTranslate } from '@tolgee/react';

import { useApiQuery, useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useConfig } from 'tg.hooks/useConfig';
import { LanguageRow } from './LanguageRow';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useTableStyles } from '../tableStyles';
import { ExpandLess, ExpandMore, Help } from '@material-ui/icons';
import { SmoothProgress } from 'tg.component/SmoothProgress';

type SetMachineTranslationSettingsDto =
  components['schemas']['SetMachineTranslationSettingsDto'];

type MachineTranslationLanguagePropsDto =
  components['schemas']['MachineTranslationLanguagePropsDto'];

type CollectionModelLanguageConfigItemModel =
  components['schemas']['CollectionModelLanguageConfigItemModel'];

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
    fontSize: 14,
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
  loading: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
  },
}));

export const MachineTranslation = () => {
  const config = useConfig();
  const classes = useStyles();
  const tableClasses = useTableStyles();
  const t = useTranslate();
  const project = useProject();
  const formInstance = useRef(0);

  const [expanded, setExpanded] = useState(false);

  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  const [newSettingsData, setNewSettingsData] =
    useState<CollectionModelLanguageConfigItemModel>();

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/machine-translation-service-settings',
    method: 'get',
    path: { projectId: project.id },
    options: {
      onSuccess(data) {
        setNewSettingsData(data);
      },
    },
  });

  const updateSettings = useApiMutation({
    url: '/v2/projects/{projectId}/machine-translation-service-settings',
    method: 'put',
  });

  const applyUpdate = (data: SetMachineTranslationSettingsDto) =>
    updateSettings
      .mutateAsync({
        path: { projectId: project.id },
        content: { 'application/json': data },
      })
      .then((data) => {
        setNewSettingsData(data);
      });

  const settingsData = newSettingsData || settings.data;

  const baseSetting = settingsData?._embedded?.languageConfigs?.find(
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
    const config = settingsData?._embedded?.languageConfigs?.find(
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

  const isFetching = settings.isFetching || languages.isFetching;

  useGlobalLoading(isFetching);

  const isUpdating = updateSettings.isLoading;

  useMemo(() => {
    // resetting form when data changes
    return (formInstance.current += 1);
  }, [settingsData, languages.data]);

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
    if (Number(settingsData?._embedded?.languageConfigs?.length) > 1) {
      setExpanded(true);
    }
  }, [settingsData]);

  return (
    <>
      {settingsData && languages.data && (
        <div className={classes.container}>
          <div className={tableClasses.table} style={{ gridTemplateColumns }}>
            <Formik
              key={formInstance.current}
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
              <>
                <div className={tableClasses.topRow} />
                {providers.map((provider) => (
                  <div
                    key={provider}
                    className={clsx(tableClasses.topRow, tableClasses.centered)}
                  >
                    {provider}
                  </div>
                ))}
                <div
                  className={clsx(tableClasses.topRow, tableClasses.centered)}
                >
                  <Tooltip title={t('project_languages_primary_provider_hint')}>
                    <Box className={classes.primaryProvider}>
                      <div>{t('project_languages_primary_provider')}</div>
                      <Help className={classes.helpIcon} />
                    </Box>
                  </Tooltip>
                </div>
                <LanguageRow
                  lang={null}
                  disabled={isUpdating || isFetching}
                  providers={providers}
                />

                {expanded && (
                  <>
                    <div className={tableClasses.divider} />
                    {languages.data?._embedded?.languages
                      ?.filter(({ base }) => !base)
                      .map((lang) => (
                        <LanguageRow
                          key={lang.id}
                          lang={lang}
                          disabled={isUpdating || isFetching}
                          providers={providers}
                        />
                      ))}
                  </>
                )}
              </>
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
        </div>
      )}
    </>
  );
};

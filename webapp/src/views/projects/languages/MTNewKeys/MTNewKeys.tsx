import { useMemo, useRef, useState } from 'react';
import {
  makeStyles,
  Checkbox,
  FormControlLabel,
  Typography,
} from '@material-ui/core';
import { useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { useApiQuery, useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { components } from 'tg.service/apiSchema.generated';

type AutoTranslationSettingsDto =
  components['schemas']['AutoTranslationSettingsDto'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
    position: 'relative',
    margin: theme.spacing(0.5, 0),
  },
  hint: {
    color: theme.palette.text.secondary,
  },
  settings: {
    alignItems: 'flex-start',
    display: 'flex',
    flexDirection: 'column',
    margin: theme.spacing(0.5, 1),
  },
  loading: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
  },
}));

export const MTNewKeys = () => {
  const classes = useStyles();
  const project = useProject();
  const t = useTranslate();
  const formInstance = useRef(0);

  const [settingsData, setSettingsData] =
    useState<AutoTranslationSettingsDto>();

  const settings = useApiQuery({
    url: '/v2/projects/{projectId}/auto-translation-settings',
    method: 'get',
    path: { projectId: project.id },
    options: {
      onSuccess(data) {
        setSettingsData(data);
      },
    },
  });

  const updateSettings = useApiMutation({
    url: '/v2/projects/{projectId}/auto-translation-settings',
    method: 'put',
  });

  const applyUpdate = (data: AutoTranslationSettingsDto) =>
    updateSettings
      .mutateAsync({
        path: { projectId: project.id },
        content: { 'application/json': data },
      })
      .then((data) => {
        setSettingsData(data);
      });

  useGlobalLoading(settings.isFetching);

  const isUpdating = updateSettings.isLoading;

  useMemo(() => {
    // resetting form when data changes
    return (formInstance.current += 1);
  }, [settingsData]);

  return settingsData ? (
    <div className={classes.container}>
      <Typography variant="caption" className={classes.hint}>
        {t('project_languages_new_keys_hint')}
      </Typography>
      <Formik
        key={formInstance.current}
        initialValues={{
          usingTranslationMemory: settingsData.usingTranslationMemory,
          usingMachineTranslation: settingsData.usingMachineTranslation,
        }}
        enableReinitialize={true}
        onSubmit={() => {}}
        validate={(values) => {
          applyUpdate(values);
        }}
        validateOnChange
      >
        {({ values, handleChange }) => (
          <div className={classes.settings}>
            <FormControlLabel
              name="usingTranslationMemory"
              label={t('project_languages_new_keys_translation_memory_switch')}
              checked={values.usingTranslationMemory}
              onChange={handleChange}
              control={<Checkbox />}
              disabled={isUpdating}
            />
            <FormControlLabel
              name="usingMachineTranslation"
              label={t(
                'project_languages_new_keys_machine_translations_switch'
              )}
              checked={values.usingMachineTranslation}
              onChange={handleChange}
              control={<Checkbox />}
              disabled={isUpdating}
            />
          </div>
        )}
      </Formik>
      <div className={classes.loading}>
        <SmoothProgress loading={isUpdating} />
      </div>
    </div>
  ) : null;
};

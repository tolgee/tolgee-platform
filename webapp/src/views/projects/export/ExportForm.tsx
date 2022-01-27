import { useMemo } from 'react';
import { Formik, FormikErrors } from 'formik';
import { container } from 'tsyringe';
import { useTranslate } from '@tolgee/react';
import { Box, CircularProgress, makeStyles } from '@material-ui/core';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { StateType, translationStates } from 'tg.constants/translationStates';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { MessageService } from 'tg.service/MessageService';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { StateSelector } from './StateSelector';
import { LanguageSelector } from './LanguageSelector';
import { FORMATS, FormatSelector } from './FormatSelector';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

const messaging = container.resolve(MessageService);

export const exportableStates = Object.keys(translationStates);
exportableStates.splice(exportableStates.indexOf('MACHINE_TRANSLATED'), 1);

const sortStates = (arr: StateType[]) =>
  [...arr].sort(
    (a, b) => exportableStates.indexOf(a) - exportableStates.indexOf(b)
  );

const EXPORT_DEFAULT_STATES: StateType[] = sortStates([
  'TRANSLATED',
  'REVIEWED',
]);

const EXPORT_DEFAULT_FORMAT: typeof FORMATS[number] = 'JSON';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    border: `1px solid ${theme.palette.lightDivider.main}`,
    marginTop: theme.spacing(5),
    padding: theme.spacing(4),
    gap: theme.spacing(3),
    gridTemplateColumns: '1fr 1fr',
    gridTemplateAreas: `
      "states states"
      "langs  format"
      ".      submit"
    `,
  },
  states: {
    gridArea: 'states',
  },
  langs: {
    gridArea: 'langs',
  },
  format: {
    gridArea: 'format',
  },
  submit: {
    gridArea: 'submit',
    justifySelf: 'end',
  },
}));

export const ExportForm = () => {
  const classes = useStyles();

  const project = useProject();
  const exportLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/export',
    method: 'post',
    fetchOptions: {
      asBlob: true,
    },
  });

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  const t = useTranslate();

  const allLangs = useMemo(
    () => languagesLoadable.data?._embedded?.languages?.map((l) => l.tag) || [],
    [languagesLoadable.data]
  );

  const sortLanguages = (arr: string[]) =>
    [...arr].sort((a, b) => allLangs.indexOf(a) - allLangs.indexOf(b));

  const [states, setStates] = useUrlSearchState('states', {
    array: true,
    defaultVal: EXPORT_DEFAULT_STATES,
  });
  const [languages, setLanguages] = useUrlSearchState('languages', {
    array: true,
    defaultVal: allLangs,
  });
  const [format, setFormat] = useUrlSearchState('format', {
    defaultVal: EXPORT_DEFAULT_FORMAT,
  });

  if (languagesLoadable.isFetching) {
    return (
      <Box mt={2} justifyContent="center" display="flex">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Formik
      initialValues={{
        states: (states?.length
          ? states
          : EXPORT_DEFAULT_STATES) as StateType[],
        languages: (languages?.length ? languages : allLangs) as string[],
        format: (format || EXPORT_DEFAULT_FORMAT) as typeof FORMATS[number],
      }}
      validate={(values) => {
        const errors: FormikErrors<typeof values> = {};
        if (values.languages.length === 0) {
          errors.languages = t('set_at_least_one_language_error');
        }

        if (values.states.length === 0) {
          errors.states = t('set_at_least_one_state_error');
        }

        // store values in url
        setStates(sortStates(values.states));
        setLanguages(sortLanguages(values.languages));
        setFormat(values.format);

        return errors;
      }}
      validateOnBlur={false}
      enableReinitialize={false}
      onSubmit={(values, actions) => {
        exportLoadable.mutate(
          {
            path: { projectId: project.id },
            content: {
              'application/json': {
                format: values.format,
                filterState: values.states,
                languages: values.languages,
                splitByScope: false,
                splitByScopeDelimiter: '.',
                splitByScopeDepth: 0,
                zip: values.languages.length > 1,
              },
            },
          },
          {
            onSuccess(data) {
              const url = URL.createObjectURL(data as any as Blob);
              const a = document.createElement('a');
              a.href = url;
              if (data.type === 'application/zip') {
                a.download = project.name + '.zip';
              } else if (data.type === 'application/json') {
                a.download = values.languages[0] + '.json';
              } else if (data.type === 'application/x-xliff+xml') {
                a.download = values.languages[0] + '.xliff';
              }
              a.click();
            },
            onError(error) {
              parseErrorResponse(error).map((e) => messaging.error(t(e)));
            },
            onSettled() {
              actions.setSubmitting(false);
            },
          }
        );
      }}
    >
      {({ isSubmitting, handleSubmit, isValid }) => (
        <form onSubmit={handleSubmit} className={classes.container}>
          <StateSelector className={classes.states} />
          <LanguageSelector
            className={classes.langs}
            languages={languagesLoadable.data?._embedded?.languages}
          />
          <FormatSelector className={classes.format} />
          <div className={classes.submit}>
            <LoadingButton
              data-cy="export-submit-button"
              loading={isSubmitting}
              variant="contained"
              color="primary"
              type="submit"
              disabled={!isValid}
            >
              {t('export_translations_export_label')}
            </LoadingButton>
          </div>
        </form>
      )}
    </Formik>
  );
};

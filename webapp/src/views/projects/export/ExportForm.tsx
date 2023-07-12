import { useMemo } from 'react';
import { Formik, FormikErrors } from 'formik';
import { container } from 'tsyringe';
import { useTranslate } from '@tolgee/react';
import { Box, CircularProgress, styled } from '@mui/material';

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
import { NsSelector } from './NsSelector';
import { NestedSelector } from './NestedSelector';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

const messaging = container.resolve(MessageService);

export const exportableStates = Object.keys(translationStates);

const sortStates = (arr: StateType[]) =>
  [...arr].sort(
    (a, b) => exportableStates.indexOf(a) - exportableStates.indexOf(b)
  );

const EXPORT_DEFAULT_STATES: StateType[] = sortStates([
  'TRANSLATED',
  'REVIEWED',
]);

const EXPORT_DEFAULT_FORMAT: (typeof FORMATS)[number] = 'JSON';

const StyledForm = styled('form')`
  display: grid;
  border: 1px solid ${({ theme }) => theme.palette.divider2.main};
  margin-top: ${({ theme }) => theme.spacing(5)};
  padding: ${({ theme }) => theme.spacing(4)};
  gap: ${({ theme }) => theme.spacing(3)};
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    'states  states'
    'langs   format'
    'ns      ns    '
    'options submit';
  & .states {
    grid-area: states;
  }
  & .langs {
    grid-area: langs;
  }
  & .format {
    grid-area: format;
  }
  & .submit {
    grid-area: submit;
    justify-self: end;
  }
  & .ns {
    grid-area: ns;
  }
`;

const StyledOptions = styled('div')`
  display: grid;
`;

export const ExportForm = () => {
  const project = useProject();
  const { satisfiesLanguageAccess } = useProjectPermissions();
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

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: project.id },
    fetchOptions: {
      disableNotFoundHandling: true,
    },
  });

  const { t } = useTranslate();

  const allNamespaces = useMemo(
    () =>
      namespacesLoadable.data?._embedded?.namespaces?.map((n) => n.name || ''),
    [namespacesLoadable.data]
  );

  const allowedLanguages = useMemo(
    () =>
      languagesLoadable.data?._embedded?.languages?.filter((l) =>
        satisfiesLanguageAccess('translations.view', l.id)
      ) || [],
    [languagesLoadable.data]
  );

  const allowedTags = useMemo(
    () => allowedLanguages?.map((l) => l.tag) || [],
    [allowedLanguages]
  );

  const sortLanguages = (arr: string[]) =>
    [...arr].sort((a, b) => allowedTags.indexOf(a) - allowedTags.indexOf(b));

  const [states, setStates] = useUrlSearchState('states', {
    array: true,
    defaultVal: EXPORT_DEFAULT_STATES,
  });

  const [languages, setLanguages] = useUrlSearchState('languages', {
    array: true,
    defaultVal: allowedTags,
  });

  const selectedTags = (languages as string[])?.filter((l) =>
    allowedTags.includes(l)
  );

  const [format, setFormat] = useUrlSearchState('format', {
    defaultVal: EXPORT_DEFAULT_FORMAT,
  });

  const [nested, setNested] = useUrlSearchState('nested', {
    defaultVal: 'false',
  });

  if (languagesLoadable.isFetching || namespacesLoadable.isFetching) {
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
        languages: (languages?.length ? selectedTags : allowedTags) as string[],
        format: (format || EXPORT_DEFAULT_FORMAT) as (typeof FORMATS)[number],
        namespaces: allNamespaces || [],
        nested: nested === 'true',
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
        setNested(String(values.nested));

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
                structureDelimiter: values.nested ? '.' : '',
                filterNamespace: values.namespaces,
                zip:
                  values.languages.length > 1 || values.namespaces.length > 1,
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
              parseErrorResponse(error).map((e) =>
                messaging.error(<TranslatedError code={e} />)
              );
            },
            onSettled() {
              actions.setSubmitting(false);
            },
          }
        );
      }}
    >
      {({ isSubmitting, handleSubmit, isValid, values }) => (
        <StyledForm onSubmit={handleSubmit}>
          <StateSelector className="states" />
          <LanguageSelector className="langs" languages={allowedLanguages} />
          <FormatSelector className="format" />
          {values.format === 'JSON' && (
            <StyledOptions className="options">
              <NestedSelector />
            </StyledOptions>
          )}
          <NsSelector className="ns" namespaces={allNamespaces} />
          <div className="submit">
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
        </StyledForm>
      )}
    </Formik>
  );
};

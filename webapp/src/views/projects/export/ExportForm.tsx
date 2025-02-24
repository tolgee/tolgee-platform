import { useMemo } from 'react';
import { Formik, FormikErrors } from 'formik';
import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { EXPORTABLE_STATES, StateType } from 'tg.constants/translationStates';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import {
  formatGroups,
  getFormatById,
  MessageFormat,
  normalizeSelectedMessageFormat,
} from './components/formatGroups';
import { downloadExported } from './downloadExported';
import { useExportHelper } from 'tg.hooks/useExportHelper';
import { ExportFormContent } from './ExportFormContent';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const sortStates = (arr: StateType[]) =>
  [...arr].sort(
    (a, b) => EXPORTABLE_STATES.indexOf(a) - EXPORTABLE_STATES.indexOf(b)
  );

export const EXPORT_DEFAULT_STATES: StateType[] = sortStates([
  'TRANSLATED',
  'REVIEWED',
]);

// noinspection CssUnusedSymbol
const StyledForm = styled('form')`
  display: grid;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  margin-top: ${({ theme }) => theme.spacing(5)};
  padding: ${({ theme }) => theme.spacing(4)};
  gap: ${({ theme }) => theme.spacing(3)};
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    'states  states'
    'langs   format'
    'ns      messageFormat'
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

  & .messageFormat {
    grid-area: messageFormat;
  }

  & .submit {
    grid-area: submit;
    justify-self: end;
  }

  & .ns {
    grid-area: ns;
  }
`;

export const ExportForm = () => {
  const project = useProject();

  const exportLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/export',
    method: 'post',
    fetchOptions: {
      rawResponse: true,
    },
  });

  const { isFetching, allNamespaces, allowedLanguages } = useExportHelper();

  const { t } = useTranslate();

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

  const defaultFormat = formatGroups[0].formats[0];

  const [urlFormat, setUrlFormat] = useUrlSearchState('format', {
    defaultVal: defaultFormat.id,
  });

  const [messageFormat, setUrlMessageFormat] = useUrlSearchState(
    'messageFormat',
    {
      defaultVal: normalizeSelectedMessageFormat({
        format: urlFormat,
        messageFormat: undefined,
      }),
    }
  );

  const [nested, setNested] = useUrlSearchState('nested', {
    defaultVal: 'false',
  });

  if (isFetching) {
    return (
      <Box mt={6}>
        <BoxLoading />
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
        format: (urlFormat as string) || defaultFormat.id,
        namespaces: allNamespaces || [],
        nested: nested === 'true',
        supportArrays:
          (urlFormat
            ? getFormatById(urlFormat as string).defaultSupportArrays
            : defaultFormat.defaultSupportArrays) || false,
        messageFormat: (messageFormat ?? 'ICU') as MessageFormat | undefined,
        escapeHtml: false,
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
        setUrlFormat(values.format);
        setNested(String(values.nested));
        setUrlMessageFormat(values.messageFormat);
        return errors;
      }}
      validateOnBlur={false}
      enableReinitialize={false}
      onSubmit={(values, actions) => {
        const format = getFormatById(values.format);
        exportLoadable.mutate(
          {
            path: { projectId: project.id },
            content: {
              'application/json': {
                format: format.format,
                filterState: values.states,
                languages: values.languages,
                structureDelimiter: format.structured
                  ? format.defaultStructureDelimiter
                  : '',
                filterNamespace: values.namespaces,
                zip:
                  values.languages.length > 1 || values.namespaces.length > 1,
                supportArrays: values.supportArrays || false,
                messageFormat:
                  // strict message format is prioritized
                  format.messageFormat ??
                  normalizeSelectedMessageFormat(values),
                escapeHtml: values.escapeHtml || false,
              },
            },
          },
          {
            async onSuccess(response) {
              return downloadExported(
                response as unknown as Response,
                values.languages,
                format,
                project.name
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
        <QuickStartHighlight
          itemKey="export_form"
          offset={10}
          borderRadius="6px"
          message={t('quick_start_item_export_form_hint')}
        >
          <StyledForm onSubmit={handleSubmit}>
            <ExportFormContent
              values={values}
              allLanguages={allowedLanguages}
              allNamespaces={allNamespaces}
            />
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
        </QuickStartHighlight>
      )}
    </Formik>
  );
};

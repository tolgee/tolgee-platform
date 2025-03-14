import { useState } from 'react';
import { Box, Button, Dialog, DialogTitle, styled } from '@mui/material';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { ExportFormContent } from 'tg.views/projects/export/ExportFormContent';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationProps } from './types';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';
import { EXPORT_DEFAULT_STATES } from 'tg.views/projects/export/ExportForm';
import {
  MessageFormat,
  formatGroups,
  getFormatById,
  normalizeSelectedMessageFormat,
} from 'tg.views/projects/export/components/formatGroups';
import { Formik, FormikErrors } from 'formik';
import { T, useTranslate } from '@tolgee/react';
import { downloadExported } from 'tg.views/projects/export/downloadExported';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { getPreselectedLanguages } from './getPreselectedLanguages';

const StyledForm = styled('form')`
  display: grid;
  padding: ${({ theme }) => theme.spacing(3.5)};
  gap: ${({ theme }) => theme.spacing(3)};
  grid-template-columns: 1fr 1fr;
  grid-template-areas:
    'states  states'
    'langs   format'
    'ns      messageFormat'
    'options submit';
  min-width: min(600px, 80vw);

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
    margin-top: 15px;
    grid-area: submit;
    justify-self: end;
  }

  & .ns {
    grid-area: ns;
  }
`;

type Props = OperationProps;

export const OperationExportTranslations = ({ disabled, onClose }: Props) => {
  const { t } = useTranslate();
  const [dialogOpen, setDialogOpen] = useState(true);
  const project = useProject();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const translationsLanguages = useTranslationsSelector(
    (c) => c.translationsLanguages
  );
  const selection = useTranslationsSelector((c) => c.selection);

  const defaultFormat = formatGroups[0].formats[0];

  const exportLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/export',
    method: 'post',
    fetchOptions: {
      rawResponse: true,
    },
  });

  return (
    <OperationContainer>
      <BatchOperationsSubmit
        disabled={disabled}
        onClick={() => setDialogOpen(true)}
      />
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)}>
        <DialogTitle>
          <T
            keyName="batch_operation_export_translations_title"
            params={{ value: selection.length }}
          />
        </DialogTitle>
        <Formik
          initialValues={{
            states: EXPORT_DEFAULT_STATES,
            languages: getPreselectedLanguages(
              allLanguages,
              translationsLanguages ?? []
            ),
            format: defaultFormat.id,
            nested: false,
            supportArrays: defaultFormat.defaultSupportArrays || false,
            escapeHtml: false,
            messageFormat: 'ICU' as MessageFormat | undefined,
          }}
          validate={(values) => {
            const errors: FormikErrors<typeof values> = {};
            if (values.languages.length === 0) {
              errors.languages = t('set_at_least_one_language_error');
            }

            if (values.states.length === 0) {
              errors.states = t('set_at_least_one_state_error');
            }

            return errors;
          }}
          validateOnBlur={false}
          enableReinitialize={false}
          onSubmit={(values, actions) => {
            actions.setSubmitting(true);
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
                    zip: true,
                    supportArrays: values.supportArrays || false,
                    filterKeyId: selection,
                    messageFormat:
                      // strict message format is prioritized
                      format.messageFormat ??
                      normalizeSelectedMessageFormat(values),
                    escapeHtml: values.escapeHtml,
                  },
                },
              },
              {
                async onSuccess(response) {
                  onClose();
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
          {({ values, handleSubmit, isSubmitting, isValid }) => (
            <StyledForm onSubmit={handleSubmit}>
              <ExportFormContent values={values} allLanguages={allLanguages} />
              <Box className="submit" display="flex" gap={1}>
                <Button onClick={() => setDialogOpen(false)}>
                  {t('global_cancel_button')}
                </Button>
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
              </Box>
            </StyledForm>
          )}
        </Formik>
      </Dialog>
    </OperationContainer>
  );
};

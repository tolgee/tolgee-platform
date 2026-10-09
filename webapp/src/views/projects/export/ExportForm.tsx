import { useState } from 'react';
import { Formik, FormikErrors } from 'formik';
import { useTranslate } from '@tolgee/react';
import { Box, Button, styled } from '@mui/material';
import { useHistory, useLocation } from 'react-router-dom';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useBranchFromUrlPath } from 'tg.component/branching/useBranchFromUrlPath';
import { useExportHelper } from 'tg.hooks/useExportHelper';
import { useProjectPreferenceStorage } from 'tg.hooks/useProjectPreferenceStorage';
import { getFormatById } from 'tg.views/projects/export/components/formatGroups';
import { LanguageSelector } from 'tg.views/projects/export/components/LanguageSelector';
import { FormatSelector } from 'tg.views/projects/export/components/FormatSelector';
import { ExportAdvancedSettings } from 'tg.views/projects/export/components/ExportAdvancedSettings';
import { downloadExported } from 'tg.views/projects/export/downloadExported';
import {
  ExportFormValues,
  getDefaultExportValues,
  getExportRequestBody,
  isSameExportSetup,
  readExportSettingsFromUrl,
  restoreExportValues,
  StoredExportSettings,
  toStoredExportSettings,
} from 'tg.views/projects/export/exportSettings';

const StyledForm = styled('form')`
  display: grid;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  margin-top: ${({ theme }) => theme.spacing(5)};
  padding: ${({ theme }) => theme.spacing(4)};
  gap: ${({ theme }) => theme.spacing(3)};
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  grid-template-areas:
    'langs    format'
    'advanced advanced'
    'footer   footer';

  & .langs {
    grid-area: langs;
  }

  & .format {
    grid-area: format;
  }

  & .advanced {
    grid-area: advanced;
  }

  & .footer {
    grid-area: footer;
  }
`;

export const ExportForm = () => {
  const project = useProject();
  const branchName = useBranchFromUrlPath();
  const location = useLocation();
  const history = useHistory();
  const { t } = useTranslate();

  const exportLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/export',
    method: 'post',
    fetchOptions: {
      rawResponse: true,
    },
  });

  const { isFetching, allNamespaces, allowedLanguages, allowedLanguageTags } =
    useExportHelper();

  const settingsStorage =
    useProjectPreferenceStorage<StoredExportSettings>('exportSettings');
  const expandedStorage = useProjectPreferenceStorage<boolean>(
    'exportAdvancedExpanded'
  );
  const [expandedOverride, setExpandedOverride] = useState<boolean>();

  if (
    isFetching ||
    settingsStorage.loadable.isLoading ||
    expandedStorage.loadable.isLoading
  ) {
    return (
      <Box mt={6}>
        <BoxLoading />
      </Box>
    );
  }

  const expanded =
    expandedOverride ?? expandedStorage.loadable.data?.data ?? false;
  const namespaces = allNamespaces ?? [];
  const defaults = getDefaultExportValues(allowedLanguageTags, namespaces);

  const initialValues = restoreExportValues(
    {
      ...settingsStorage.loadable.data?.data,
      ...readExportSettingsFromUrl(location.search),
    },
    allowedLanguageTags,
    namespaces
  );

  function handleExpandedChange(
    value: boolean,
    source: 'user' | 'invalidField'
  ) {
    setExpandedOverride(value);
    if (source === 'user') {
      expandedStorage.update(value);
    }
  }

  return (
    <Formik
      initialValues={initialValues}
      validate={(values) => {
        const errors: FormikErrors<ExportFormValues> = {};
        if (values.languages.length === 0) {
          errors.languages = t('set_at_least_one_language_error');
        }
        if (values.states.length === 0) {
          errors.states = t('set_at_least_one_state_error');
        }
        return errors;
      }}
      validateOnMount
      validateOnBlur={false}
      enableReinitialize={false}
      onSubmit={(values, actions) => {
        const format = getFormatById(values.format);
        exportLoadable.mutate(
          {
            path: { projectId: project.id },
            content: {
              'application/json': getExportRequestBody(values, branchName),
            },
          },
          {
            async onSuccess(response) {
              settingsStorage.update(
                toStoredExportSettings(values, allowedLanguageTags, namespaces)
              );
              return downloadExported(
                response as unknown as Response,
                values.languages,
                format,
                project.name,
                branchName
              );
            },
            onSettled() {
              actions.setSubmitting(false);
            },
          }
        );
      }}
    >
      {({ isSubmitting, handleSubmit, isValid, values, resetForm }) => (
        <QuickStartHighlight
          itemKey="export_form"
          offset={10}
          borderRadius="6px"
          message={t('quick_start_item_export_form_hint')}
        >
          <StyledForm onSubmit={handleSubmit}>
            <LanguageSelector className="langs" languages={allowedLanguages} />
            <FormatSelector className="format" />
            <div className="advanced">
              <ExportAdvancedSettings
                allNamespaces={allNamespaces}
                collapsible
                expanded={expanded}
                onExpandedChange={handleExpandedChange}
              />
            </div>
            <Box
              className="footer"
              display="flex"
              justifyContent="space-between"
              alignItems="center"
            >
              <Button
                sx={{ ml: -2 }}
                color="inherit"
                disabled={isSameExportSetup(values, defaults)}
                onClick={() => {
                  resetForm({ values: defaults });
                  history.replace(location.pathname);
                  settingsStorage.update(null);
                }}
                data-cy="export-reset-to-defaults-button"
              >
                {t('export_reset_to_defaults', 'Reset to defaults')}
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
        </QuickStartHighlight>
      )}
    </Formik>
  );
};

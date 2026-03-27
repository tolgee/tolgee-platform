import { useState } from 'react';
import { Formik } from 'formik';
import {
  styled,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Box,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { LanguageItem } from 'tg.component/languages/LanguageItem';
import { QaSettingsItem } from 'tg.ee.module/qa/components/QaSettingsItem';
import { components } from 'tg.service/apiSchema.generated';

type QaSettings = components['schemas']['QaSettingsRequest'];
type QaCheckType = components['schemas']['QaIssueModel']['type'];
type QaCheckSeverity = QaSettings['settings'][keyof QaSettings['settings']];
type LanguageQaConfigModel = components['schemas']['LanguageQaConfigModel'];

const StyledBanner = styled('div')`
  background: ${({ theme }) => theme.palette.warning.light};
  color: ${({ theme }) => theme.palette.grey[900]};
  padding: ${({ theme }) => theme.spacing(0.5, 3)};
`;

type Props = {
  onClose: () => void;
  languageConfig: LanguageQaConfigModel;
  globalSettings: Record<QaCheckType, QaCheckSeverity>;
};

type FormValues = {
  settings: Record<QaCheckType, QaCheckSeverity | null>;
};

export const QaLanguageSettingsDialog = ({
  onClose,
  languageConfig,
  globalSettings,
}: Props) => {
  const { t } = useTranslate();
  const project = useProject();

  // All settings from global settings with value from language config or null if overridden
  const initialSettings = Object.fromEntries(
    Object.keys(globalSettings).map((type) => [
      type,
      languageConfig.customSettings?.[type as QaCheckType] ?? null,
    ])
  ) as Record<QaCheckType, QaCheckSeverity | null>;

  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/qa-settings/languages/{languageId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/qa-settings',
  });

  const deleteMutation = useApiMutation({
    url: '/v2/projects/{projectId}/qa-settings/languages/{languageId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/qa-settings',
  });

  const [resetting, setResetting] = useState(false);
  async function handleResetToGlobal() {
    setResetting(true);
    try {
      await deleteMutation.mutateAsync({
        path: {
          projectId: project.id,
          languageId: languageConfig.language.id,
        },
      });
      onClose();
    } catch {
      setResetting(false);
    }
  }

  const isInherited = !languageConfig.customSettings;

  return (
    <Formik<FormValues>
      initialValues={{
        settings: initialSettings,
      }}
      onSubmit={async (values) => {
        await updateMutation.mutateAsync({
          path: {
            projectId: project.id,
            languageId: languageConfig.language.id,
          },
          content: {
            'application/json': {
              // Values can be null, but the schema doesn't capture this...
              settings: values.settings as any,
            },
          },
        });
        onClose();
      }}
    >
      {({ handleSubmit, values, setFieldValue, isSubmitting, dirty }) => {
        const isBusy = resetting || isSubmitting;
        return (
          <Dialog open={true} onClose={onClose}>
            {isInherited && (
              <StyledBanner data-cy="qa-language-dialog-inherited-banner">
                {t('project_settings_qa_language_dialog_inherited_banner')}
              </StyledBanner>
            )}
            <DialogTitle>
              <div>{t('project_settings_qa_language_dialog_title')}</div>
              <Box sx={{ fontSize: 14, fontWeight: 'normal' }}>
                <LanguageItem language={languageConfig.language} />
              </Box>
            </DialogTitle>
            <DialogContent sx={{ width: '90vw', maxWidth: 600 }}>
              {(Object.keys(globalSettings) as QaCheckType[]).map((type) => (
                <QaSettingsItem
                  key={type}
                  showDefault
                  type={type}
                  value={values.settings[type] ?? null}
                  globalDefault={globalSettings[type]}
                  onChange={(checkType, severity) => {
                    setFieldValue(`settings.${checkType}`, severity);
                  }}
                />
              ))}
            </DialogContent>
            <Box display="flex" justifyContent="space-between">
              <DialogActions>
                {!isInherited && (
                  <LoadingButton
                    data-cy="qa-language-dialog-reset-to-global"
                    variant="outlined"
                    color="secondary"
                    loading={isBusy}
                    onClick={handleResetToGlobal}
                  >
                    {t('project_settings_qa_language_dialog_reset_to_global')}
                  </LoadingButton>
                )}
              </DialogActions>
              <DialogActions>
                <Button onClick={onClose}>
                  {t('project_settings_qa_language_dialog_cancel')}
                </Button>
                <LoadingButton
                  disabled={!dirty}
                  onClick={() => handleSubmit()}
                  variant="contained"
                  color="primary"
                  loading={isBusy}
                  data-cy="qa-language-dialog-save"
                >
                  {t('project_settings_qa_language_dialog_save')}
                </LoadingButton>
              </DialogActions>
            </Box>
          </Dialog>
        );
      }}
    </Formik>
  );
};

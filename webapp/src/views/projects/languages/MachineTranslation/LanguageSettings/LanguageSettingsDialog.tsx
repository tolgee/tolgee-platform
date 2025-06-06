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

import { useConfig } from 'tg.globalContext/helpers';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { LanguageItem } from '../../../../../component/languages/LanguageItem';
import { FormalityType, RowData, ServiceType } from '../types';
import { supportsFormality } from '../supportsFormality';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import {
  FormType,
  LanguageSettingsForm,
  Service,
} from './LanguageSettingsForm';

const StyledSubtitle = styled('div')`
  font-size: 14px;
  font-weight: normal;
`;

const StyledBanner = styled('div')`
  background: ${({ theme }) => theme.palette.warning.light};
  color: ${({ theme }) => theme.palette.grey[900]};
  padding: ${({ theme }) => theme.spacing(0.5, 3)};
`;

type Props = {
  onClose: () => void;
  rowData: RowData;
};

export const LanguageSettingsDialog = ({
  onClose,
  rowData: { inheritedFromDefault, onChange, settings },
}: Props) => {
  const config = useConfig();
  const project = useProject();

  const mtEnabled = Object.values(
    config?.machineTranslationServices.services
  ).some(({ enabled }) => enabled);

  const promptEnabled =
    config?.machineTranslationServices.services &&
    Object.entries(config?.machineTranslationServices.services).find(
      ([service]) => service === 'PROMPT'
    )?.[1]?.enabled;

  const { t } = useTranslate();
  const primaryServiceInfo = settings.mtSettings?.primaryServiceInfo;

  const isDefault = !settings.language;

  const prompts = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 1000,
    },
    options: {
      enabled: Boolean(promptEnabled),
    },
  });

  const services: Service[] = (
    Object.entries(config.machineTranslationServices.services)
      .map(([service, value]) => (value.enabled ? service : undefined))
      .filter(Boolean) as unknown as ServiceType[]
  ).map((type) => {
    const serviceInfo = settings.info?.supportedServices.find(
      (s) => s.serviceType === type
    );
    const languageSupported = Boolean(serviceInfo) || isDefault;
    const formalitySupported = supportsFormality(settings.info!, type) ?? false;
    return {
      type,
      languageSupported,
      formalitySupported,
    };
  });

  const formalityMap = {} as Record<ServiceType, FormalityType | undefined>;
  const promptId =
    settings.mtSettings?.primaryServiceInfo?.promptId ??
    settings.mtSettings?.enabledServicesInfo.find(
      ({ serviceType }) => serviceType === 'PROMPT'
    )?.promptId;

  settings.mtSettings?.enabledServicesInfo.forEach(
    ({ serviceType, formality }) => {
      formalityMap[serviceType] = formality;
    }
  );
  if (primaryServiceInfo?.serviceType && primaryServiceInfo.formality) {
    formalityMap[primaryServiceInfo.serviceType] = primaryServiceInfo.formality;
  }

  const [reseting, setReseting] = useState(false);
  async function handleResetToDefault() {
    setReseting(true);
    return onChange(settings.info!, null)
      .then(() => {
        onClose();
      })
      .catch(() => {
        setReseting(false);
      });
  }

  return (
    <Formik
      initialValues={
        {
          enabledServices:
            settings.mtSettings?.enabledServicesInfo.map(
              (s) => s.serviceType
            ) ?? [],
          servicesFormality: formalityMap,
          primaryService: primaryServiceInfo?.serviceType,
          autoTranslation: settings.autoSettings,
          promptId,
        } satisfies FormType
      }
      onSubmit={async (values) => {
        const primaryService = values.primaryService;

        const primaryFormality =
          primaryService &&
          (values.servicesFormality[primaryService] === 'DEFAULT'
            ? undefined
            : values.servicesFormality[primaryService]);

        const primaryServiceInfo =
          primaryService !== undefined
            ? {
                serviceType: primaryService,
                formality: primaryFormality,
                promptId:
                  primaryService === 'PROMPT' ? values.promptId : undefined,
              }
            : undefined;

        await onChange(settings.info!, {
          machineTranslation: {
            targetLanguageId: settings.language?.id,
            primaryServiceInfo: primaryServiceInfo,
            enabledServicesInfo: values.enabledServices.map((serviceType) => ({
              serviceType: serviceType,
              formality: values.servicesFormality[serviceType],
              promptId: serviceType === 'PROMPT' ? values.promptId : undefined,
            })),
          },
          autoTranslation: {
            ...values.autoTranslation!,
            languageId: settings.language?.id,
          },
        });
        onClose();
      }}
    >
      {({ handleSubmit, values, setFieldValue, isSubmitting, dirty }) => {
        return (
          <Dialog open={true} onClose={onClose}>
            {inheritedFromDefault && (
              <StyledBanner data-cy="project-mt-dialog-settings-inherited">
                {t('project_mt_dialog_settings_inherited_message')}
              </StyledBanner>
            )}
            <DialogTitle>
              <div>{t('project_mt_dialog_title')}</div>
              <StyledSubtitle>
                {settings.language ? (
                  <LanguageItem language={settings.language} />
                ) : (
                  t('project_languages_default_settings')
                )}
              </StyledSubtitle>
            </DialogTitle>
            <DialogContent sx={{ width: '90vw', maxWidth: 600 }}>
              <LanguageSettingsForm
                services={services}
                prompts={prompts.data?._embedded?.prompts}
                mtEnabled={mtEnabled}
              />
            </DialogContent>
            <Box display="flex" justifyContent="space-between">
              <DialogActions>
                {!inheritedFromDefault && !isDefault && (
                  <LoadingButton
                    data-cy="permissions-menu-reset-to-organization"
                    variant="outlined"
                    color="secondary"
                    loading={reseting}
                    onClick={handleResetToDefault}
                  >
                    {t('project_mt_dialog_reset_to_default')}
                  </LoadingButton>
                )}
              </DialogActions>
              <DialogActions>
                <Button onClick={onClose}>
                  {t('project_mt_dialog_cancel_button')}
                </Button>
                <LoadingButton
                  disabled={!dirty}
                  onClick={() => handleSubmit()}
                  variant="contained"
                  color="primary"
                  loading={isSubmitting}
                  data-cy="mt-language-dialog-save"
                >
                  {t('project_mt_dialog_save_button')}
                </LoadingButton>
              </DialogActions>
            </Box>
          </Dialog>
        );
      }}
    </Formik>
  );
};

import React, { useState } from 'react';
import clsx from 'clsx';
import { Formik, getIn } from 'formik';
import {
  styled,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Checkbox,
  Box,
  Radio,
  Select,
  MenuItem,
  FormControlLabel,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { useConfig } from 'tg.globalContext/helpers';
import { useFormalityTranslation } from 'tg.translationTools/useFormalityTranslation';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { LanguageItem } from '../../../../component/languages/LanguageItem';
import {
  StyledLanguageTable,
  TABLE_CENTERED,
  TABLE_FIRST_CELL,
  TABLE_TOP_ROW,
} from '../../../../component/languages/tableStyles';
import { FormalityType, RowData, ServiceType } from './types';
import { ServiceLabel } from './ServiceLabel';
import { PrimaryServiceLabel } from './PrimaryServiceLabel';
import { SuggestionsLabel } from './SuggestionsLabel';
import { supportsFormality } from './supportsFormality';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useServiceImg } from 'tg.views/projects/translations/ToolsPanel/panels/MachineTranslation/useServiceImg';
import { getServiceName } from './getServiceName';

const StyledSubtitle = styled('div')`
  font-size: 14px;
  font-weight: normal;
`;

const StyledBanner = styled('div')`
  background: ${({ theme }) => theme.palette.warning.light};
  color: ${({ theme }) => theme.palette.grey[900]};
  padding: ${({ theme }) => theme.spacing(0.5, 3)};
`;

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  position: relative;
  padding: ${({ theme }) => theme.spacing(0.5, 0)};
`;

const StyledHint = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledFormalityHint = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 15px;
`;

const StyledSettings = styled('div')`
  align-items: flex-start;
  display: flex;
  flex-direction: column;
  margin: ${({ theme }) => theme.spacing(0.5, 1)};
`;

const FORMALITY_VALUES = ['DEFAULT', 'FORMAL', 'INFORMAL'] as const;

type Service = {
  type: ServiceType;
  languageSupported: boolean;
  formalitySupported: boolean;
};

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

  const { t } = useTranslate();
  const translateFormality = useFormalityTranslation();
  const primaryServiceInfo = settings.mtSettings?.primaryServiceInfo;
  const getServiceImg = useServiceImg();

  const isDefault = !settings.language;

  const prompts = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 1000,
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
      initialValues={{
        enabledServices:
          settings.mtSettings?.enabledServicesInfo.map((s) => s.serviceType) ??
          [],
        servicesFormality: formalityMap,
        primaryService: primaryServiceInfo?.serviceType,
        autoTranslation: settings.autoSettings,
        promptId,
      }}
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
        function setPrimary(value: ServiceType | undefined) {
          if (values.primaryService !== value) {
            setFieldValue('primaryService', value);
          } else {
            setFieldValue('primaryService', undefined);
          }
        }

        function toggleService(value: ServiceType) {
          if (values.enabledServices.includes(value)) {
            setFieldValue(
              'enabledServices',
              values.enabledServices.filter((s) => s !== value)
            );
          } else {
            setFieldValue('enabledServices', [
              ...values.enabledServices,
              value,
            ]);
          }
        }

        function toggleCheckBox(name: string) {
          setFieldValue(name, !getIn(values, name));
        }

        function changeFormality(
          service: ServiceType | number,
          value: FormalityType
        ) {
          setFieldValue(`servicesFormality.${service}`, value);
        }

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
              <StyledLanguageTable
                sx={{
                  gridTemplateColumns: 'auto auto auto auto',
                  my: 2,
                }}
              >
                <div className={TABLE_TOP_ROW} />
                <div className={clsx(TABLE_TOP_ROW, TABLE_CENTERED)}>
                  <PrimaryServiceLabel />
                </div>
                <div className={clsx(TABLE_TOP_ROW, TABLE_CENTERED)}>
                  <SuggestionsLabel />
                </div>
                <div className={clsx(TABLE_TOP_ROW, TABLE_CENTERED)}>
                  {t('project_mt_dialog_service_formality')}
                </div>
                {services.map(
                  ({ type, languageSupported, formalitySupported }) => {
                    return (
                      <React.Fragment key={type}>
                        <Box
                          className={TABLE_FIRST_CELL}
                          display="flex"
                          gap={2}
                        >
                          <ServiceLabel
                            name={getServiceName(type)}
                            icon={getServiceImg(type, false)}
                            isSupported={languageSupported}
                          />
                          {type === 'PROMPT' && (
                            <Select
                              size="small"
                              value={values.promptId ?? null}
                              onChange={(e) =>
                                setFieldValue('promptId', e.target.value)
                              }
                              displayEmpty
                            >
                              <MenuItem value={null as any}>default</MenuItem>
                              {prompts.data?._embedded?.prompts?.map((item) => {
                                return (
                                  <MenuItem key={item.id} value={item.id}>
                                    {item.name}
                                  </MenuItem>
                                );
                              })}
                            </Select>
                          )}
                        </Box>
                        <div className={TABLE_CENTERED}>
                          <Radio
                            data-cy="mt-language-dialog-primary-radio"
                            data-cy-service={type}
                            size="small"
                            checked={values.primaryService === type}
                            onClick={() => setPrimary(type)}
                            disabled={!languageSupported}
                          />
                        </div>
                        <div className={TABLE_CENTERED}>
                          {languageSupported && (
                            <Checkbox
                              data-cy="mt-language-dialog-enabled-checkbox"
                              data-cy-service={type}
                              disabled={!languageSupported}
                              size="small"
                              checked={Boolean(
                                values.enabledServices.includes(type)
                              )}
                              onClick={() => toggleService(type)}
                            />
                          )}
                        </div>
                        <div className={TABLE_CENTERED}>
                          {formalitySupported ? (
                            <Select
                              data-cy="mt-language-dialog-formality-select"
                              data-cy-service={type}
                              sx={{ width: 130 }}
                              value={
                                values.servicesFormality[type] ?? 'DEFAULT'
                              }
                              size="small"
                              onChange={(e) =>
                                changeFormality(
                                  type,
                                  e.target.value as FormalityType
                                )
                              }
                            >
                              {FORMALITY_VALUES.map((val) => (
                                <MenuItem
                                  key={val}
                                  value={val}
                                  data-cy="mt-language-dialog-formality-select-item"
                                >
                                  {translateFormality(val)}
                                </MenuItem>
                              ))}
                            </Select>
                          ) : (
                            <StyledFormalityHint>
                              {t('project_mt_dialog_formality_not_supported')}
                            </StyledFormalityHint>
                          )}
                        </div>
                      </React.Fragment>
                    );
                  }
                )}
              </StyledLanguageTable>

              <StyledContainer>
                <Box mt={4} mb={2}>
                  <Typography variant="h5">
                    <T keyName="machine_translation_new_keys_title" />
                  </Typography>
                </Box>
                <StyledHint variant="caption">
                  {t('project_languages_new_keys_hint')}
                </StyledHint>
                <StyledSettings>
                  <FormControlLabel
                    data-cy="mt-language-dialog-auto-machine-translation"
                    label={t(
                      'project_languages_new_keys_machine_translations_switch',
                      'Enable machine translation with primary provider'
                    )}
                    checked={
                      values.autoTranslation?.usingMachineTranslation || false
                    }
                    onChange={() =>
                      toggleCheckBox('autoTranslation.usingMachineTranslation')
                    }
                    control={<Checkbox />}
                    disabled={!mtEnabled}
                  />
                  <FormControlLabel
                    data-cy="mt-language-dialog-auto-translation-memory"
                    label={t(
                      'project_languages_new_keys_translation_memory_switch',
                      'Enable translation memory'
                    )}
                    checked={
                      values.autoTranslation?.usingTranslationMemory || false
                    }
                    onChange={() =>
                      toggleCheckBox('autoTranslation.usingTranslationMemory')
                    }
                    control={<Checkbox />}
                  />
                  <FormControlLabel
                    data-cy="mt-language-dialog-auto-for-import"
                    label={t(
                      'project_languages_auto_translation_enable_for_import_switch',
                      'Enable for import'
                    )}
                    checked={values.autoTranslation?.enableForImport || false}
                    onChange={() =>
                      toggleCheckBox('autoTranslation.enableForImport')
                    }
                    control={<Checkbox />}
                  />
                </StyledSettings>
              </StyledContainer>
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

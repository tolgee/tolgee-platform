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

import { LanguageItem } from '../LanguageItem';
import {
  StyledLanguageTable,
  TABLE_CENTERED,
  TABLE_FIRST_CELL,
  TABLE_TOP_ROW,
} from '../tableStyles';
import { FormalityType, RowData, ServiceType } from './types';
import { ServiceLabel } from './ServiceLabel';
import { PrimaryServiceLabel } from './PrimaryServiceLabel';
import { SuggestionsLabel } from './SuggestionsLabel';

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

const StyledSettings = styled('div')`
  align-items: flex-start;
  display: flex;
  flex-direction: column;
  margin: ${({ theme }) => theme.spacing(0.5, 1)};
`;

const FORMALITY_VALUES = ['DEFAULT', 'FORMAL', 'INFORMAL'] as const;

type Props = {
  onClose: () => void;
  rowData: RowData;
};

export const LanguageSettingsDialog = ({
  onClose,
  rowData: { inheritedFromDefault, onChange, settings },
}: Props) => {
  const config = useConfig();

  const mtEnabled = Object.values(
    config?.machineTranslationServices.services
  ).some(({ enabled }) => enabled);

  const { t } = useTranslate();
  const translateFormality = useFormalityTranslation();
  const primaryServiceInfo = settings.mtSettings?.primaryServiceInfo;

  const isDefault = !settings.language;

  const services = Object.entries(config.machineTranslationServices.services)
    .map(([service, value]) => (value.enabled ? service : undefined))
    .filter(Boolean) as unknown as ServiceType[];

  const formalityMap = {} as Record<ServiceType, FormalityType | undefined>;
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
    return onChange(settings.language?.id, null)
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
          settings.mtSettings?.enabledServicesInfo.map((s) => s.serviceType) ||
          [],
        servicesFormality: formalityMap,
        primaryService: primaryServiceInfo?.serviceType,
        autoTranslation: settings.autoSettings,
      }}
      onSubmit={async (values) => {
        const primaryService = values.primaryService;

        const primaryFormality =
          primaryService &&
          (values.servicesFormality[primaryService] === 'DEFAULT'
            ? undefined
            : values.servicesFormality[primaryService]);

        const primaryServiceInfo = primaryService && {
          serviceType: primaryService,
          formality: primaryFormality,
        };

        await onChange(settings.id || undefined, {
          machineTranslation: {
            targetLanguageId: settings.language?.id,
            primaryServiceInfo: primaryServiceInfo,
            enabledServicesInfo: values.enabledServices.map((serviceType) => ({
              serviceType,
              formality: values.servicesFormality[serviceType],
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

        function changeFormality(service: ServiceType, value: FormalityType) {
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
                {services.map((service) => {
                  const serviceInfo = settings.info?.supportedServices.find(
                    (s) => s.serviceType === service
                  );
                  const languageSupported = Boolean(serviceInfo) || isDefault;
                  const formalitySupported = serviceInfo?.formalitySupported;
                  return (
                    <React.Fragment key={service}>
                      <Box className={TABLE_FIRST_CELL} key={service}>
                        <ServiceLabel
                          service={service}
                          isSupported={languageSupported}
                        />
                      </Box>
                      <div className={TABLE_CENTERED}>
                        <Radio
                          data-cy="mt-language-dialog-primary-radio"
                          data-cy-service={service}
                          size="small"
                          checked={values.primaryService === service}
                          onClick={() => setPrimary(service)}
                          disabled={!languageSupported}
                        />
                      </div>
                      <div className={TABLE_CENTERED}>
                        {languageSupported && (
                          <Checkbox
                            data-cy="mt-language-dialog-enabled-checkbox"
                            data-cy-service={service}
                            disabled={!languageSupported}
                            size="small"
                            checked={Boolean(
                              values.enabledServices.includes(service)
                            )}
                            onClick={() => toggleService(service)}
                          />
                        )}
                      </div>
                      <div className={TABLE_CENTERED}>
                        {formalitySupported && (
                          <Select
                            data-cy="mt-language-dialog-formality-select"
                            data-cy-service={service}
                            sx={{ width: 130 }}
                            value={
                              values.servicesFormality[service] ?? 'DEFAULT'
                            }
                            size="small"
                            onChange={(e) =>
                              changeFormality(
                                service,
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
                        )}
                      </div>
                    </React.Fragment>
                  );
                })}
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

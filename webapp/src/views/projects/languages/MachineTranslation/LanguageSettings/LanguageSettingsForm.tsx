import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  MenuItem,
  Radio,
  styled,
  Typography,
} from '@mui/material';
import clsx from 'clsx';
import {
  StyledLanguageTable,
  TABLE_CENTERED,
  TABLE_TOP_ROW,
} from 'tg.component/languages/tableStyles';
import { useServiceImg } from 'tg.hooks/useServiceImg';

import { PrimaryServiceLabel } from '../PrimaryServiceLabel';
import { SuggestionsLabel } from '../SuggestionsLabel';
import React from 'react';
import { ServiceLabel } from '../ServiceLabel';
import {
  AutoTranslationSettingsDto,
  FormalityType,
  ServiceType,
} from '../types';
import { getIn, useFormikContext } from 'formik';
import { Select } from 'tg.component/common/Select';
import { useFormalityTranslation } from 'tg.translationTools/useFormalityTranslation';
import { components } from 'tg.service/apiSchema.generated';

import { useServiceName } from 'tg.hooks/useServiceName';

type PromptModel = components['schemas']['PromptModel'];

export type Service = {
  type: ServiceType;
  languageSupported: boolean;
  formalitySupported: boolean;
};

export type FormType = {
  enabledServices: ServiceType[];
  servicesFormality: Record<ServiceType, FormalityType>;
  primaryService: ServiceType | undefined;
  autoTranslation: AutoTranslationSettingsDto | undefined;
  promptId: number | undefined;
};

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

const StyledLabelCell = styled(Box)`
  display: grid;
  gap: 8px;
  align-items: center;
  min-height: 50px;
  grid-column-start: 1;
  padding: ${({ theme }) => theme.spacing(1, 0, 1, 2)};
`;

const StyledCell = styled(Box)`
  display: grid;
  justify-content: center;
  align-items: center;
  height: 60px;
`;

const StyledSelect = styled(Select)`
  width: 200px;
`;

const FORMALITY_VALUES = ['DEFAULT', 'FORMAL', 'INFORMAL'] as const;

type Props = {
  services: Service[];
  prompts: PromptModel[] | undefined;
  mtEnabled: boolean;
};

export const LanguageSettingsForm = ({
  services,
  prompts,
  mtEnabled,
}: Props) => {
  const { t } = useTranslate();
  const getServiceImg = useServiceImg();
  const getServiceName = useServiceName();
  const translateFormality = useFormalityTranslation();

  const { setFieldValue, values } = useFormikContext<FormType>();

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
      setFieldValue('enabledServices', [...values.enabledServices, value]);
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
    <>
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
        {services.map(({ type, languageSupported, formalitySupported }) => {
          return (
            <React.Fragment key={type}>
              <StyledLabelCell>
                <ServiceLabel
                  name={getServiceName(type)}
                  icon={getServiceImg(type, false)}
                  isSupported={languageSupported}
                />
                {type === 'PROMPT' && (
                  <StyledSelect
                    size="small"
                    value={values.promptId ?? null}
                    onChange={(e) => setFieldValue('promptId', e.target.value)}
                    displayEmpty
                    minHeight={false}
                  >
                    <MenuItem value={null as any}>
                      {t('ai_prompt_default_name')}
                    </MenuItem>
                    {prompts?.map((item) => {
                      return (
                        <MenuItem key={item.id} value={item.id}>
                          {item.name}
                        </MenuItem>
                      );
                    })}
                  </StyledSelect>
                )}
              </StyledLabelCell>
              <StyledCell>
                <Radio
                  data-cy="mt-language-dialog-primary-radio"
                  data-cy-service={type}
                  size="small"
                  checked={values.primaryService === type}
                  onClick={() => setPrimary(type)}
                  disabled={!languageSupported}
                />
              </StyledCell>
              <StyledCell>
                {languageSupported && (
                  <Checkbox
                    data-cy="mt-language-dialog-enabled-checkbox"
                    data-cy-service={type}
                    disabled={!languageSupported}
                    size="small"
                    checked={Boolean(values.enabledServices.includes(type))}
                    onClick={() => toggleService(type)}
                  />
                )}
              </StyledCell>
              <StyledCell>
                {formalitySupported ? (
                  <Select
                    data-cy="mt-language-dialog-formality-select"
                    data-cy-service={type}
                    sx={{ width: 130 }}
                    value={values.servicesFormality[type] ?? 'DEFAULT'}
                    size="small"
                    minHeight={false}
                    onChange={(e) =>
                      changeFormality(type, e.target.value as FormalityType)
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
              </StyledCell>
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
            checked={values.autoTranslation?.usingMachineTranslation || false}
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
            checked={values.autoTranslation?.usingTranslationMemory || false}
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
            onChange={() => toggleCheckBox('autoTranslation.enableForImport')}
            control={<Checkbox />}
          />
        </StyledSettings>
      </StyledContainer>
    </>
  );
};

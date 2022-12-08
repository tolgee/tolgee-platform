import { useEffect, useRef, useState } from 'react';
import { Formik, FormikProps } from 'formik';
import { Checkbox, FormControlLabel, styled, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useAutoTranslateSettings } from './useAutoTranslateSettings';

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

const StyledLoadingWrapper = styled('div')`
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
`;

type Props = {
  mtEnabled: boolean;
};

export const AutoTranslations: React.FC<Props> = ({ mtEnabled }) => {
  const { t } = useTranslate();
  const [formInstance, setFormInstance] = useState(0);
  const formRef = useRef<FormikProps<any>>();

  const { settings, updateSettings, applyUpdate } = useAutoTranslateSettings({
    onReset: () => setFormInstance((i) => i + 1),
  });

  useEffect(() => {
    formRef.current?.resetForm();
  }, [settings.data]);

  useGlobalLoading(settings.isFetching);

  const isUpdating = updateSettings.isLoading;

  return settings.data ? (
    <StyledContainer>
      <StyledHint variant="caption">
        {t('project_languages_new_keys_hint')}
      </StyledHint>
      <Formik
        key={formInstance}
        initialValues={{
          usingTranslationMemory: settings.data.usingTranslationMemory,
          usingMachineTranslation: settings.data.usingMachineTranslation,
        }}
        enableReinitialize={true}
        onSubmit={() => {}}
        validate={(values) => {
          applyUpdate(values);
        }}
        validateOnChange
      >
        {(form) => {
          formRef.current = form;
          return (
            <StyledSettings>
              <FormControlLabel
                name="usingTranslationMemory"
                data-cy="languages-auto-translation-memory"
                label={t(
                  'project_languages_new_keys_translation_memory_switch',
                  'Enable translation memory'
                )}
                checked={form.values.usingTranslationMemory}
                onChange={form.handleChange}
                control={<Checkbox />}
              />
              <FormControlLabel
                name="usingMachineTranslation"
                data-cy="languages-auto-machine-translation"
                label={t(
                  'project_languages_new_keys_machine_translations_switch',
                  'Enable machine translation with primary provider'
                )}
                checked={form.values.usingMachineTranslation}
                onChange={form.handleChange}
                control={<Checkbox />}
                disabled={!mtEnabled}
              />
            </StyledSettings>
          );
        }}
      </Formik>
      <StyledLoadingWrapper>
        <SmoothProgress loading={isUpdating} />
      </StyledLoadingWrapper>
    </StyledContainer>
  ) : null;
};

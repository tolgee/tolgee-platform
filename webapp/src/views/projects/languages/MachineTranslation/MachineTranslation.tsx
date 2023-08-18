import { useEffect, useRef, useState } from 'react';
import { styled } from '@mui/material';
import { Formik, FormikProps } from 'formik';

import { components } from 'tg.service/apiSchema.generated';
import { StyledLanguageTable } from '../tableStyles';
import { ExpandLess, ExpandMore } from '@mui/icons-material';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useMachineTranslationSettings } from './useMachineTranslationSettings';
import { SettingsForm } from './SettingsForm';

type MachineTranslationLanguagePropsDto =
  components['schemas']['MachineTranslationLanguagePropsDto'];

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
`;

const StyledToggle = styled('div')`
  display: flex;
  justify-content: center;
  grid-column: 1 / -1;
  cursor: pointer;
  background-color: ${({ theme }) => theme.palette.emphasis[100]};
  transition: background-color 0.1s ease-in-out;
  &:active,
  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[200]};
  }
`;

const StyledLoadingWrapper = styled('div')`
  position: absolute;
  top: 0px;
  left: 0px;
  right: 0px;
`;

export const MachineTranslation = () => {
  const formRef = useRef<FormikProps<any>>();
  const [expanded, setExpanded] = useState(false);
  const [formInstance, setFormInstance] = useState(0);

  const {
    settings,
    languages,
    updateSettings,
    providers,
    baseSetting,
    applyUpdate,
  } = useMachineTranslationSettings({
    // completely reset form (by creating new instance)
    onReset: () => setFormInstance((i) => i + 1),
  });

  const isUpdating = updateSettings.isLoading;

  const formatLangSettings = (
    lang: string | null,
    enabled: string[],
    primary: string | null | undefined
  ): MachineTranslationLanguagePropsDto | null => {
    const targetLanguageId = lang
      ? languages.data?._embedded?.languages?.find((l) => l.tag === lang)?.id
      : null;

    if (primary === 'default') {
      return null;
    }

    return {
      targetLanguageId: targetLanguageId as any,
      primaryService: (primary === 'none' ? null : primary) as any,
      enabledServices: enabled as any,
    };
  };

  const langDefaults: Record<
    string,
    { enabled: string[]; primary?: string | null }
  > = {};

  languages.data?._embedded?.languages?.forEach((lang) => {
    const config = settings.data?._embedded?.languageConfigs?.find(
      (langSettings) => langSettings.targetLanguageId === lang.id
    );

    langDefaults[lang.tag] = {
      enabled: config?.enabledServices || baseSetting?.enabledServices || [],
      primary: config ? config.primaryService || 'none' : 'default',
    };
  });

  const initialValues = {
    default: {
      enabled: baseSetting?.enabledServices,
      primary: baseSetting?.primaryService || 'none',
    },
    languages: langDefaults,
  };

  const submit = (values: typeof initialValues) => {
    const languageSettings = Object.entries(values.languages).map(
      ([lang, value]) => formatLangSettings(lang, value.enabled, value.primary)
    );

    languageSettings.push(
      formatLangSettings(null, values.default.enabled!, values.default.primary)
    );

    applyUpdate({
      settings: languageSettings.filter(
        Boolean
      ) as MachineTranslationLanguagePropsDto[],
    });
  };

  const gridTemplateColumns = `1fr ${providers
    .map(() => 'auto')
    .join(' ')} auto`;

  useEffect(() => {
    formRef.current?.resetForm();
  }, [settings.data]);

  useEffect(() => {
    if (Number(settings.data?._embedded?.languageConfigs?.length) > 1) {
      setExpanded(true);
    }
  }, [settings.data]);

  const languagesCount = languages.data?._embedded?.languages?.length || 0;

  return (
    <>
      {settings.data && languages.data && (
        <StyledContainer>
          <StyledLanguageTable style={{ gridTemplateColumns }}>
            <Formik
              key={formInstance}
              initialValues={initialValues}
              enableReinitialize={true}
              onSubmit={() => {}}
              validate={submit}
              validateOnChange
            >
              {(form) => {
                formRef.current = form;
                return (
                  <SettingsForm
                    providers={providers}
                    expanded={expanded}
                    languages={languages.data}
                  />
                );
              }}
            </Formik>
            {languagesCount > 1 && (
              <StyledToggle
                role="button"
                onClick={() => setExpanded((expanded) => !expanded)}
              >
                {expanded ? <ExpandLess /> : <ExpandMore />}
              </StyledToggle>
            )}
            <StyledLoadingWrapper>
              <SmoothProgress loading={isUpdating} />
            </StyledLoadingWrapper>
          </StyledLanguageTable>
        </StyledContainer>
      )}
    </>
  );
};

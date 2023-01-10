import { useEffect, useRef, useState } from 'react';
import { Box, Link as MuiLink, styled, Typography } from '@mui/material';
import { Formik, FormikProps } from 'formik';
import { useTranslate, T } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { components } from 'tg.service/apiSchema.generated';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { StyledLanguageTable } from '../tableStyles';
import { ExpandLess, ExpandMore } from '@mui/icons-material';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useMachineTranslationSettings } from './useMachineTranslationSettings';
import { SettingsForm } from './SettingsForm';
import { useConfig, usePreferredOrganization } from 'tg.globalContext/helpers';
import { LINKS, PARAMS } from 'tg.constants/links';
import { MtHint } from 'tg.component/billing/MtHint';

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

const StyledHint = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
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
  const { t } = useTranslate();
  const [formInstance, setFormInstance] = useState(0);

  const {
    settings,
    languages,
    creditBalance,
    updateSettings,
    providers,
    baseSetting,
    applyUpdate,
  } = useMachineTranslationSettings({
    // completely reset form (by creating new instance)
    onReset: () => setFormInstance((i) => i + 1),
  });

  const isFetching =
    settings.isFetching || languages.isFetching || creditBalance.isFetching;

  useGlobalLoading(isFetching);

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

  const { preferredOrganization } = usePreferredOrganization();
  const config = useConfig();

  const languagesCount = languages.data?._embedded?.languages?.length || 0;

  const params = {
    link: (
      <MuiLink
        component={Link}
        to={LINKS.ORGANIZATION_BILLING.build({
          [PARAMS.ORGANIZATION_SLUG]: preferredOrganization.slug,
        })}
      />
    ),
    hint: <MtHint />,
  };

  return (
    <>
      {settings.data && languages.data && creditBalance.data && (
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
          {creditBalance.data.creditBalance !== -1 && (
            <Box my={1} display="flex" flexDirection="column">
              <Typography variant="body1">
                {t('project_languages_credit_balance', {
                  balance: String(
                    Math.round(creditBalance.data.creditBalance / 100)
                  ),
                })}
              </Typography>
              <StyledHint variant="caption">
                {t('project_languages_credit_balance_help')}{' '}
                {config.billing.enabled ? (
                  preferredOrganization.currentUserRole === 'OWNER' ? (
                    <T
                      keyName="project_languages_credit_balance_help_owner"
                      params={params}
                    />
                  ) : (
                    <T
                      keyName="project_languages_credit_balance_help_member"
                      params={params}
                    />
                  )
                ) : (
                  <T
                    keyName="project_languages_credit_balance_help_no_billing"
                    params={params}
                  />
                )}
              </StyledHint>
            </Box>
          )}
        </StyledContainer>
      )}
    </>
  );
};

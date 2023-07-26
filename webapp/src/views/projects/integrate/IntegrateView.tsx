import { default as React, FunctionComponent, useEffect } from 'react';
import { Box, Step, StepContent, StepLabel, Stepper } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { WeaponSelector } from 'tg.views/projects/integrate/component/WeaponSelector';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { ApiKeySelector } from 'tg.views/projects/integrate/component/ApiKeySelector';
import { MdxProvider } from 'tg.component/MdxProvider';
import { useIntegrateState } from 'tg.views/projects/integrate/useIntegrateState';
import { useProject } from 'tg.hooks/useProject';
import { BaseProjectView } from '../BaseProjectView';
import { useReportEvent } from 'tg.hooks/useReportEvent';

export const API_KEY_PLACEHOLDER = '{{{apiKey}}}';
export const IntegrateView: FunctionComponent = () => {
  const project = useProject();

  const {
    selectedWeapon,
    selectedApiKey,
    onSelectApiKey,
    onWeaponSelect,
    keys,
    keysLoading,
    onNewKeyCreated,
  } = useIntegrateState();

  const activeStep = !selectedWeapon ? 0 : !selectedApiKey ? 1 : 2;
  const { t } = useTranslate();

  const reportEvent = useReportEvent();
  useEffect(() => {
    reportEvent('INTEGRATE_VIEW');
  }, []);

  return (
    <BaseProjectView
      windowTitle={t('project_integrate_title')}
      navigation={[
        [
          t('project_integrate_title'),
          LINKS.PROJECT_INTEGRATE.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
      md={11}
      lg={9}
      containerMaxWidth="lg"
    >
      <Stepper activeStep={activeStep} orientation="vertical">
        <Step expanded={true}>
          <StepLabel data-cy="integrate-choose-your-weapon-step-label">
            <T keyName="integrate_choose_your_weapon" />
          </StepLabel>
          <StepContent data-cy="integrate-choose-your-weapon-step-content">
            <WeaponSelector
              selected={selectedWeapon}
              onSelect={onWeaponSelect}
            />
          </StepContent>
        </Step>
        <Step expanded={activeStep > 0}>
          <StepLabel data-cy="integrate-select-api-key-step-label">
            <T keyName="integrate_step_select_api_key" />
          </StepLabel>
          <StepContent data-cy="integrate-select-api-key-step-content">
            <ApiKeySelector
              selected={selectedApiKey}
              onSelect={onSelectApiKey}
              keys={keys}
              keysLoading={keysLoading}
              onNewCreated={onNewKeyCreated}
            />
          </StepContent>
        </Step>
        <Step>
          <StepLabel>
            <T keyName="integrate_step_integrate" />
          </StepLabel>
          <StepContent>
            <React.Suspense fallback={<BoxLoading />}>
              {selectedWeapon && selectedApiKey && (
                <Box data-cy="integrate-guide" mb={12}>
                  <MdxProvider
                    modifyValue={(code) => {
                      return code
                        ?.replace(
                          API_KEY_PLACEHOLDER,
                          selectedApiKey['key'] ||
                            t({
                              key: 'integrate-api-key-hidden-description',
                              params: {
                                description: selectedApiKey?.description,
                              },
                            })
                        )
                        .replace(
                          '{{{apiUrl}}}',
                          process.env.REACT_APP_API_URL ||
                            window.location.origin
                        );
                    }}
                  >
                    {React.createElement(selectedWeapon.guide)}
                  </MdxProvider>
                </Box>
              )}
            </React.Suspense>
          </StepContent>
        </Step>
      </Stepper>
    </BaseProjectView>
  );
};

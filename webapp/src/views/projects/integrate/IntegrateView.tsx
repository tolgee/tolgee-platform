import { default as React, FunctionComponent } from 'react';
import { Box, Step, StepContent, StepLabel, Stepper } from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';

import { BaseView } from 'tg.component/layout/BaseView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { WeaponSelector } from 'tg.views/projects/integrate/component/WeaponSelector';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { ApiKeySelector } from 'tg.views/projects/integrate/component/ApiKeySelector';
import { MdxProvider } from 'tg.component/MdxProvider';
import { useIntegrateState } from 'tg.views/projects/integrate/useIntegrateState';
import { useProject } from 'tg.hooks/useProject';

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
  const t = useTranslate();

  return (
    <BaseView
      windowTitle={t('project_integrate_title')}
      navigation={[
        [
          project.name,
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
        [
          <span key={''} data-cy={'integrate-navigation-title'}>
            <T>project_integrate_title</T>
          </span>,
          LINKS.PROJECT_EXPORT.build({
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
            <T>integrate_choose_your_weapon</T>
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
            <T>integrate_step_select_api_key</T>
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
            <T>integrate_step_integrate</T>
          </StepLabel>
          <StepContent>
            <React.Suspense fallback={<BoxLoading />}>
              {selectedWeapon && selectedApiKey && (
                <Box data-cy="integrate-guide">
                  <MdxProvider
                    modifyValue={(code) => {
                      return code
                        ?.replace(
                          API_KEY_PLACEHOLDER,
                          selectedApiKey?.key || ''
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
    </BaseView>
  );
};

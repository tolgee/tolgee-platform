import { default as React, FunctionComponent, useEffect } from 'react';
import {
  Box,
  Link,
  Step,
  StepContent,
  StepLabel,
  Stepper,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { WeaponSelector } from 'tg.views/projects/integrate/component/WeaponSelector';
import { ApiKeySelector } from 'tg.views/projects/integrate/component/ApiKeySelector';
import { MdxProvider } from 'tg.component/MdxProvider';
import { useIntegrateState } from 'tg.views/projects/integrate/useIntegrateState';
import { useProject } from 'tg.hooks/useProject';
import { BaseProjectView } from '../BaseProjectView';
import { useReportEvent } from 'tg.hooks/useReportEvent';
import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';

export const API_KEY_PLACEHOLDER = '{{{apiKey}}}';
const API_URL_PLACEHOLDER = '{{{apiUrl}}}';
const PROJECT_ID = '{{{projectId}}}';

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
      maxWidth="normal"
    >
      <Box my={2}>
        <T
          keyName="project_integrate_description"
          params={{
            link: (
              <Link
                href="https://tolgee.io/integrations"
                target="_blank"
                rel="noopener noreferrer"
              />
            ),
          }}
        />
      </Box>
      <Stepper
        activeStep={activeStep}
        orientation="vertical"
        sx={{ display: 'grid', maxWidth: '100%', contain: 'layout' }}
      >
        <Step expanded={true} sx={{ maxWidth: '100%' }}>
          <QuickStartHighlight
            itemKey="integrate_form"
            message={t('quick_start_item_integrate_form_hint')}
            offset={4}
            borderRadius="5px"
          >
            <Box>
              <StepLabel data-cy="integrate-choose-your-weapon-step-label">
                <T keyName="integrate_choose_your_weapon" />
              </StepLabel>
              <StepContent
                data-cy="integrate-choose-your-weapon-step-content"
                sx={{ pb: 1 }}
              >
                <WeaponSelector
                  selected={selectedWeapon}
                  onSelect={onWeaponSelect}
                />
              </StepContent>
            </Box>
          </QuickStartHighlight>
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
        <Step sx={{ overflowX: 'auto', maxWidth: '100%' }}>
          <StepLabel>
            <T keyName="integrate_step_integrate" />
          </StepLabel>
          <StepContent sx={{ maxWidth: '100%' }}>
            <React.Suspense fallback={<FullPageLoading />}>
              {selectedWeapon && selectedApiKey && (
                <Box data-cy="integrate-guide">
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
                          API_URL_PLACEHOLDER,
                          import.meta.env.VITE_APP_API_URL ||
                            window.location.origin
                        )
                        .replace(PROJECT_ID, String(project.id));
                    }}
                    content={selectedWeapon.guide}
                  />
                </Box>
              )}
            </React.Suspense>
          </StepContent>
        </Step>
      </Stepper>
    </BaseProjectView>
  );
};

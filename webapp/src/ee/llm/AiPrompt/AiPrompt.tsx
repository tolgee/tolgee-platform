import {
  Box,
  IconButton,
  MenuItem,
  Select,
  styled,
  Tab,
  Tabs,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Edit01, X } from '@untitled-ui/icons-react';

import { useTranslationsActions } from 'tg.views/projects/translations/context/TranslationsContext';
import { components } from 'tg.service/apiSchema.generated';
import { DeletableKeyWithTranslationsModelType } from 'tg.views/projects/translations/context/types';

import { AiResult } from './AiResult';
import { PromptLoadMenu } from './PromptLoadMenu';
import { PromptPreviewMenu } from './PromptPreviewMenu';
import { PromptSaveMenu } from './PromptSaveMenu';
import { TabAdvanced } from './TabAdvanced';
import { AiResultUsage } from './AiResultUsage';
import { AiRenderedPrompt } from './AiRenderedPrompt';
import { TabBasic } from './TabBasic';
import { PromptRename } from './PromptRename';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { usePromptState } from './usePromptState';
import { useState } from 'react';

type ProjectModel = components['schemas']['ProjectModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: grid;
  height: 100%;
  max-height: 100%;
  grid-template-rows: 1fr auto;
  overflow: auto;
`;

const StyledMainContent = styled('div')`
  display: grid;
  align-self: start;
  margin-bottom: 50px;
`;

const StyledHeader = styled('div')`
  display: grid;
  margin: 16px 20px 0px 20px;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider};
  gap: 16px;
`;

const StyledTitle = styled('div')`
  display: flex;
  align-items: center;
  justify-content: space-between;
  &:hover .editButton {
    opacity: 1;
  }
  .editButton {
    opacity: 0;
    transition: opacity 0.2s ease-in-out;
  }
`;

const StyledTitleText = styled('div')`
  font-size: 20px;
  font-weight: 400;
`;

const StyledPromptName = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;
`;

const StyledTab = styled(Tab)`
  padding: 9px 16px;
  min-height: 42px;
`;

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
  min-height: unset;
`;

const StyledActionsWrapper = styled('div')`
  padding: 12px 20px;
  display: flex;
  gap: 8px;
  justify-content: space-between;
  align-items: end;
  position: sticky;
  bottom: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  box-shadow: 0px -4px 6px 0px rgba(0, 0, 0, 0.08);
`;

type Props = {
  width: number;
  project: ProjectModel;
  language: LanguageModel;
  keyData: DeletableKeyWithTranslationsModelType;
};

export const AiPrompt: React.FC<Props> = ({
  width,
  project,
  language,
  keyData,
}) => {
  const { t } = useTranslate();
  const { refetchTranslations } = useTranslationsActions();
  const [renameOpen, setRenameOpen] = useState(false);
  const cellSelected = Boolean(keyData && language);

  const {
    isLoading,
    canBeRenamed,
    openPromptData,
    setAiPlayground,
    errors,
    confirmUnsaved,
    updateForm,
    setOpenPrompt,
    provider,
    setProvider,
    providers,
    tab,
    setTab,
    featureEnabled,
    value,
    setValue,
    handleTestPrompt,
    variables,
    options,
    setOptions,
    runData,
    runIsLoading,
    usage,
    unsavedChanges,
  } = usePromptState({
    language,
    keyData,
  });

  if (isLoading && !openPromptData) {
    return (
      <Box display="flex" justifyContent="center" mt={3}>
        <BoxLoading />
      </Box>
    );
  }

  return (
    <StyledContainer>
      <StyledMainContent>
        <StyledHeader>
          <StyledTitle>
            <StyledPromptName>
              <StyledTitleText data-cy="ai-prompt-name">
                {openPromptData?.id === undefined
                  ? t('ai_prompt_default_name')
                  : openPromptData.name}
              </StyledTitleText>
              {canBeRenamed && openPromptData && (
                <IconButton
                  onClick={() => setRenameOpen(true)}
                  className="editButton"
                  data-cy="ai-prompt-provider-rename-button"
                >
                  <Edit01 width={20} height={20} />
                </IconButton>
              )}
            </StyledPromptName>
            <Box display="flex" alignItems="center">
              <PromptLoadMenu
                projectId={project.id}
                onSelect={(data) =>
                  confirmUnsaved(() => {
                    updateForm(data, true);
                    setOpenPrompt(data.id);
                  })
                }
              />

              <IconButton
                data-cy="ai-prompt-playground-close"
                onClick={() =>
                  confirmUnsaved(() => {
                    setAiPlayground(undefined);
                    setOpenPrompt(undefined);
                  })
                }
              >
                <X />
              </IconButton>
            </Box>
          </StyledTitle>
          <Select
            size="small"
            value={provider}
            onChange={(e) => setProvider(e.target.value)}
            sx={{ width: '50%' }}
            data-cy="ai-prompt-provider-select"
          >
            {providers?._embedded?.providers?.map((i) => (
              <MenuItem
                key={i.name}
                value={i.name}
                data-cy="ai-prompt-provider-item"
              >
                {i.name}
              </MenuItem>
            ))}
          </Select>
          <StyledTabs value={tab} onChange={(_, value) => setTab(value)}>
            <StyledTab
              label={t('ai_prompt_tab_basic')}
              value="basic"
              data-cy="ai-prompt-tab-basic"
            />
            <StyledTab
              label={t('ai_prompt_tab_advanced')}
              value="advanced"
              data-cy="ai-prompt-tab-advanced"
            />
          </StyledTabs>
        </StyledHeader>
        {tab === 'advanced' ? (
          featureEnabled && (
            <TabAdvanced
              value={value ?? ''}
              onChange={setValue}
              onRun={handleTestPrompt}
              availableVariables={variables?.data}
              errors={errors}
              cellSelected={cellSelected}
            />
          )
        ) : (
          <TabBasic value={options} onChange={setOptions} />
        )}
        {featureEnabled && (
          <Box sx={{ margin: '20px', display: 'grid', gap: 1.5 }}>
            <AiResult
              raw={runData?.result}
              json={runData?.parsedJson as any}
              isPlural={keyData?.keyIsPlural}
              locale={language?.tag}
              loading={runIsLoading}
            />
            {usage && <AiResultUsage usage={usage} price={runData?.price} />}
          </Box>
        )}
        {featureEnabled && (
          <Box sx={{ margin: '20px', display: 'grid' }}>
            <AiRenderedPrompt data={runData?.prompt} loading={runIsLoading} />
          </Box>
        )}
        {!featureEnabled && (
          <Box sx={{ margin: '20px', display: 'grid' }}>
            <DisabledFeatureBanner
              customMessage={t('ai_customization_not_enabled_message')}
            />
          </Box>
        )}
      </StyledMainContent>
      <StyledActionsWrapper>
        <PromptPreviewMenu
          projectId={project.id}
          languageId={language?.id}
          templateValue={tab === 'advanced' ? value : undefined}
          options={tab === 'basic' ? options : undefined}
          providerName={provider}
          onBatchFinished={() => {
            refetchTranslations();
          }}
          onTestPrompt={handleTestPrompt}
          loading={runIsLoading}
          disabled={!featureEnabled}
        />
        <PromptSaveMenu
          projectId={project.id}
          data={{
            providerName: provider,
            template: tab === 'advanced' ? value : undefined,
            basicPromptOptions: tab === 'basic' ? options : undefined,
          }}
          existingPrompt={openPromptData}
          unsavedChanges={unsavedChanges}
          disabled={!featureEnabled}
          onSuccess={(data) => updateForm(data, true)}
        />
      </StyledActionsWrapper>
      {renameOpen && openPromptData && (
        <PromptRename
          data={openPromptData}
          projectId={project.id}
          onClose={() => setRenameOpen(false)}
        />
      )}
    </StyledContainer>
  );
};

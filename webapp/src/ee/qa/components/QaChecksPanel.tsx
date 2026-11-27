import { FC, useEffect, useMemo, useState } from 'react';
import { LinearProgress, styled } from '@mui/material';
import { T } from '@tolgee/react';
import {
  PanelContentData,
  PanelContentProps,
} from 'tg.views/projects/translations/ToolsPanel/common/types';
import { TabMessage } from 'tg.views/projects/translations/ToolsPanel/common/TabMessage';
import { useQaChecksForPanel } from '../hooks/useQaChecksForPanel';
import { QaPreviewIssue } from 'tg.ee.module/qa/models/QaPreviewWsModels';
import { useProject } from 'tg.hooks/useProject';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { QaCheckItem } from './QaCheckItem';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useReportEvent } from 'tg.hooks/useReportEvent';
import { LinkExternal } from 'tg.component/LinkExternal';
import { applyQaReplacement } from 'tg.fixtures/qaUtils';

const StyledWrapper = styled('div')`
  margin-top: 4px;
`;

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(0, 0.5)};
`;

const StyledLinearProgress = styled(LinearProgress)`
  height: 2px;
  margin-bottom: -2px;
`;

export const useQaChecksCount = (data: PanelContentData) => {
  const translation = data.keyData.translations[data.language.tag];
  return translation?.qaIssueCount ?? 0;
};

function isCorrectable(
  issue: QaPreviewIssue,
  keyIsPlural: boolean,
  activeVariant: string | undefined
): boolean {
  if (keyIsPlural && issue.pluralVariant !== activeVariant) {
    return false;
  }
  return (
    issue.replacement != null &&
    issue.positionStart != null &&
    issue.positionEnd != null &&
    issue.state === 'OPEN'
  );
}

export const QaChecksPanel: FC<React.PropsWithChildren<PanelContentProps>> = (
  data
) => {
  const { isEnabled } = useEnabledFeatures();
  const { issues, isLoading, isDisconnected, toggleIgnore } =
    useQaChecksForPanel(data);
  const text = data.editingText ?? '';
  const project = useProject();
  const reportEvent = useReportEvent();

  const [showProgress, setShowProgress] = useState(false);

  useEffect(() => {
    // Debounce progress indicator
    setShowProgress(false);
    if (isLoading) {
      const timeout = setTimeout(() => {
        setShowProgress(true);
      }, 400);
      return () => clearTimeout(timeout);
    }
  }, [isLoading, text]);

  const openIssueCount = useMemo(
    () => issues.filter((i) => i.state !== 'IGNORED').length,
    [issues]
  );

  useEffect(() => {
    data.setItemsCount(openIssueCount);
  }, [openIssueCount, data.setItemsCount]);

  if (!isEnabled('QA_CHECKS')) {
    return (
      <StyledWrapper>
        <StyledContainer data-cy="qa-panel-container-disabled">
          <TabMessage>
            <T keyName="translation_tools_qa_not_available" />
          </TabMessage>
        </StyledContainer>
      </StyledWrapper>
    );
  }

  const qaSettingsLink = (
    <LinkExternal
      href={LINKS.PROJECT_EDIT_QA.build({
        [PARAMS.PROJECT_ID]: project.id,
      })}
    />
  );

  if (!project.useQaChecks) {
    return (
      <StyledWrapper>
        <StyledContainer data-cy="qa-panel-container-project-disabled">
          <TabMessage>
            <T
              keyName="translation_tools_qa_disabled_for_project"
              params={{ link: qaSettingsLink }}
            />
          </TabMessage>
        </StyledContainer>
      </StyledWrapper>
    );
  }

  if (issues.length === 0) {
    return (
      <StyledWrapper>
        {showProgress && <StyledLinearProgress />}
        <StyledContainer data-cy="qa-panel-container-empty">
          <TabMessage>
            <T
              keyName="translation_tools_qa_no_issues_with_settings"
              params={{ link: qaSettingsLink }}
            />
          </TabMessage>
        </StyledContainer>
      </StyledWrapper>
    );
  }

  const handleCorrect = (issue: QaPreviewIssue) => {
    data.setValue(applyQaReplacement(text, issue));
    reportEvent('QA_ISSUE_CORRECTED_INLINE', { checkType: issue.type });
  };

  return (
    <StyledWrapper>
      {showProgress && <StyledLinearProgress />}
      <StyledContainer data-cy="qa-panel-container">
        {isDisconnected && (
          <TabMessage>
            <T keyName="translation_tools_qa_connection_lost" />
          </TabMessage>
        )}
        {issues.map((issue, index) => (
          <QaCheckItem
            key={`${issue.type}-${issue.message}-${
              issue.positionStart ?? 'x'
            }-${issue.positionEnd ?? 'x'}-${issue.pluralVariant ?? ''}`}
            issue={issue}
            index={index + 1}
            text={text}
            locale={data.language.tag}
            slim
            disabled={
              data.activeVariant !== undefined &&
              issue.pluralVariant !== undefined &&
              issue.pluralVariant !== data.activeVariant
            }
            onCorrect={
              isCorrectable(issue, data.keyData.keyIsPlural, data.activeVariant)
                ? () => handleCorrect(issue)
                : undefined
            }
            onIgnore={() => toggleIgnore(issue)}
          />
        ))}
      </StyledContainer>
    </StyledWrapper>
  );
};

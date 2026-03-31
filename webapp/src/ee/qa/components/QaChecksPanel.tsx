import { FC, useEffect, useMemo, useState } from 'react';
import { LinearProgress, styled } from '@mui/material';
import { T } from '@tolgee/react';
import {
  PanelContentData,
  PanelContentProps,
} from 'tg.views/projects/translations/ToolsPanel/common/types';
import { TabMessage } from 'tg.views/projects/translations/ToolsPanel/common/TabMessage';
import { useQaChecksForPanel } from '../hooks/useQaChecksForPanel';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { QaCheckItem } from './QaCheckItem';
import { applyQaReplacement } from 'tg.fixtures/qaUtils';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useReportEvent } from 'tg.hooks/useReportEvent';
import { LinkExternal } from 'tg.component/LinkExternal';

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

function isCorrectable(issue: {
  replacement?: string | null;
  positionStart?: number | null;
  positionEnd?: number | null;
  state: string;
}) {
  return (
    issue.replacement != null &&
    issue.positionStart != null &&
    issue.positionEnd != null &&
    issue.state === 'OPEN'
  );
}

export const QaChecksPanel: FC<PanelContentProps> = (data) => {
  const { isEnabled } = useEnabledFeatures();
  const { issues, isLoading, isDisconnected, updateIssueState } =
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

  const ignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/suppressions',
    method: 'post',
  });

  const unignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/suppressions',
    method: 'delete',
  });

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

  const handleCorrect = (issue: (typeof issues)[0]) => {
    data.setValue(applyQaReplacement(text, issue));
    reportEvent('QA_ISSUE_CORRECTED_INLINE', { checkType: issue.type });
  };

  const handleIgnoreToggle = (issue: (typeof issues)[0]) => {
    const translationId = data.keyData.translations[data.language.tag]?.id;
    if (translationId == null) return;

    const newState = issue.state === 'IGNORED' ? 'OPEN' : 'IGNORED';
    const mutation =
      issue.state === 'IGNORED' ? unignoreMutation : ignoreMutation;
    const { state: _, ...issueRequest } = issue;
    mutation.mutate(
      {
        path: {
          projectId: project.id,
          translationId,
        },
        content: {
          'application/json': issueRequest,
        },
      },
      {
        onSuccess: () => updateIssueState(issue, newState),
      }
    );
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
        {data.activeVariant && (
          <TabMessage>
            <T keyName="translation_tools_qa_plural_variant_note" />
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
            slim
            onCorrect={
              isCorrectable(issue) ? () => handleCorrect(issue) : undefined
            }
            onIgnore={() => handleIgnoreToggle(issue)}
          />
        ))}
      </StyledContainer>
    </StyledWrapper>
  );
};

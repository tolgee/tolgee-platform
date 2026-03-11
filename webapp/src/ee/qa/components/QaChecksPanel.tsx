import React, { useEffect, useState } from 'react';
import { LinearProgress, styled } from '@mui/material';
import { T } from '@tolgee/react';
import {
  PanelContentData,
  PanelContentProps,
} from 'tg.views/projects/translations/ToolsPanel/common/types';
import { TabMessage } from 'tg.views/projects/translations/ToolsPanel/common/TabMessage';
import { useQaCheckPreview } from '../hooks/useQaCheckPreview';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { QaCheckItem } from './QaCheckItem';

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

const useQaChecksForPanel = (data: PanelContentData) => {
  const { keyData, language, editingText } = data;
  const text = editingText ?? '';
  // TODO: When user is editing plural, either use full ICU form for the plural, or don't generate QA issues, or something like that
  // if we go with no live QA for plurals, then the UI should say so.

  return useQaCheckPreview({
    text,
    languageTag: language.tag,
    keyId: keyData.keyId,
  });
};

export const useQaChecksCount = (data: PanelContentData) => {
  const translation = data.keyData.translations[data.language.tag];
  return translation?.qaIssueCount ?? 0;
};

export const QaChecksPanel: React.FC<PanelContentProps> = (data) => {
  const { issues, isLoading } = useQaChecksForPanel(data);
  const text = data.editingText ?? '';
  const project = useProject();

  const [showProgress, setShowProgress] = useState(false);

  useEffect(() => {
    setShowProgress(false);
    if (isLoading) {
      const timeout = setTimeout(() => {
        setShowProgress(true);
      }, 400);
      return () => clearTimeout(timeout);
    }
  }, [isLoading, text]);

  const openIssues = issues.filter((i) => i.state !== 'IGNORED');
  useEffect(() => {
    data.setItemsCount(openIssues.length);
  }, [openIssues.length]);

  const ignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/ignore',
    method: 'post',
  });

  const unignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/unignore',
    method: 'post',
  });

  // TODO: different message when QA feature is not enabled

  if (issues.length === 0) {
    return (
      <StyledWrapper>
        {showProgress && <StyledLinearProgress />}
        <StyledContainer data-cy="qa-panel-container-empty">
          <TabMessage>
            <T keyName="translation_tools_qa_no_issues" />
          </TabMessage>
        </StyledContainer>
      </StyledWrapper>
    );
  }

  // TODO: add note that some QA checks are not supported for plurals yes (when editing plural)

  const handleCorrect = (issue: (typeof issues)[0]) => {
    if (issue.replacement == null) return;
    const corrected =
      text.slice(0, issue.positionStart) +
      issue.replacement +
      text.slice(issue.positionEnd);
    data.setValue(corrected);
  };

  const handleIgnoreToggle = (issue: (typeof issues)[0]) => {
    const translationId = data.keyData.translations[data.language.tag]?.id;
    if (translationId == null) return;

    const mutation =
      issue.state === 'IGNORED' ? unignoreMutation : ignoreMutation;
    mutation.mutate({
      path: {
        projectId: project!.id,
        translationId,
      },
      content: {
        'application/json': issue as any,
      },
    });
  };

  return (
    <StyledWrapper>
      {showProgress && <StyledLinearProgress />}
      <StyledContainer data-cy="qa-panel-container">
        {issues.map((issue, index) => (
          <QaCheckItem
            key={`${issue.type}-${index}`}
            issue={issue}
            index={index + 1}
            text={text}
            slim={true}
            onCorrect={
              issue.replacement != null && issue.state === 'OPEN'
                ? () => handleCorrect(issue)
                : undefined
            }
            onIgnore={() => handleIgnoreToggle(issue)}
          />
        ))}
      </StyledContainer>
    </StyledWrapper>
  );
};

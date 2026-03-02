import React from 'react';
import { styled } from '@mui/material';
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

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin-top: 4px;
  gap: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(0, 0.5)};
`;

const useQaChecksForPanel = (data: PanelContentData) => {
  const { keyData, language, editingText } = data;
  const text = editingText ?? '';

  return useQaCheckPreview({
    text,
    languageTag: language.tag,
    keyId: keyData.keyId,
  });
};

export const QaChecksPanel: React.FC<PanelContentProps> = (data) => {
  const issues = useQaChecksForPanel(data);
  const text = data.editingText ?? '';
  const project = useProject();

  const ignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/{issueId}/ignore',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/qa-check/preview',
  });

  const unignoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/qa-issues/{issueId}/unignore',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/qa-check/preview',
  });

  if (issues.length === 0) {
    return (
      <StyledContainer data-cy="qa-panel-container-empty">
        <TabMessage>
          <T keyName="translation_tools_qa_no_issues" />
        </TabMessage>
      </StyledContainer>
    );
  }

  const handleIgnoreToggle = (issue: (typeof issues)[0]) => {
    const persistedIssueId = issue.persistedIssueId;
    if (persistedIssueId == null) return;

    const translationId = data.keyData.translations[data.language.tag]?.id;
    if (translationId == null) return;

    const mutation = issue.ignored ? unignoreMutation : ignoreMutation;
    mutation.mutate({
      path: {
        projectId: project!.id,
        translationId,
        issueId: persistedIssueId,
      },
    });
  };

  return (
    <StyledContainer data-cy="qa-panel-container">
      {issues.map((issue, index) => (
        <QaCheckItem
          key={`${issue.type}-${index}`}
          issue={issue}
          index={index + 1}
          text={text}
          slim={true}
          onCorrect={
            issue.replacement != null && !issue.ignored
              ? () => {
                  const corrected =
                    text.slice(0, issue.positionStart) +
                    issue.replacement +
                    text.slice(issue.positionEnd);
                  data.setValue(corrected);
                }
              : undefined
          }
          onIgnore={
            issue.persistedIssueId != null
              ? () => handleIgnoreToggle(issue)
              : undefined
          }
        />
      ))}
    </StyledContainer>
  );
};

export const useQaChecksCount = (data: PanelContentData) =>
  useQaChecksForPanel(data).length;

import React from 'react';
import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import {
  PanelContentData,
  PanelContentProps,
} from 'tg.views/projects/translations/ToolsPanel/common/types';
import { TabMessage } from 'tg.views/projects/translations/ToolsPanel/common/TabMessage';
import { useQaCheckPreview } from '../hooks/useQaCheckPreview';
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

  if (issues.length === 0) {
    return (
      <StyledContainer data-cy="qa-panel-container-empty">
        <TabMessage>
          <T keyName="translation_tools_qa_no_issues" />
        </TabMessage>
      </StyledContainer>
    );
  }

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
            issue.replacement != null
              ? () => {
                  const corrected =
                    text.slice(0, issue.positionStart) +
                    issue.replacement +
                    text.slice(issue.positionEnd);
                  data.setValue(corrected);
                }
              : undefined
          }
        />
      ))}
    </StyledContainer>
  );
};

export const useQaChecksCount = (data: PanelContentData) =>
  useQaChecksForPanel(data).length;

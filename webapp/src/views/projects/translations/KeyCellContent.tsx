import React from 'react';
import { styled } from '@mui/material';
import ReactMarkdown from 'react-markdown';

import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { MarkdownLink } from 'tg.component/common/MarkdownLink';

export const StyledKey = styled('div')`
  grid-area: key;
  margin: 12px 12px 8px 12px;
  overflow: hidden;
  position: relative;
`;

export const StyledDescription = styled('div')`
  grid-area: description;
  padding: 0px 12px 8px 12px;
  font-size: 13px;
  color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[300]
      : theme.palette.emphasis[500]};
`;

type Props = {
  keyName: string;
  description?: string | null;
  width?: string | number;
};

export const KeyCellContent: React.FC<Props> = ({
  keyName,
  description,
  width,
}) => {
  return (
    <>
      <StyledKey data-cy="translations-key-name">
        <LimitedHeightText width={width} maxLines={3} wrap="break-all">
          {keyName}
        </LimitedHeightText>
      </StyledKey>
      {description && (
        <StyledDescription data-cy="translations-key-cell-description">
          <LimitedHeightText maxLines={5}>
            <ReactMarkdown
              components={{
                a: MarkdownLink,
              }}
            >
              {description}
            </ReactMarkdown>
          </LimitedHeightText>
        </StyledDescription>
      )}
    </>
  );
};

import React from 'react';
import { Box, styled } from '@mui/material';
import ReactMarkdown from 'react-markdown';
import clsx from 'clsx';

import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { MarkdownLink } from 'tg.component/common/MarkdownLink';
import { CELL_PLAIN } from 'tg.views/projects/translations/cell/styles';
import { BranchMergeKeyModel } from '../../types';

const StyledKey = styled('div')`
  overflow: hidden;
  position: relative;
`;

const StyledDescription = styled('div')`
  font-size: 13px;
  color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[300]
      : theme.palette.emphasis[500]};
`;

const Markdown = styled('div')`
  p {
    margin-block: 0;
  }
`;

type Props = {
  data: BranchMergeKeyModel;
};

export const MergeKeyHeader: React.FC<Props> = ({ data }) => {
  return (
    <Box
      display="flex"
      justifyContent="flex-start"
      flexDirection="column"
      data-cy="translations-table-cell"
      tabIndex={0}
      className={clsx({
        [CELL_PLAIN]: true,
      })}
    >
      <StyledKey data-cy="translations-key-name">
        <LimitedHeightText maxLines={3} wrap="break-all">
          {data.keyName}
        </LimitedHeightText>
      </StyledKey>
      {data.keyDescription && (
        <StyledDescription data-cy="translations-key-cell-description">
          <LimitedHeightText maxLines={5} lineHeight="auto">
            <Markdown>
              <ReactMarkdown
                components={{
                  a: MarkdownLink,
                }}
              >
                {data.keyDescription}
              </ReactMarkdown>
            </Markdown>
          </LimitedHeightText>
        </StyledDescription>
      )}
    </Box>
  );
};

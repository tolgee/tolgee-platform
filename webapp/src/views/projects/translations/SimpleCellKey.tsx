import React from 'react';
import { Box, styled } from '@mui/material';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { components } from 'tg.service/apiSchema.generated';

import { CELL_PLAIN } from './cell/styles';
import { Screenshots } from './Screenshots/Screenshots';
import ReactMarkdown from 'react-markdown';
import { MarkdownLink } from 'tg.component/common/MarkdownLink';
import clsx from 'clsx';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

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
const StyledScreenshots = styled('div')`
  position: relative;
  overflow: hidden;
  margin-top: -12px;
  padding-bottom: 8px;
`;

const Markdown = styled('div')`
  p {
    margin-block: 0;
  }
`;

type Props = {
  data: KeyWithTranslationsModel;
};

export const SimpleCellKey: React.FC<Props> = ({ data }) => {
  return (
    <Box
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
      {data.screenshots && (
        <StyledScreenshots>
          <Screenshots screenshots={data.screenshots} keyId={data.keyId} />
        </StyledScreenshots>
      )}
    </Box>
  );
};

import React from 'react';
import { Box, styled } from '@mui/material';
import ReactMarkdown from 'react-markdown';
import clsx from 'clsx';
import { MinusCircle, PlusCircle } from '@untitled-ui/icons-react';

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
  variant?: 'added' | 'deleted';
};

const HeaderRow = styled('div')`
  display: flex;
  align-items: flex-start;
  gap: ${({ theme }) => theme.spacing(1.5)};
`;

const IconWrapper = styled('div')<{ variant: 'added' | 'deleted' }>`
  display: flex;
  align-items: center;
  color: ${({ theme, variant }) =>
    variant === 'added'
      ? theme.palette.tokens.success.main
      : theme.palette.tokens.error.main};
`;

const TextColumn = styled('div')<{ strike?: boolean }>`
  display: flex;
  flex-direction: column;
  ${({ strike }) => (strike ? 'text-decoration: line-through;' : '')}
`;

export const MergeKeyHeader: React.FC<Props> = ({ data, variant }) => {
  const strike = variant === 'deleted';
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
      <HeaderRow>
        {variant && (
          <IconWrapper variant={variant}>
            {variant === 'added' ? (
              <PlusCircle width={22} height={22} />
            ) : (
              <MinusCircle width={22} height={22} />
            )}
          </IconWrapper>
        )}
        <TextColumn strike={strike}>
          <StyledKey data-cy="translations-key-name">
            <LimitedHeightText maxLines={3} wrap="break-all">
              {[data.namespace, data.keyName].filter(Boolean).join('.')}
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
        </TextColumn>
      </HeaderRow>
    </Box>
  );
};

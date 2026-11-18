import { useLayoutEffect, useRef, useState } from 'react';
import { Box, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import ReactMarkdown from 'react-markdown';

import { MarkdownLink } from 'tg.component/common/MarkdownLink';

const COLLAPSED_LINES = 3;

const StyledContent = styled(Box)`
  line-height: 1.5;
  & > * {
    margin-top: 0;
  }
  & > *:last-child {
    margin-bottom: 0;
  }
  &.collapsed {
    max-height: ${COLLAPSED_LINES * 1.5}em;
    overflow: hidden;
  }
  &.collapsed.overflowing {
    mask-image: linear-gradient(to top, transparent 0, black 1.5em);
  }
`;

export const StyledShowMore = styled('button')`
  border: none;
  background: none;
  cursor: pointer;
  font-family: inherit;
  padding: 0;
  justify-self: start;
  font-size: 13px;
  font-weight: ${({ theme }) => theme.typography.button.fontWeight};
  letter-spacing: 0.46px;
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};

  &:hover {
    color: ${({ theme }) => theme.palette.text.primary};
  }
`;

type Props = {
  text: string;
  'data-cy': string;
};

export const ExpandableMarkdown = ({ text, ...props }: Props) => {
  const { t } = useTranslate();
  const [expanded, setExpanded] = useState(false);
  const [overflowing, setOverflowing] = useState(false);
  const contentRef = useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    const element = contentRef.current;
    if (!element || expanded) {
      return;
    }
    const measure = () =>
      setOverflowing(element.scrollHeight > element.clientHeight + 1);
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => observer.disconnect();
  }, [text, expanded]);

  return (
    <Box display="grid" gap={1} data-cy={props['data-cy']}>
      <StyledContent
        ref={contentRef}
        className={
          expanded
            ? undefined
            : overflowing
            ? 'collapsed overflowing'
            : 'collapsed'
        }
      >
        <ReactMarkdown components={{ a: MarkdownLink }}>{text}</ReactMarkdown>
      </StyledContent>
      {(overflowing || expanded) && (
        <StyledShowMore
          type="button"
          onClick={() => setExpanded(!expanded)}
          data-cy="community-show-more"
        >
          {expanded
            ? t('community_show_less', 'Show less')
            : t('community_show_more', 'Show more')}
        </StyledShowMore>
      )}
    </Box>
  );
};

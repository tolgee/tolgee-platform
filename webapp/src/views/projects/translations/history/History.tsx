import React, { useEffect, useRef, useState } from 'react';
import { T } from '@tolgee/react';
import { FormControlLabel, styled, Switch } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { StickyDateSeparator } from 'tg.component/common/StickyDateSeparator';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import { HistoryItem } from './HistoryItem';

type TranslationViewModel = components['schemas']['TranslationViewModel'];
type TranslationHistoryModel = components['schemas']['TranslationHistoryModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  flex-basis: 100px;
  position: relative;
  contain: size;
  overflow: hidden;
`;

const StyledDifferenceToggle = styled(FormControlLabel)`
  position: absolute;
  right: 24px;
  top: 2px;
  z-index: 2;
  display: flex;
  align-items: start;
  & > span {
    font-size: 14px;
  }
`;

const StyledScroller = styled('div')`
  margin-top: -1px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
`;

const StyledProgressWrapper = styled('div')`
  position: absolute;
  bottom: 0px;
  left: 0px;
  right: 0px;
`;

const StyledLoadMore = styled('div')`
  display: flex;
  justify-content: center;
  align-items: flex-end;
  min-height: 50px;
`;

type Props = {
  language: LanguageModel;
  translation: TranslationViewModel | undefined;
};

export const History: React.FC<Props> = ({ translation, language }) => {
  const scrollerRef = useRef<HTMLDivElement>(null);
  const project = useProject();
  const counter = useDateCounter();

  const path = {
    projectId: project.id,
    translationId: translation?.id as number,
  };
  const query = {
    size: 20,
  };

  const fetchMore = () => {
    const previousHeight = Number(scrollerRef.current?.scrollHeight);
    history.fetchNextPage().then(() => {
      const newHeight = Number(scrollerRef.current?.scrollHeight);
      scrollerRef.current?.scrollTo({
        // persist scrolling position
        top: newHeight - previousHeight,
      });
    });
  };

  const history = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/translations/{translationId}/history',
    method: 'get',
    path,
    query,
    options: {
      enabled: Boolean(translation?.id),
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path,
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        } else {
          return null;
        }
      },
    },
  });

  const historyItems: TranslationHistoryModel[] = [];

  history.data?.pages.forEach((page) =>
    page._embedded?.revisions?.forEach((item) => historyItems.push(item))
  );

  historyItems.reverse();

  useEffect(() => {
    scrollerRef.current?.scrollTo({ top: scrollerRef.current.scrollHeight });
  }, [history.data?.pages?.[0].page?.totalElements]);

  const [showdifferences, setShowDifferences] = useState(true);
  const toggleDifferences = () => setShowDifferences((val) => !val);

  return (
    <StyledContainer>
      <StyledDifferenceToggle
        label={<T keyName="translations-history-differences-toggle" />}
        labelPlacement="start"
        control={
          <Switch
            size="small"
            checked={showdifferences}
            onChange={toggleDifferences}
          />
        }
      />
      <StyledScroller ref={scrollerRef}>
        {history.hasNextPage && (
          <StyledLoadMore>
            <LoadingButton
              onClick={fetchMore}
              loading={history.isFetchingNextPage}
              data-cy="translations-history-load-more-button"
            >
              <T>translations_history_load_more</T>
            </LoadingButton>
          </StyledLoadMore>
        )}
        {historyItems?.map((entry) => {
          const date = new Date(entry.timestamp);
          return (
            <React.Fragment key={entry.timestamp}>
              {counter.isNewDate(date) && <StickyDateSeparator date={date} />}
              <HistoryItem
                languageTag={language.tag}
                key={entry.timestamp}
                entry={entry}
                showDifferences={showdifferences}
              />
            </React.Fragment>
          );
        })}
      </StyledScroller>
      <StyledProgressWrapper>
        <SmoothProgress loading={history.isFetching} />
      </StyledProgressWrapper>
    </StyledContainer>
  );
};

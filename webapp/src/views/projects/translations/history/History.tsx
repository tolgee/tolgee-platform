import React from 'react';
import { T } from '@tolgee/react';
import { styled } from '@mui/material';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { HistoryItem } from './HistoryItem';

type TranslationViewModel = components['schemas']['TranslationViewModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  flex-grow: 1;
  flex-basis: 100px;
  overflow: hidden;
  position: relative;
`;

const StyledScrollerWrapper = styled('div')`
  flex-grow: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
`;

const StyledReverseScroller = styled('div')`
  display: flex;
  flex-direction: column-reverse;
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
  keyId: number;
  language: LanguageModel;
  translation: TranslationViewModel | undefined;
  onCancel: () => void;
  editEnabled: boolean;
};

export const History: React.FC<Props> = ({ keyId, language, translation }) => {
  const project = useProject();

  const path = {
    projectId: project.id,
    translationId: translation?.id as number,
  };
  const query = {
    size: 20,
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

  return (
    <StyledContainer>
      <StyledScrollerWrapper>
        <StyledReverseScroller>
          {history.data?.pages.map((page, i) => (
            <React.Fragment key={i}>
              {page._embedded?.revisions?.map((entry) => (
                <HistoryItem key={entry.timestamp} entry={entry} />
              ))}
            </React.Fragment>
          ))}

          {history.hasNextPage && (
            <StyledLoadMore>
              <LoadingButton
                onClick={() => history.fetchNextPage()}
                loading={history.isFetchingNextPage}
                data-cy="translations-history-load-more-button"
              >
                <T>translations_history_load_more</T>
              </LoadingButton>
            </StyledLoadMore>
          )}
        </StyledReverseScroller>
      </StyledScrollerWrapper>
      <StyledProgressWrapper>
        <SmoothProgress loading={history.isFetching} />
      </StyledProgressWrapper>
    </StyledContainer>
  );
};

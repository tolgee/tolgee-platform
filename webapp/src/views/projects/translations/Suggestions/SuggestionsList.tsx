import { styled } from '@mui/material';
import { PanelHeader } from '../ToolsPanel/common/PanelHeader';
import { useTranslate } from '@tolgee/react';
import { useLocalStorageState } from 'tg.hooks/useLocalStorageState';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationSuggestion } from './TranslationSuggestion';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useMemo } from 'react';

const OPEN_SUGGESTIONS_KEY = '__tolgee_suggestions_hidden';

const FETCH_NEXT_PAGE_SCROLL_THRESHOLD_IN_PIXELS = 220;

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

type TranslationSuggestionModel =
  components['schemas']['TranslationSuggestionModel'];

const StyledContainer = styled('div')`
  display: grid;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.tokens.text._states.hover};
`;

const StyledHeader = styled('div')`
  display: flex;
  justify-content: space-between;
  padding: 6px 12px;
`;

const StyledScrollWrapper = styled('div')`
  display: grid;
  max-height: 300px;
  overflow: auto;
  padding-bottom: 6px;
  border-radius: 0px 0px 8px 8px;
`;

const StyledItemsWrapper = styled('div')`
  display: grid;
  gap: 8px;
`;

const StyledTranslationSuggestion = styled(TranslationSuggestion)`
  padding-left: 12px;
  padding-right: 12px;
  transition: background-color 0.1s ease-in-out;

  &:hover {
    background-color: ${({ theme }) => theme.palette.divider};
  }
`;

type Props = {
  countContent: React.ReactNode;
  suggestions: TranslationSuggestionSimpleModel[];
  keyId: number;
  languageId: number;
  isPlural: boolean;
  locale: string;
};

export const SuggestionsList = ({
  countContent,
  suggestions,
  keyId,
  languageId,
  isPlural,
  locale,
}: Props) => {
  const project = useProject();
  const { t } = useTranslate();

  const query = {
    filterKeyId: [keyId],
    filterLanguageId: [languageId],
    sort: ['createdAt,desc'],
  };
  const projectId = project.id;

  const suggestionsLoadable = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/translation-suggestion',
    method: 'get',
    query,
    path: {
      projectId,
    },
    options: {
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { projectId },
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

  const handleFetchMore = () => {
    if (suggestionsLoadable.hasNextPage && !suggestionsLoadable.isFetching) {
      suggestionsLoadable.fetchNextPage();
    }
  };

  const suggestionsList = useMemo(() => {
    const result: TranslationSuggestionModel[] = [];
    suggestionsLoadable.data?.pages.forEach((page) =>
      page._embedded?.suggestions?.forEach((suggestion) => {
        result.push(suggestion);
      })
    );
    return result.length ? result : undefined;
  }, [suggestionsLoadable.data]);

  const [hidden, setHidden] = useLocalStorageState({
    key: OPEN_SUGGESTIONS_KEY,
    initial: undefined,
  });
  const panelId = 'suggestions';

  return (
    <StyledContainer>
      <StyledHeader>
        <PanelHeader
          sx={{ height: 'unset', padding: 0, background: 'unset' }}
          icon={null}
          name={t('translation_tools_suggestions')}
          countContent={countContent}
          onToggle={() => {
            setHidden((value) => (value ? undefined : 'true'));
          }}
          panelId={panelId}
          open={!hidden}
        />
      </StyledHeader>
      {!hidden && (
        <StyledScrollWrapper
          onScroll={(event) => {
            const target = event.target as HTMLDivElement;
            if (
              target.scrollHeight - target.clientHeight - target.scrollTop <
              FETCH_NEXT_PAGE_SCROLL_THRESHOLD_IN_PIXELS
            ) {
              handleFetchMore();
            }
          }}
        >
          <StyledItemsWrapper>
            {(suggestionsList || suggestions).map((item) => {
              const itemWithDate = item as Partial<TranslationSuggestionModel>;
              return (
                <StyledTranslationSuggestion
                  key={item.id}
                  suggestion={item}
                  isPlural={isPlural}
                  locale={locale}
                  lastUpdated={itemWithDate.updatedAt ?? itemWithDate.createdAt}
                />
              );
            })}
          </StyledItemsWrapper>
        </StyledScrollWrapper>
      )}
    </StyledContainer>
  );
};

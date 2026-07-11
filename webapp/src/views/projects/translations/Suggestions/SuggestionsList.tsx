import { useMemo, useState } from 'react';
import { Checkbox, FormControlLabel, styled } from '@mui/material';
import { PanelHeader } from '../ToolsPanel/common/PanelHeader';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationSuggestion } from './TranslationSuggestion';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { useTranslationsActions } from '../context/TranslationsContext';
import { messageService } from 'tg.service/MessageService';
import { useInfiniteSuggestions } from './useInfiniteSuggestions';
import { LabelHint } from 'tg.component/common/LabelHint';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useUser } from 'tg.globalContext/helpers';

const FETCH_NEXT_PAGE_SCROLL_THRESHOLD_IN_PIXELS = 220;

type TranslationViewModel = components['schemas']['TranslationViewModel'];

type TranslationSuggestionModel =
  components['schemas']['TranslationSuggestionModel'];

const StyledContainer = styled('div')`
  display: grid;
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.tokens.background.onDefaultGrey};
`;

const StyledMessage = styled('div')`
  display: grid;
  text-align: center;
  padding: 8px 0px;
  color: ${({ theme }) => theme.palette.text.secondary};
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
`;

const StyledTranslationSuggestion = styled(TranslationSuggestion)`
  padding-left: 12px;
  padding-right: 12px;
  transition: background-color 0.1s ease-in-out;

  &:hover {
    background-color: ${({ theme }) => theme.palette.tokens.action.selected};
  }
`;

type Props = {
  translation: TranslationViewModel;
  keyId: number;
  languageId: number;
  languageTag: string;
  isPlural: boolean;
  locale: string;
};

export const SuggestionsList = ({
  translation,
  keyId,
  languageId,
  languageTag,
  isPlural,
  locale,
}: Props) => {
  const project = useProject();
  const { t } = useTranslate();
  const { updateTranslation, setEditForce } = useTranslationsActions();
  const [showAll, setShowAll] = useState(false);
  const [hidden, setHidden] = useState(translation.activeSuggestionCount === 0);
  const { satisfiesLanguageAccess } = useProjectPermissions();
  const canReview = satisfiesLanguageAccess(
    'translations.state-edit',
    languageId
  );
  const user = useUser();

  const projectId = project.id;

  const declineLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion/{suggestionId}/decline',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion',
  });

  const reverseLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion/{suggestionId}/set-active',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion',
  });

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion/{suggestionId}',
    method: 'delete',
    invalidatePrefix:
      '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion',
  });

  const acceptLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion/{suggestionId}/accept',
    method: 'put',
    invalidatePrefix:
      '/v2/projects/{projectId}/languages/{languageId}/key/{keyId}/suggestion',
  });

  const activeSuggestions = useInfiniteSuggestions({
    projectId,
    keyId,
    languageId,
    filterState: ['ACTIVE'],
    expectedCount: translation.activeSuggestionCount,
    onSuccess(suggestions) {
      updateTranslation({
        keyId,
        lang: languageTag,
        data(translation) {
          const firstPage = suggestions.pages?.[0];
          const firstSuggestion = firstPage?._embedded?.suggestions?.[0];
          return {
            ...translation,
            suggestions: firstSuggestion ? [firstSuggestion] : [],
            activeSuggestionCount: firstPage.page?.totalElements ?? 0,
          };
        },
      });
    },
  });

  const allSuggestions = useInfiniteSuggestions({
    projectId,
    keyId,
    languageId,
    expectedCount: translation.totalSuggestionCount,
    enabled: showAll,
  });

  const allLoaded = allSuggestions.data;

  const suggestionsLoadable =
    showAll && allLoaded ? allSuggestions : activeSuggestions;

  function handleDecline(suggestionId: number) {
    declineLoadable.mutate({
      path: { projectId, keyId, languageId, suggestionId },
    });
  }

  function handleDelete(suggestionId: number) {
    deleteLoadable.mutate({
      path: { projectId, keyId, languageId, suggestionId },
    });
  }

  function handleReverse(suggestionId: number) {
    reverseLoadable.mutate({
      path: { projectId, keyId, languageId, suggestionId },
    });
  }

  function acceptSuggestion(suggestionId: number, declineOther: boolean) {
    acceptLoadable.mutate(
      {
        path: { projectId, keyId, languageId, suggestionId },
        query: { declineOther },
      },
      {
        onSuccess(data) {
          setEditForce(undefined);
          updateTranslation({
            keyId,
            lang: languageTag,
            data(translation) {
              return {
                ...translation,
                text: data.accepted.translation,
                suggestions: [],
                activeSuggestionCount: 0,
              };
            },
          });
          if (data.declined.length) {
            messageService.success(
              <T
                keyName="suggestion_accepted_other_declined"
                params={{ declinedCount: data.declined.length }}
              />
            );
          } else {
            messageService.success(<T keyName="suggestion_accepted" />);
          }
        },
      }
    );
  }

  function handleAccept(suggestionId: number) {
    const firstPage = activeSuggestions.data?.pages?.[0];
    acceptSuggestion(suggestionId, (firstPage?.page?.totalElements ?? 0) > 1);
  }

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
    return result.length ? result : translation.suggestions;
  }, [suggestionsLoadable.data, translation.suggestions]);

  const panelId = 'suggestions';

  const isLoading =
    declineLoadable.isLoading ||
    acceptLoadable.isLoading ||
    deleteLoadable.isLoading ||
    reverseLoadable.isLoading ||
    (suggestionsLoadable.isFetching && !suggestionsLoadable.isFetchingNextPage);

  return (
    <StyledContainer data-cy="suggestions-list">
      <StyledHeader>
        <PanelHeader
          sx={{ height: 'unset', padding: 0, background: 'unset' }}
          icon={null}
          name={t('translation_tools_suggestions')}
          countContent={
            suggestionsLoadable.data?.pages?.[0]?.page?.totalElements ??
            translation.activeSuggestionCount
          }
          onToggle={() => {
            setHidden(!hidden);
          }}
          panelId={panelId}
          open={!hidden}
        />
        {translation.totalSuggestionCount !==
          translation.activeSuggestionCount && (
          <FormControlLabel
            data-cy="translation-tools-suggestions-show-all-checkbox"
            label={
              <LabelHint
                title={t('translation_tools_suggestions_show_all_hint')}
              >
                {t('translation_tools_suggestions_show_all_label')}
              </LabelHint>
            }
            control={
              <Checkbox
                size="small"
                checked={showAll}
                onChange={(e) => {
                  setShowAll(e.target.checked);
                  setHidden(false);
                }}
                sx={{ height: 34, width: 34, marginRight: '2px' }}
              />
            }
            sx={{ mr: 0, my: '-8px' }}
          />
        )}
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
            {suggestionsList?.length ? (
              suggestionsList.map((item) => {
                const itemWithDate =
                  item as Partial<TranslationSuggestionModel>;
                return (
                  <StyledTranslationSuggestion
                    key={item.id}
                    suggestion={item}
                    isPlural={isPlural}
                    locale={locale}
                    lastUpdated={itemWithDate.createdAt}
                    onAccept={
                      canReview ? () => handleAccept(item.id) : undefined
                    }
                    onDecline={
                      canReview ? () => handleDecline(item.id) : undefined
                    }
                    onReverse={
                      canReview ? () => handleReverse(item.id) : undefined
                    }
                    onDelete={
                      user?.id === item.author.id
                        ? () => handleDelete(item.id)
                        : undefined
                    }
                    isLoading={isLoading}
                  />
                );
              })
            ) : (
              <StyledMessage>
                {t('suggestions_list_no_active_suggestions')}
              </StyledMessage>
            )}
          </StyledItemsWrapper>
        </StyledScrollWrapper>
      )}
    </StyledContainer>
  );
};

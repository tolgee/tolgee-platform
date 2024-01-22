import { useState, useRef, useEffect } from 'react';
import { useQueryClient } from 'react-query';
import { T } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import {
  useApiInfiniteQuery,
  useApiMutation,
} from 'tg.service/http/useQueryApi';

import { useTranslationsActions } from '../context/TranslationsContext';
import { messageService } from 'tg.service/MessageService';

type TranslationCommentModel = components['schemas']['TranslationCommentModel'];
type PagedModelTranslationCommentModel =
  components['schemas']['PagedModelTranslationCommentModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];
type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  keyId: number;
  translation: TranslationViewModel | undefined;
  language: LanguageModel;
  onCancel: () => void;
};

export const useComments = ({
  keyId,
  translation,
  language,
  onCancel,
}: Props) => {
  const project = useProject();
  const scrollRef = useRef<HTMLDivElement>(null);

  const queryClient = useQueryClient();
  const { updateTranslation } = useTranslationsActions();

  const [inputValue, setInputValue] = useState('');

  const path = {
    projectId: project.id,
    translationId: translation?.id as number,
  };
  const query = { sort: ['createdAt,desc', 'id,desc'], size: 30, page: 0 };

  const comments = useApiInfiniteQuery({
    url: '/v2/projects/{projectId}/translations/{translationId}/comments',
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

  const refetchComments = () => {
    if (translation?.id) {
      comments.refetch();
    }
  };

  const addComment = useApiMutation({
    url: '/v2/projects/{projectId}/translations/create-comment',
    method: 'post',
  });

  const deleteComment = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/comments/{commentId}',
    method: 'delete',
  });

  const resolveComment = useApiMutation({
    url: '/v2/projects/{projectId}/translations/{translationId}/comments/{commentId}/set-state/{state}',
    method: 'put',
  });

  const keyData = useApiMutation({
    url: '/v2/projects/{projectId}/translations',
    method: 'get',
  });

  const refreshKeyData = (keyId: number) => {
    keyData
      .mutateAsync({
        path: { projectId: project.id },
        query: { filterKeyId: [keyId], languages: [language.tag] },
      })
      .then((data) => {
        const translationData =
          data._embedded?.keys?.[0].translations[language.tag];
        if (translationData) {
          updateTranslation({
            keyId,
            lang: language.tag,
            data: translationData,
          });
        }
      });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!e.altKey && !e.ctrlKey && !e.metaKey && !e.shiftKey) {
      if (e.key === 'Enter') {
        handleAddComment();
        e.preventDefault();
      } else if (e.key === 'Escape') {
        onCancel();
        e.preventDefault();
      }
    }
  };

  const handleAddComment = () => {
    if (addComment.isLoading) {
      return;
    }
    if (!inputValue) {
      messageService.error(<T keyName="global_empty_value" />);
      return;
    }
    addComment
      .mutateAsync(
        {
          path: { projectId: project.id },
          content: {
            'application/json': {
              keyId,
              languageId: language.id,
              text: inputValue,
              state: 'NEEDS_RESOLUTION',
            },
          },
        },
        {
          onSuccess() {
            setInputValue('');
            // keep just first page and refetch it
            queryClient.setQueriesData(
              '/v2/projects/{projectId}/translations/{translationId}/comments',
              (data: any) => ({
                pages: data?.pages?.slice(0, 1),
                pageParams: data?.pageParams?.slice(0, 1),
              })
            );
            refetchComments();
          },
        }
      )
      .then(() => {
        refreshKeyData(keyId);
      });
  };

  const handleDelete = (commentId: number) => {
    deleteComment
      .mutateAsync(
        { path: { projectId: project.id, commentId } },
        {
          onSuccess() {
            refetchComments();
          },
        }
      )
      .then(() => {
        refreshKeyData(keyId);
      });
  };

  const changeState = (
    commentId: number,
    state: TranslationCommentModel['state']
  ) => {
    resolveComment
      .mutateAsync(
        {
          path: {
            projectId: project.id,
            commentId,
            state,
          },
        },
        {
          onSuccess() {
            // update comment state
            queryClient.setQueriesData(
              '/v2/projects/{projectId}/translations/{translationId}/comments',
              (data: any) => ({
                ...data,
                pages: data?.pages?.map(
                  (
                    page: PagedModelTranslationCommentModel
                  ): PagedModelTranslationCommentModel => ({
                    ...page,
                    _embedded: {
                      ...page._embedded,
                      translationComments:
                        page._embedded?.translationComments?.map((comment) =>
                          comment.id === commentId
                            ? {
                                ...comment,
                                state,
                              }
                            : comment
                        ),
                    },
                  })
                ),
              })
            );
          },
        }
      )
      .then(() => {
        refreshKeyData(keyId);
      });
  };

  const commentsList: TranslationCommentModel[] = [];
  comments.data?.pages?.forEach((page) =>
    page._embedded?.translationComments?.forEach((item) =>
      commentsList.push(item)
    )
  );
  commentsList.reverse();

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight });
  }, [comments.data?.pages?.[0].page?.totalElements]);

  const fetchMore = () => {
    const previousHeight = Number(scrollRef.current?.scrollHeight);
    comments.fetchNextPage().then(() => {
      const newHeight = Number(scrollRef.current?.scrollHeight);
      scrollRef.current?.scrollTo({
        // persist scrolling position
        top: newHeight - previousHeight,
      });
    });
  };

  return {
    inputValue,
    setInputValue,
    scrollRef,
    comments,
    fetchMore,
    handleKeyDown,
    handleAddComment,
    handleDelete,
    changeState,
    commentsList,
    isLoading:
      addComment.isLoading ||
      deleteComment.isLoading ||
      resolveComment.isLoading ||
      keyData.isLoading ||
      (comments.isFetching && !comments.isFetchingNextPage),
    isAddingComment: addComment.isLoading,
  };
};

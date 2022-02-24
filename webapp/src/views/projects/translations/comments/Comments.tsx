import React, { useState, useRef } from 'react';
import { useQueryClient } from 'react-query';
import { container } from 'tsyringe';
import { T } from '@tolgee/react';
import { makeStyles, TextField, IconButton } from '@material-ui/core';
import { Send } from '@material-ui/icons';

import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { useProject } from 'tg.hooks/useProject';
import {
  useApiInfiniteQuery,
  useApiMutation,
} from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { MessageService } from 'tg.service/MessageService';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useUser } from 'tg.hooks/useUser';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { Comment } from './Comment';

type TranslationCommentModel = components['schemas']['TranslationCommentModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];
type LanguageModel = components['schemas']['LanguageModel'];
type PagedModelTranslationCommentModel =
  components['schemas']['PagedModelTranslationCommentModel'];

const messaging = container.resolve(MessageService);

const useStyles = makeStyles((theme) => {
  const borderColor = theme.palette.grey[200];
  return {
    container: {
      display: 'flex',
      flexDirection: 'column',
      flexGrow: 1,
      flexBasis: 100,
      overflow: 'hidden',
      position: 'relative',
    },
    scrollerWrapper: {
      flexGrow: 1,
      overflow: 'hidden',
      display: 'flex',
      flexDirection: 'column',
    },
    reverseScroller: {
      display: 'flex',
      flexDirection: 'column-reverse',
      overflowY: 'auto',
      overflowX: 'hidden',
      overscrollBehavior: 'contain',
    },
    loadMore: {
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'flex-end',
      minHeight: 50,
    },
    bottomPanel: {
      display: 'flex',
      alignItems: 'flex-end',
      borderTop: `1px solid ${borderColor}`,
    },
    linearProgress: {
      marginTop: -4,
      height: 4,
    },
    input: {
      flexGrow: 1,
      padding: 12,
      alignSelf: 'center',
      '& *:after': {
        display: 'none',
      },
      '& *:before': {
        display: 'none',
      },
      '& > div': {
        padding: 0,
      },
    },
  };
});

type Props = {
  keyId: number;
  language: LanguageModel;
  translation: TranslationViewModel | undefined;
  onCancel: () => void;
  editEnabled: boolean;
};

export const Comments: React.FC<Props> = ({
  keyId,
  language,
  translation,
  onCancel,
  editEnabled,
}) => {
  const project = useProject();
  const classes = useStyles();
  const queryClient = useQueryClient();
  const scrollRef = useRef<HTMLDivElement>(null);
  const dispatch = useTranslationsDispatch();
  const user = useUser();
  const permissions = useProjectPermissions();

  const [value, setValue] = useState('');

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
          dispatch({
            type: 'UPDATE_TRANSLATION',
            payload: { keyId, lang: language.tag, data: translationData },
          });
        }
      });
  };

  const commentsList = comments.data?.pages
    ?.flatMap((p) => p._embedded?.translationComments)
    .filter(Boolean) as TranslationCommentModel[] | undefined;

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
    if (!value) {
      messaging.error(<T>global_empty_value</T>);
      return;
    }
    scrollRef.current?.scrollTo({
      top: scrollRef.current.scrollHeight,
    });
    addComment
      .mutateAsync(
        {
          path: { projectId: project.id },
          content: {
            'application/json': {
              keyId,
              languageId: language.id,
              text: value,
              state: 'NEEDS_RESOLUTION',
            },
          },
        },
        {
          onSuccess() {
            setValue('');
            // keep just first page and refetch it
            queryClient.setQueriesData(
              '/v2/projects/{projectId}/translations/{translationId}/comments',
              (data: any) => ({
                pages: data?.pages?.slice(0, 1),
                pageParams: data?.pageParams?.slice(0, 1),
              })
            );
            comments.refetch();
          },
        }
      )
      .then(() => {
        refreshKeyData(keyId);
      })
      .catch((e) => {
        const parsed = parseErrorResponse(e);
        parsed.forEach((error) => messaging.error(<T>{error}</T>));
      });
  };

  const handleDelete = (commentId: number) => {
    deleteComment
      .mutateAsync(
        { path: { projectId: project.id, commentId } },
        {
          onSuccess() {
            comments.refetch();
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

  return (
    <div className={classes.container}>
      <div className={classes.scrollerWrapper}>
        <div className={classes.reverseScroller} ref={scrollRef}>
          {commentsList?.map((comment) => {
            const canDelete =
              user?.id === comment.author.id ||
              permissions.satisfiesPermission(ProjectPermissionType.MANAGE);
            const canChangeState =
              user?.id === comment.author.id ||
              permissions.satisfiesPermission(ProjectPermissionType.TRANSLATE);
            return (
              <Comment
                key={comment.id}
                data={comment}
                onDelete={canDelete ? handleDelete : undefined}
                onChangeState={canChangeState ? changeState : undefined}
              />
            );
          })}

          {comments.hasNextPage && (
            <div className={classes.loadMore}>
              <LoadingButton
                onClick={() => comments.fetchNextPage()}
                loading={comments.isFetchingNextPage}
                data-cy="translations-comments-load-more-button"
              >
                <T>translations_comments_load_more</T>
              </LoadingButton>
            </div>
          )}
        </div>
      </div>

      <div className={classes.linearProgress}>
        <SmoothProgress
          loading={
            addComment.isLoading ||
            deleteComment.isLoading ||
            (comments.isFetching && !comments.isFetchingNextPage)
          }
        />
      </div>

      {editEnabled && (
        <div className={classes.bottomPanel}>
          <TextField
            className={classes.input}
            multiline
            value={value}
            onChange={(e) => setValue(e.currentTarget.value)}
            onKeyDown={handleKeyDown}
            variant="standard"
            data-cy="translations-comments-input"
            autoFocus
          />
          <IconButton
            color="primary"
            onClick={handleAddComment}
            disabled={addComment.isLoading}
          >
            <Send />
          </IconButton>
        </div>
      )}
    </div>
  );
};

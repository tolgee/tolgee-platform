import React, { useState, useRef, useEffect } from 'react';
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
  const query = { sort: ['updatedAt,desc', 'id,desc'], size: 30, page: 0 };

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

  useEffect(() => {
    if (comments.isSuccess) {
      const commentCount = comments.data?.pages?.[0]?.page?.totalElements;
      // update total comments count in translations list
      dispatch({
        type: 'UPDATE_TRANSLATION',
        payload: {
          keyId,
          lang: language.tag,
          data: { commentCount },
        },
      });
    }
  }, [comments.data?.pages?.[0]]);

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
    addComment.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            keyId,
            languageId: language.id,
            text: value,
            state: 'RESOLUTION_NOT_NEEDED',
          },
        },
      },
      {
        onSuccess(data) {
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
          dispatch({
            type: 'UPDATE_TRANSLATION',
            payload: {
              keyId,
              lang: language.tag,
              data: data.translation,
            },
          });
        },
        onError(e) {
          const parsed = parseErrorResponse(e);
          parsed.forEach((error) => messaging.error(<T>{error}</T>));
        },
      }
    );
  };

  const handleDelete = (commentId: number) => {
    deleteComment.mutate(
      { path: { projectId: project.id, commentId } },
      {
        onSuccess() {
          comments.refetch();
        },
      }
    );
  };

  return (
    <div className={classes.container}>
      <div className={classes.scrollerWrapper}>
        <div className={classes.reverseScroller} ref={scrollRef}>
          {commentsList?.map((comment) => {
            const canDelete =
              user?.id === comment.author.id ||
              permissions.satisfiesPermission(ProjectPermissionType.MANAGE);
            return (
              <Comment
                key={comment.id}
                data={comment}
                onDelete={canDelete ? handleDelete : undefined}
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

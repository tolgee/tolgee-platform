import React from 'react';
import { T } from '@tolgee/react';
import { makeStyles, TextField, IconButton } from '@material-ui/core';
import { Send } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useUser } from 'tg.hooks/useUser';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { Comment } from './Comment';
import { useComments } from './useComments';

type TranslationViewModel = components['schemas']['TranslationViewModel'];
type LanguageModel = components['schemas']['LanguageModel'];

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
    progressWrapper: {
      height: 0,
      position: 'relative',
    },
    linearProgress: {
      position: 'absolute',
      bottom: 0,
      left: 0,
      right: 0,
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
  const classes = useStyles();
  const permissions = useProjectPermissions();
  const user = useUser();

  const {
    commentsList,
    comments,
    scrollRef,
    handleAddComment,
    handleDelete,
    handleKeyDown,
    changeState,
    isLoading,
    isAddingComment,
    inputValue,
    setInputValue,
  } = useComments({
    keyId,
    language,
    translation,
    onCancel,
  });

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

      <div className={classes.progressWrapper}>
        <SmoothProgress
          className={classes.linearProgress}
          loading={isLoading}
        />
      </div>

      {editEnabled && (
        <div className={classes.bottomPanel}>
          <TextField
            className={classes.input}
            multiline
            value={inputValue}
            onChange={(e) => setInputValue(e.currentTarget.value)}
            onKeyDown={handleKeyDown}
            variant="standard"
            data-cy="translations-comments-input"
            autoFocus
          />
          <IconButton
            color="primary"
            onClick={handleAddComment}
            disabled={isAddingComment}
          >
            <Send />
          </IconButton>
        </div>
      )}
    </div>
  );
};

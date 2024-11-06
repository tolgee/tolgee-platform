import clsx from 'clsx';
import React, { useEffect, useState } from 'react';
import { T } from '@tolgee/react';
import { Box, IconButton, styled, TextField } from '@mui/material';
import { Send03 } from '@untitled-ui/icons-react';
import { LoadingSkeletonFadingIn } from 'tg.component/LoadingSkeleton';
import { StickyDateSeparator } from 'tg.views/projects/translations/ToolsPanel/common/StickyDateSeparator';
import { useUser } from 'tg.globalContext/helpers';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

import { Comment } from './Comment';
import { useComments } from './useComments';
import {
  PanelContentData,
  PanelContentProps,
  TranslationViewModel,
} from '../../common/types';
import { TabMessage } from '../../common/TabMessage';
import {
  StyledLoadMore,
  StyledLoadMoreButton,
} from '../../common/StyledLoadMore';
import { arraySplit } from '../../common/splitByParameter';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;
  margin-top: 4px;
`;

const StyledTextField = styled(TextField)`
  flex-grow: 1;
  margin: 8px;
  opacity: 0.5;
  &:focus-within {
    opacity: 1;
  }
  &:focus-within .icon-button {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

export const Comments: React.FC<PanelContentProps> = ({
  keyData,
  language,
  setItemsCount,
}) => {
  const { satisfiesPermission } = useProjectPermissions();
  const user = useUser();
  const [limit, setLimit] = useState(true);
  const translation = keyData.translations[language.tag] as
    | TranslationViewModel
    | undefined;

  const isAssignedToTask = keyData.tasks?.find(
    (t) => t.languageTag === language.tag
  )?.userAssigned;

  const canAddComment =
    satisfiesPermission('translation-comments.add') || isAssignedToTask;
  const canEditComment =
    satisfiesPermission('translation-comments.edit') || isAssignedToTask;
  const canSetCommentState =
    satisfiesPermission('translation-comments.set-state') || isAssignedToTask;

  const keyId = keyData.keyId;

  const {
    commentsList,
    comments,
    handleAddComment,
    handleDelete,
    handleKeyDown,
    changeState,
    isAddingComment,
    inputValue,
    setInputValue,
    fetchMore,
    isLoading,
  } = useComments({
    keyId,
    language,
    translation,
  });

  useEffect(() => {
    setItemsCount(translation?.commentCount);
  }, [translation?.commentCount]);

  const showLoadMore =
    comments.hasNextPage || (limit && commentsList.length > 4);

  function handleShowMore() {
    if (limit) {
      setLimit(false);
    } else {
      fetchMore();
    }
  }

  const trimmedComments = limit ? commentsList.slice(-4) : commentsList;

  const dayGroups = arraySplit(trimmedComments, (i) =>
    new Date(i.createdAt).toLocaleDateString()
  );

  return (
    <StyledContainer
      className={clsx({ commentsPresent: Boolean(trimmedComments.length) })}
    >
      {dayGroups.length !== 0 ? (
        dayGroups.map((items, gIndex) => (
          <Box key={gIndex} display="grid">
            <StickyDateSeparator date={new Date(items[0].createdAt)} />
            {items?.map((comment, cIndex) => {
              const canDelete =
                user?.id === comment.author.id || canEditComment;
              return (
                <React.Fragment key={comment.id}>
                  {showLoadMore && cIndex + gIndex === 0 && (
                    <StyledLoadMore>
                      <StyledLoadMoreButton
                        role="button"
                        onClick={handleShowMore}
                        data-cy="translations-comments-load-more-button"
                      >
                        <T keyName="translations_comments_previous_comments" />
                      </StyledLoadMoreButton>
                    </StyledLoadMore>
                  )}
                  <Comment
                    data={comment}
                    onDelete={canDelete ? handleDelete : undefined}
                    onChangeState={canSetCommentState ? changeState : undefined}
                  />
                </React.Fragment>
              );
            })}
          </Box>
        ))
      ) : isLoading ? (
        <TabMessage>
          <LoadingSkeletonFadingIn variant="text" />
        </TabMessage>
      ) : (
        <TabMessage>
          <T keyName="translations_comments_no_comments"></T>
        </TabMessage>
      )}

      {canAddComment && (
        <StyledTextField
          multiline
          variant="outlined"
          size="small"
          value={inputValue}
          onChange={(e) => setInputValue(e.currentTarget.value)}
          onKeyDown={handleKeyDown}
          data-cy="translations-comments-input"
          InputProps={{
            sx: {
              padding: '8px 4px 8px 12px',
              borderRadius: '8px',
            },
            endAdornment: (
              <IconButton
                className="icon-button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={handleAddComment}
                disabled={isAddingComment}
                sx={{ my: '-6px', alignSelf: 'end' }}
              >
                <Send03 width={20} height={20} color="inherit" />
              </IconButton>
            ),
          }}
        />
      )}
    </StyledContainer>
  );
};

export const commentsCount = ({ keyData, language }: PanelContentData) => {
  const translation = keyData.translations[language.tag] as
    | TranslationViewModel
    | undefined;
  return translation?.commentCount ?? 0;
};

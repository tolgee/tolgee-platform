import React from 'react';
import { T } from '@tolgee/react';
import { TextField, IconButton, styled } from '@mui/material';
import { Send } from '@mui/icons-material';

import { components } from 'tg.service/apiSchema.generated';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { SmoothProgress } from 'tg.component/SmoothProgress';
import { useUser } from 'tg.globalContext/helpers';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { Comment } from './Comment';
import { useComments } from './useComments';
import { useDateCounter } from 'tg.hooks/useDateCounter';
import { StickyDateSeparator } from 'tg.component/common/StickyDateSeparator';

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
  margin-top: -1px;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
  overscroll-behavior: contain;
`;

const StyledLoadMore = styled('div')`
  display: flex;
  justify-content: center;
  align-items: flex-end;
  min-height: 50px;
`;

const StyledBottomPanel = styled('div')`
  display: flex;
  align-items: flex-end;
  border-top: 1px solid ${({ theme }) => theme.palette.divider1.main};
`;

const StyledProgressWrapper = styled('div')`
  height: 0px;
  position: relative;
`;

const StyledSmoothProgress = styled(SmoothProgress)`
  position: absolute;
  bottom: 0px;
  left: 0px;
  right: 0px;
`;

const StyledTextField = styled(TextField)`
  flex-grow: 1;
  padding: 12px;
  align-self: center;
  & *:after {
    display: none;
  }
  & *:before {
    display: none;
  }
  & > div {
    padding: 0px;
  }
`;

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
  const permissions = useProjectPermissions();
  const user = useUser();
  const counter = useDateCounter();

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
    fetchMore,
  } = useComments({
    keyId,
    language,
    translation,
    onCancel,
  });

  return (
    <StyledContainer>
      <StyledScrollerWrapper>
        <StyledReverseScroller ref={scrollRef}>
          {comments.hasNextPage && (
            <StyledLoadMore>
              <LoadingButton
                onClick={fetchMore}
                loading={comments.isFetchingNextPage}
                data-cy="translations-comments-load-more-button"
              >
                <T>translations_comments_load_more</T>
              </LoadingButton>
            </StyledLoadMore>
          )}

          {commentsList?.map((comment) => {
            const canDelete =
              user?.id === comment.author.id ||
              permissions.satisfiesPermission(ProjectPermissionType.MANAGE);
            const canChangeState =
              user?.id === comment.author.id ||
              permissions.satisfiesPermission(ProjectPermissionType.TRANSLATE);
            const date = new Date(comment.createdAt);
            return (
              <React.Fragment key={comment.id}>
                {counter.isNewDate(date) && <StickyDateSeparator date={date} />}
                <Comment
                  data={comment}
                  onDelete={canDelete ? handleDelete : undefined}
                  onChangeState={canChangeState ? changeState : undefined}
                />
              </React.Fragment>
            );
          })}
        </StyledReverseScroller>
      </StyledScrollerWrapper>

      <StyledProgressWrapper>
        <StyledSmoothProgress loading={isLoading} />
      </StyledProgressWrapper>

      {editEnabled && (
        <StyledBottomPanel>
          <StyledTextField
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
            size="large"
          >
            <Send />
          </IconButton>
        </StyledBottomPanel>
      )}
    </StyledContainer>
  );
};

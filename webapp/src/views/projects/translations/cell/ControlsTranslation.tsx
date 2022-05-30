import React from 'react';
import clsx from 'clsx';
import { Badge, styled } from '@mui/material';
import { Edit, Comment, Check } from '@mui/icons-material';
import { T } from '@tolgee/react';

import { StateType } from 'tg.constants/translationStates';
import { components } from 'tg.service/apiSchema.generated';
import { ControlsButton } from './ControlsButton';
import { StateTransitionButtons } from './StateTransitionButtons';
import { CELL_HIGHLIGHT_ON_HOVER, CELL_SHOW_ON_HOVER } from './styles';

type State = components['schemas']['TranslationViewModel']['state'];

const StyledBadge = styled(Badge)`
  & .unresolved {
    font-size: 10px;
    height: unset;
    padding: 3px 3px;
    display: flex;
  }
  & .resolved {
    background: ${({ theme }) => theme.palette.emphasis[600]};
    padding: 0px;
    height: 16px;
    width: 18px;
    display: flex;
    min-width: unset;
    align-items: center;
    justify-content: center;
  }
`;

const StyledCheckIcon = styled(Check)`
  color: ${({ theme }) => theme.palette.emphasis[100]};
  font-size: 14px;
  margin: -5px;
`;

type ControlsProps = {
  state?: State;
  editEnabled?: boolean;
  onEdit?: () => void;
  onStateChange?: (state: StateType) => void;
  onComments?: () => void;
  commentsCount: number | undefined;
  unresolvedCommentCount: number | undefined;
  // render last focusable button
  lastFocusable?: boolean;
};

export const ControlsTranslation: React.FC<
  React.PropsWithChildren<ControlsProps>
> = ({
  state,
  editEnabled,
  onEdit,
  onStateChange,
  onComments,
  commentsCount,
  unresolvedCommentCount,
  lastFocusable,
}) => {
  const displayTransitionButtons = editEnabled && state;
  const displayEdit = editEnabled && onEdit;
  const commentsPresent = Boolean(commentsCount);
  const displayComments = onComments || lastFocusable || commentsPresent;
  const onlyResolved = commentsPresent && !unresolvedCommentCount;

  return (
    <>
      {displayTransitionButtons && (
        <StateTransitionButtons
          state={state}
          onStateChange={onStateChange}
          className={CELL_SHOW_ON_HOVER}
        />
      )}
      {displayEdit && (
        <ControlsButton
          onClick={onEdit}
          data-cy="translations-cell-edit-button"
          className={CELL_SHOW_ON_HOVER}
          tooltip={<T>translations_cell_edit</T>}
        >
          <Edit fontSize="small" />
        </ControlsButton>
      )}
      {displayComments && (
        <ControlsButton
          onClick={onComments}
          data-cy="translations-cell-comments-button"
          className={clsx({
            [CELL_SHOW_ON_HOVER]: !commentsPresent,
            [CELL_HIGHLIGHT_ON_HOVER]: onlyResolved,
          })}
          tooltip={<T>translation_cell_comments</T>}
        >
          {onlyResolved ? (
            <StyledBadge
              badgeContent={<StyledCheckIcon fontSize="small" />}
              classes={{
                badge: 'resolved',
              }}
            >
              <Comment fontSize="small" />
            </StyledBadge>
          ) : (
            <StyledBadge
              badgeContent={unresolvedCommentCount}
              color="primary"
              classes={{ badge: 'unresolved' }}
            >
              <Comment fontSize="small" />
            </StyledBadge>
          )}
        </ControlsButton>
      )}
    </>
  );
};

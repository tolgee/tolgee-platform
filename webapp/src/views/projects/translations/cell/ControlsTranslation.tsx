import React from 'react';
import clsx from 'clsx';
import { Badge, makeStyles } from '@material-ui/core';
import { Edit, Comment } from '@material-ui/icons';
import { T } from '@tolgee/react';

import { StateType } from 'tg.constants/translationStates';
import { useCellStyles } from './styles';
import { ControlsButton } from './ControlsButton';
import { StateTransitionButtons } from './StateTransitionButtons';

const useStyles = makeStyles((theme) => ({
  badge: {
    fontSize: 10,
    height: 'unset',
    padding: '3px 3px',
    display: 'flex',
  },
  badgeDot: {
    background: theme.palette.grey[800],
  },
}));

type ControlsProps = {
  state?: StateType;
  editEnabled?: boolean;
  onEdit?: () => void;
  onStateChange?: (state: StateType) => void;
  onComments?: () => void;
  commentsCount: number | undefined;
  unresolvedCommentCount: number | undefined;
  // render last focusable button
  lastFocusable?: boolean;
};

export const ControlsTranslation: React.FC<ControlsProps> = ({
  state,
  editEnabled,
  onEdit,
  onStateChange,
  onComments,
  commentsCount,
  unresolvedCommentCount,
  lastFocusable,
}) => {
  const classes = useStyles();
  const cellClasses = useCellStyles({});

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
          className={cellClasses.showOnHover}
        />
      )}
      {displayEdit && (
        <ControlsButton
          onClick={onEdit}
          data-cy="translations-cell-edit-button"
          className={cellClasses.showOnHover}
          tooltip={<T>translations_cell_edit</T>}
        >
          <Edit fontSize="small" />
        </ControlsButton>
      )}
      {displayComments && (
        <ControlsButton
          onClick={onComments}
          data-cy="translations-cell-comments-button"
          className={clsx({ [cellClasses.showOnHover]: !commentsPresent })}
          tooltip={<T>translation_cell_comments</T>}
        >
          {onlyResolved ? (
            <Badge variant="dot" classes={{ badge: classes.badgeDot }}>
              <Comment fontSize="small" />
            </Badge>
          ) : (
            <Badge
              badgeContent={unresolvedCommentCount}
              color="primary"
              classes={{ badge: classes.badge }}
            >
              <Comment fontSize="small" />
            </Badge>
          )}
        </ControlsButton>
      )}
    </>
  );
};

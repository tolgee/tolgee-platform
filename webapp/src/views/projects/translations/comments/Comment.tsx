import React from 'react';
import clsx from 'clsx';
import { useCurrentLanguage, T, useTranslate } from '@tolgee/react';
import { makeStyles, Menu, MenuItem, Tooltip } from '@material-ui/core';
import { MoreVert, Check } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { SmallActionButton } from './SmallActionButton';

type TranslationCommentModel = components['schemas']['TranslationCommentModel'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    gridTemplateAreas: `
      "avatar  text   time            menu"
      "avatar  text   resolveAction   menu"
    `,
    gridTemplateColumns: 'auto 1fr auto 26px',
    padding: '7px 12px 7px 12px',
    background: 'transparent',
    transition: 'background 0.1s ease-out',
    '&:hover': {
      background: theme.palette.grey[200],
      transition: 'background 0.1s ease-in',
    },
    '&:hover $hoverVisible': {
      opacity: 1,
      transition: 'opacity 0.5s ease-in',
    },
  },
  unresolved: {
    background: theme.palette.grey[100],
  },
  hoverVisible: {
    opacity: 0,
    transition: 'opacity 0.1s ease-out',
  },
  avatar: {
    gridArea: 'avatar',
    margin: theme.spacing(0.5, 1, 0.5, 0),
    alignSelf: 'start',
  },
  menu: {
    gridArea: 'menu',
    display: 'flex',
    margin: theme.spacing(0.5, -1, 0, 0.5),
  },
  actionItem: {
    height: 26,
    width: 26,
  },
  resolveButton: {
    gridArea: 'resolveAction',
    display: 'flex',
    justifySelf: 'end',
    alignSelf: 'start',
    margin: -4,
    marginTop: -2,
  },
  text: {
    gridArea: 'text',
    padding: 0,
    margin: 0,
    marginTop: 7,
    marginBottom: 4,
    lineHeight: 1.1,
    fontFamily: theme.typography.fontFamily,
    whiteSpace: 'pre-wrap',
    wordWrap: 'break-word',
  },
  textUnresolved: {
    fontWeight: 600,
  },
  time: {
    gridArea: 'time',
    display: 'flex',
    fontSize: '11px',
    justifyContent: 'flex-end',
  },
}));

type Props = {
  data: TranslationCommentModel;
  onDelete: ((commentId: number) => void) | undefined;
  onChangeState:
    | ((commentId: number, state: TranslationCommentModel['state']) => void)
    | undefined;
};

export const Comment: React.FC<Props> = ({ data, onDelete, onChangeState }) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const classes = useStyles();
  const lang = useCurrentLanguage();
  const date = new Date(data.createdAt);
  const isToday = date.toLocaleDateString() === new Date().toLocaleDateString();
  const t = useTranslate();

  const unresolveVisible = data.state !== 'NEEDS_RESOLUTION' && onChangeState;

  const handleClose = () => {
    setAnchorEl(null);
  };

  const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleDelete = () => {
    handleClose();
    confirmation({
      message: <T>translations_comments_delete_confirmation</T>,
      confirmButtonText: <T>translations_comments_delete_button</T>,
      onConfirm: () => onDelete?.(data.id),
    });
  };

  const handleResolve = () => {
    onChangeState?.(data.id, 'RESOLVED');
  };

  const handleUnresolved = () => {
    handleClose();
    onChangeState?.(data.id, 'NEEDS_RESOLUTION');
  };

  return (
    <div
      className={clsx(classes.container, {
        [classes.unresolved]: data.state === 'NEEDS_RESOLUTION',
      })}
      data-cy="comment"
    >
      <Tooltip title={data.author.name || data.author.username}>
        <div className={classes.avatar}>
          <AvatarImg
            owner={{ ...data.author, type: 'USER' }}
            size={24}
            autoAvatarType="IDENTICON"
            circle
          />
        </div>
      </Tooltip>
      <pre
        className={clsx(classes.text, {
          [classes.textUnresolved]: data.state === 'NEEDS_RESOLUTION',
        })}
        data-cy="comment-text"
      >
        {data.text}
      </pre>
      <div className={classes.time}>
        {!isToday && date.toLocaleDateString(lang()) + ' '}
        {date.toLocaleTimeString(lang(), {
          hour: 'numeric',
          minute: 'numeric',
        })}
      </div>
      <div className={clsx(classes.menu, classes.hoverVisible)}>
        {(onDelete || unresolveVisible) && (
          <SmallActionButton
            className={classes.actionItem}
            onClick={handleClick}
            data-cy="comment-menu"
          >
            <MoreVert fontSize="small" />
          </SmallActionButton>
        )}
      </div>
      {data.state === 'NEEDS_RESOLUTION' && onChangeState && (
        <Tooltip title={t('translations_comments_resolve', 'Resolve')}>
          <div className={clsx(classes.resolveButton, classes.hoverVisible)}>
            <SmallActionButton
              onClick={handleResolve}
              data-cy="comment-resolve"
            >
              <Check fontSize="small" color="primary" />
            </SmallActionButton>
          </div>
        </Tooltip>
      )}
      {anchorEl && (
        <Menu
          id="simple-menu"
          anchorEl={anchorEl}
          keepMounted
          open={Boolean(anchorEl)}
          onClose={handleClose}
        >
          {unresolveVisible && (
            <MenuItem
              onClick={handleUnresolved}
              data-cy="comment-menu-needs-resolution"
            >
              {t('translations_comments_needs_resolution', 'Not resolved')}
            </MenuItem>
          )}
          {onDelete && (
            <MenuItem onClick={handleDelete} data-cy="comment-menu-delete">
              {t('translations_comments_delete_button')}
            </MenuItem>
          )}
        </Menu>
      )}
    </div>
  );
};

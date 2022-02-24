import React from 'react';
import clsx from 'clsx';
import { useCurrentLanguage, T, useTranslate } from '@tolgee/react';
import {
  IconButton,
  makeStyles,
  Menu,
  MenuItem,
  Tooltip,
} from '@material-ui/core';
import { MoreHoriz } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import { useCellStyles } from '../cell/styles';
import { confirmation } from 'tg.hooks/confirmation';
import { SmallActionButton } from './SmallActionButton';

type TranslationCommentModel = components['schemas']['TranslationCommentModel'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'grid',
    gridTemplateAreas: `
      "indicator  text   text   menu"
      "indicator  autor  time   time"
      "action     action action action"
    `,
    gridTemplateColumns: '2px 1fr 1fr auto',
    padding: '7px 12px 7px 12px',
    background: 'transparent',
    transition: 'background 0.1s ease-out',
    '&:hover': {
      background: theme.palette.grey[100],
      transition: 'background 0.1s ease-in',
    },
    '&:hover $actionItem': {
      opacity: 1,
      transition: 'opacity 0.5s ease-in',
    },
  },
  menu: {
    display: 'flex',
    gridArea: 'menu',
    margin: '-7px -6px -3px 0px',
  },
  actionItem: {
    opacity: 0,
    transition: 'opacity 0.1s ease-out',
    height: 26,
    width: 26,
  },
  bottomActions: {
    display: 'flex',
    gridArea: 'action',
    justifyContent: 'flex-end',
  },
  actionButton: {
    padding: 0,
  },
  indicator: {
    gridArea: 'indicator',
    marginTop: 6,
    marginLeft: -7,
    width: 6,
    height: 6,
    borderRadius: '50%',
    background: theme.palette.primary.main,
  },
  text: {
    gridArea: 'text',
    padding: 0,
    margin: 0,
    lineHeight: 1.1,
    fontFamily: theme.typography.fontFamily,
    whiteSpace: 'pre-wrap',
    wordWrap: 'break-word',
  },
  autor: {
    gridArea: 'autor',
    fontSize: '11px',
  },
  time: {
    display: 'flex',
    gridArea: 'time',
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
  const cellClasses = useCellStyles({});
  const lang = useCurrentLanguage();
  const date = new Date(data.createdAt);
  const isToday = date.toLocaleDateString() === new Date().toLocaleDateString();
  const t = useTranslate();

  const unresolveVisible = data.state === 'RESOLVED' && onChangeState;

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
      className={clsx(classes.container, cellClasses.hover)}
      data-cy="comment"
    >
      {data.state !== 'RESOLVED' && (
        <Tooltip title={t('translations_comments_needs_resolution')}>
          <div className={classes.indicator} />
        </Tooltip>
      )}
      <pre className={classes.text} data-cy="comment-text">
        {data.text}
      </pre>
      <div className={classes.menu}>
        {(onDelete || unresolveVisible) && (
          <IconButton
            className={classes.actionItem}
            size="small"
            onClick={handleClick}
            data-cy="comment-menu"
          >
            <MoreHoriz fontSize="small" />
          </IconButton>
        )}
      </div>
      <div className={classes.autor}>{data.author.name}</div>
      <div className={classes.time}>
        {!isToday && date.toLocaleDateString(lang()) + ' '}
        {date.toLocaleTimeString(lang(), {
          hour: 'numeric',
          minute: 'numeric',
        })}
      </div>
      {data.state !== 'RESOLVED' && onChangeState && (
        <div className={classes.bottomActions}>
          <SmallActionButton onClick={handleResolve} data-cy="comment-resolve">
            {t('translations_comments_resolve')}
          </SmallActionButton>
        </div>
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
              {t('translations_comments_needs_resolution')}
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

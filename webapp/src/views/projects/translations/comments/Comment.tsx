import React from 'react';
import clsx from 'clsx';
import { useCurrentLanguage, T } from '@tolgee/react';
import { IconButton, makeStyles, Menu, MenuItem } from '@material-ui/core';
import { MoreVert } from '@material-ui/icons';

import { components } from 'tg.service/apiSchema.generated';
import { useCellStyles } from '../cell/styles';
import { confirmation } from 'tg.hooks/confirmation';

type TranslationCommentModel = components['schemas']['TranslationCommentModel'];

const useStyles = makeStyles((theme) => ({
  menu: {
    gridArea: 'menu',
    height: 26,
    width: 26,
    margin: '-7px -6px -3px 0px',
    opacity: 0,
    transition: 'opacity 0.1s ease-out',
  },
  container: {
    display: 'grid',
    gridTemplateAreas: `
      "text     text   menu"
      "autor    time   time"
    `,
    gridTemplateColumns: '1fr 1fr auto',
    padding: '7px 12px 7px 12px',
    background: 'transparent',
    transition: 'background 0.1s ease-out',
    '&:hover': {
      background: theme.palette.grey[100],
      transition: 'background 0.1s ease-in',
    },
    '&:hover $menu': {
      opacity: 1,
      transition: 'opacity 0.5s ease-in',
    },
  },
  text: {
    gridArea: 'text',
    lineHeight: 1.1,
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
};

export const Comment: React.FC<Props> = ({ data, onDelete }) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const classes = useStyles();
  const cellClasses = useCellStyles({});
  const lang = useCurrentLanguage();
  const date = new Date(data.updatedAt);
  const isToday = date.toLocaleDateString() === new Date().toLocaleDateString();

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

  return (
    <div
      className={clsx(classes.container, cellClasses.hover)}
      data-cy="comment"
    >
      <div className={classes.text} data-cy="comment-text">
        {data.text}
      </div>
      {onDelete && (
        <IconButton
          className={classes.menu}
          size="small"
          onClick={handleClick}
          data-cy="comment-menu"
        >
          <MoreVert fontSize="small" />
        </IconButton>
      )}
      <div className={classes.autor}>{data.author.name}</div>
      <div className={classes.time}>
        {!isToday && date.toLocaleDateString(lang()) + ' '}
        {date.toLocaleTimeString(lang(), {
          hour: 'numeric',
          minute: 'numeric',
        })}
      </div>
      {anchorEl && (
        <Menu
          id="simple-menu"
          anchorEl={anchorEl}
          keepMounted
          open={Boolean(anchorEl)}
          onClose={handleClose}
        >
          <MenuItem onClick={handleDelete} data-cy="comment-menu-delete">
            <T noWrap>translations_comments_delete_button</T>
          </MenuItem>
        </Menu>
      )}
    </div>
  );
};

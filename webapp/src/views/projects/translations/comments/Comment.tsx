import React from 'react';
import clsx from 'clsx';
import { T, useTranslate } from '@tolgee/react';
import { Menu, MenuItem, styled, Tooltip } from '@mui/material';
import { Check, MoreVert } from '@mui/icons-material';

import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { SmallActionButton } from '../cell/SmallActionButton';
import { UserName } from 'tg.component/common/UserName';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';

type TranslationCommentModel = components['schemas']['TranslationCommentModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-areas:
    'avatar  text   time            menu'
    'avatar  text   resolveAction   menu';
  grid-template-columns: auto 1fr auto 26px;
  padding: 4px 9px 4px 9px;
  margin: 3px;
  background: transparent;
  transition: background-color 0.1s ease-out;
  border-radius: 8px;

  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[100]};
    transition: background-color 0.1s ease-in;
  }

  & .hoverVisible {
    opacity: 0;
    transition: opacity 0.1s ease-out;
  }

  &:hover .hoverVisible {
    opacity: 1;
    transition: opacity 0.5s ease-in;
  }
`;

const StyledAvatar = styled('div')`
  grid-area: avatar;
  margin: ${({ theme }) => theme.spacing(0.5, 1, 0.5, 0)};
  align-self: start;
`;

const StyledMenu = styled('div')`
  grid-area: menu;
  display: flex;
  margin: ${({ theme }) => theme.spacing(0.5, -1, 0, 0.5)};
`;

const StyledSmallActionButton = styled(SmallActionButton)`
  color: ${({ theme }) => theme.palette.text.primary};
  height: 26px;
  width: 26px;
`;

const StyledResolveButton = styled('div')`
  grid-area: resolveAction;
  display: flex;
  justify-self: end;
  align-self: start;
  margin: -4px;
  margin-top: -2px;
`;

const StyledTextPre = styled('pre')`
  grid-area: text;
  padding: 0px;
  margin: 0px;
  margin-top: 7px;
  margin-bottom: 4px;
  line-height: 1.1;
  font-family: ${({ theme }) => theme.typography.fontFamily};
  white-space: pre-wrap;
  word-wrap: break-word;

  &.textUnresolved {
    color: ${({ theme }) => theme.palette.primary.main};
    font-weight: 500;
  }
`;

const StyledTime = styled('div')`
  grid-area: time;
  display: flex;
  font-size: 11px;
  justify-content: flex-end;
`;

type Props = {
  data: TranslationCommentModel;
  onDelete: ((commentId: number) => void) | undefined;
  onChangeState:
    | ((commentId: number, state: TranslationCommentModel['state']) => void)
    | undefined;
};

export const Comment: React.FC<Props> = ({ data, onDelete, onChangeState }) => {
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const lang = useCurrentLanguage();
  const date = new Date(data.createdAt);
  const { t } = useTranslate();

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
    <StyledContainer
      className={clsx({
        unresolved: data.state === 'NEEDS_RESOLUTION',
      })}
      data-cy="comment"
    >
      <Tooltip title={<UserName {...data.author} />}>
        <StyledAvatar>
          <AvatarImg owner={{ ...data.author, type: 'USER' }} size={24} />
        </StyledAvatar>
      </Tooltip>
      <StyledTextPre
        className={clsx({
          textUnresolved: data.state === 'NEEDS_RESOLUTION',
        })}
        data-cy="comment-text"
      >
        {data.text}
      </StyledTextPre>
      <StyledTime>
        {date.toLocaleTimeString(lang, {
          hour: 'numeric',
          minute: 'numeric',
        })}
      </StyledTime>
      <StyledMenu className="hoverVisible">
        {(onDelete || unresolveVisible) && (
          <StyledSmallActionButton onClick={handleClick} data-cy="comment-menu">
            <MoreVert fontSize="small" />
          </StyledSmallActionButton>
        )}
      </StyledMenu>
      {data.state === 'NEEDS_RESOLUTION' && onChangeState && (
        <Tooltip title={t('translations_comments_resolve', 'Resolve')}>
          <StyledResolveButton className="hoverVisible">
            <SmallActionButton
              onClick={handleResolve}
              data-cy="comment-resolve"
            >
              <Check fontSize="small" color="primary" />
            </SmallActionButton>
          </StyledResolveButton>
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
    </StyledContainer>
  );
};

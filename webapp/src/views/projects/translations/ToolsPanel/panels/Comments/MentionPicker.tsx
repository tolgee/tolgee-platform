import React from 'react';
import {
  Box,
  ListItemIcon,
  MenuItem,
  MenuList,
  Paper,
  Popper,
  styled,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';

type MentionedUserModel = components['schemas']['MentionedUserModel'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledPaper = styled(Paper)`
  min-width: 240px;
  max-width: 360px;
  overflow: hidden;
`;

const StyledFooter = styled(Box)`
  padding: ${({ theme }) => theme.spacing(0.75, 2, 1, 2)};
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
`;

const StyledName = styled('span')`
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const StyledInThread = styled('span')`
  margin-left: auto;
  padding-left: ${({ theme }) => theme.spacing(1.5)};
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
  white-space: nowrap;
`;

type Props = {
  anchorEl: HTMLElement | null;
  open: boolean;
  users: MentionedUserModel[];
  highlighted: number;
  isLoading: boolean;
  language: LanguageModel;
  onHighlight: (index: number) => void;
  onSelect: (user: MentionedUserModel) => void;
};

export const MentionPicker = ({
  anchorEl,
  open,
  users,
  highlighted,
  isLoading,
  language,
  onHighlight,
  onSelect,
}: Props) => {
  const { t } = useTranslate();

  return (
    <Popper
      open={open && Boolean(anchorEl)}
      anchorEl={anchorEl}
      placement="top-start"
      sx={{ zIndex: (theme) => theme.zIndex.modal }}
      modifiers={[{ name: 'offset', options: { offset: [8, 4] } }]}
    >
      <StyledPaper elevation={4} data-cy="comment-mention-picker">
        <MenuList dense disablePadding sx={{ py: 0.5 }} role="listbox">
          {users.map((user, index) => (
            <MenuItem
              key={user.id}
              selected={index === highlighted}
              onMouseDown={(e) => e.preventDefault()}
              onMouseEnter={() => onHighlight(index)}
              onClick={() => onSelect(user)}
              data-cy="comment-mention-picker-item"
              data-cy-user-id={user.id}
            >
              <ListItemIcon sx={{ minWidth: '32px !important' }}>
                <AvatarImg
                  owner={{
                    id: user.id,
                    name: user.name,
                    avatar: user.avatar,
                    type: 'USER',
                  }}
                  size={24}
                />
              </ListItemIcon>
              <StyledName>{user.name}</StyledName>
              {user.inThread && (
                <StyledInThread data-cy="comment-mention-picker-in-thread">
                  {t('comment_mention_picker_in_thread', 'In this thread')}
                </StyledInThread>
              )}
            </MenuItem>
          ))}
          {!users.length && !isLoading && (
            <Box px={2} py={1} data-cy="comment-mention-picker-empty">
              <Typography variant="body2" color="text.secondary">
                {t('comment_mention_picker_empty', 'Nobody matches this name')}
              </Typography>
            </Box>
          )}
        </MenuList>
        <StyledFooter data-cy="comment-mention-picker-hint">
          {t(
            'comment_mention_picker_hint',
            'People who can view {languageName}',
            {
              languageName: language.name,
            }
          )}
        </StyledFooter>
      </StyledPaper>
    </Popper>
  );
};

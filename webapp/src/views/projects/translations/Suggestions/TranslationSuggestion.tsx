import {
  Box,
  Menu,
  MenuItem,
  styled,
  SxProps,
  Tooltip,
  useTheme,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import {
  Check,
  DotsVertical,
  ReverseLeft,
  Trash01,
  XClose,
} from '@untitled-ui/icons-react';
import { useUser } from 'tg.globalContext/helpers';
import { SuggestionAction } from './SuggestionAction';
import { TranslationVisual } from '../translationVisual/TranslationVisual';
import { useRef, useState } from 'react';
import React from 'react';
import clsx from 'clsx';

type TranslationSuggestionSimpleModel =
  components['schemas']['TranslationSuggestionSimpleModel'];

const StyledContainer = styled(Box)`
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 8px;
  padding: 8px 8px;
  align-items: start;

  & .actions,
  & .date {
    transition: opacity 0.1s ease-in-out;
  }
  & .actions {
    opacity: 0;
  }

  &.showActions,
  &.withActions:hover,
  &.withActions:focus-within {
    .actions {
      opacity: 1;
    }
    .date {
      opacity: 0;
    }
  }
`;

const StyledContent = styled('div')`
  display: grid;
  padding: 2px 0px;
`;

const StyledRightPart = styled('div')`
  display: grid;
  grid-template-areas: rightPart;
  justify-items: end;
`;

const StyledDate = styled('div')`
  padding-top: 3px;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
  grid-area: rightPart;
`;

const StyledActions = styled('div')`
  padding: 1px 0px;
  display: flex;
  grid-area: rightPart;
  gap: 4px;
`;

type ActionItem = {
  label: string;
  onClick: () => void;
  icon: React.ElementType<{ width: number; height: number }>;
  color?: string;
  disabled?: boolean;
  action: string;
};

type Props = {
  suggestion: TranslationSuggestionSimpleModel;
  isPlural: boolean;
  locale: string;
  maxLines?: number;
  lastUpdated?: number | string;
  isLoading?: boolean;
  onDecline?: () => void;
  onAccept?: () => void;
  onDelete?: () => void;
  onReverse?: () => void;
  sx?: SxProps;
  className?: string;
};

export const TranslationSuggestion = ({
  suggestion,
  locale,
  maxLines = 3,
  onAccept,
  onDecline,
  onDelete,
  onReverse,
  lastUpdated,
  sx,
  className,
  isLoading,
}: Props) => {
  const theme = useTheme();
  const formatDate = useTimeDistance();
  const user = useUser();
  const { t } = useTranslate();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLButtonElement>(null);

  const actions: ActionItem[] = [];

  if (onDelete && suggestion.author.id === user?.id) {
    actions.push({
      action: 'delete',
      label: t('translation_suggestion_delete_tooltip'),
      onClick: onDelete,
      icon: Trash01,
      color: theme.palette.tokens.error.main,
      disabled: isLoading,
    });
  }
  if (suggestion.state !== 'ACTIVE') {
    if (onReverse) {
      actions.push({
        action: 'reverse',
        label: t('translation_suggestion_reverse_tooltip'),
        onClick: onReverse,
        icon: ReverseLeft,
        disabled: isLoading,
      });
    }
  } else {
    if (onAccept) {
      actions.push({
        action: 'accept',
        label: t('translation_suggestion_accept_tooltip'),
        onClick: onAccept,
        icon: Check,
        color: theme.palette.tokens.success.main,
        disabled: isLoading,
      });
    }
    if (onDecline) {
      actions.push({
        action: 'decline',
        label: t('translation_suggestion_decline_tooltip'),
        onClick: onDecline,
        icon: XClose,
        disabled: isLoading,
      });
    }
  }

  const showMenu = actions.length > 2;
  const regularItems = showMenu ? actions.slice(0, 1) : actions;
  const menuItems = showMenu ? actions.slice(1) : [];
  const active = suggestion.state === 'ACTIVE';
  return (
    <StyledContainer
      {...{
        sx,
        className: clsx(
          { withActions: actions.length !== 0, showActions: menuOpen },
          className
        ),
      }}
      data-cy="translation-suggestion"
    >
      <Tooltip
        title={
          <T
            keyName="suggestion_author_tooltip"
            params={{ user: user?.name ?? user?.username, b: <b /> }}
          />
        }
      >
        <Box sx={{ opacity: active ? 1 : 0.5 }}>
          <AvatarImg owner={{ ...suggestion.author, type: 'USER' }} size={24} />
        </Box>
      </Tooltip>
      <StyledContent sx={{ opacity: active ? 1 : 0.5 }}>
        <TranslationVisual
          text={suggestion.translation}
          isPlural={suggestion.isPlural}
          locale={locale}
          maxLines={maxLines}
          extraPadding={false}
        />
      </StyledContent>
      {lastUpdated && (
        <StyledRightPart>
          <StyledDate className="date">{formatDate(lastUpdated)}</StyledDate>
          <StyledActions className="actions">
            {regularItems.map((action, i) => (
              <SuggestionAction
                key={i}
                tooltip={action.label}
                icon={action.icon}
                sx={{ color: action.color }}
                disabled={action.disabled}
                onClick={action.onClick}
                action={action.action}
              />
            ))}
            {showMenu && (
              <>
                <SuggestionAction
                  tooltip={t('translation_suggestion_show_more_tooltip')}
                  icon={DotsVertical}
                  color="default"
                  onClick={() => setMenuOpen(true)}
                  ref={menuRef}
                  action="menu"
                />
                <Menu
                  open={menuOpen}
                  anchorEl={menuRef.current}
                  onClose={() => setMenuOpen(false)}
                >
                  {menuItems.map((action, i) => {
                    const Icon = action.icon;
                    return (
                      <MenuItem
                        key={i}
                        onClick={() => {
                          action.onClick();
                          setMenuOpen(false);
                        }}
                        sx={{ color: action.color }}
                        data-cy="translation-suggestion-action-menu-item"
                      >
                        <Box display="flex" gap={1} alignItems="center">
                          <Icon width={20} height={20} />
                          <span>{action.label}</span>
                        </Box>
                      </MenuItem>
                    );
                  })}
                </Menu>
              </>
            )}
          </StyledActions>
        </StyledRightPart>
      )}
    </StyledContainer>
  );
};

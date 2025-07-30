import {
  Box,
  IconButtonOwnProps,
  Menu,
  MenuItem,
  styled,
  SxProps,
  useTheme,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { components } from 'tg.service/apiSchema.generated';
import { useTimeDistance } from 'tg.hooks/useTimeDistance';
import {
  Check,
  DotsVertical,
  ReverseLeft,
  Trash02,
  X,
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
  padding: 6px 8px;
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
  color?: IconButtonOwnProps['color'];
  disabled?: boolean;
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
  isPlural,
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
      label: t('translation_suggestion_delete_tooltip'),
      onClick: onDelete,
      icon: Trash02,
      color: 'error',
      disabled: isLoading,
    });
  }
  if (suggestion.state !== 'ACTIVE') {
    if (onReverse) {
      actions.push({
        label: t('translation_suggestion_reverse_tooltip'),
        onClick: onReverse,
        icon: ReverseLeft,
        disabled: isLoading,
      });
    }
  } else {
    if (onAccept) {
      actions.push({
        label: t('translation_suggestion_accept_tooltip'),
        onClick: onAccept,
        icon: Check,
        color: 'success',
        disabled: isLoading,
      });
    }
    if (onDecline) {
      actions.push({
        label: t('translation_suggestion_decline_tooltip'),
        onClick: onDecline,
        icon: X,
        disabled: isLoading,
      });
    }
  }

  const showMenu = actions.length > 2;
  const regularItems = showMenu ? actions.slice(0, 1) : actions;
  const menuItems = showMenu ? actions.slice(1) : [];

  return (
    <StyledContainer
      style={{ opacity: suggestion.state === 'ACTIVE' ? 1 : 0.5 }}
      {...{
        sx,
        className: clsx(
          { withActions: actions.length !== 0, showActions: menuOpen },
          className
        ),
      }}
      data-cy="translation-suggestion"
    >
      <AvatarImg owner={{ ...suggestion.author, type: 'USER' }} size={24} />
      <StyledContent>
        <TranslationVisual
          text={suggestion.translation}
          isPlural={isPlural}
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
                color={action.color}
                disabled={action.disabled}
                onClick={action.onClick}
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
                />
                <Menu
                  open={menuOpen}
                  anchorEl={menuRef.current}
                  onClose={() => setMenuOpen(false)}
                >
                  {menuItems.map((action, i) => {
                    const Icon = action.icon;
                    const color = action.color
                      ? theme.palette[action.color]?.main
                      : undefined;
                    return (
                      <MenuItem
                        key={i}
                        onClick={() => {
                          action.onClick();
                          setMenuOpen(false);
                        }}
                      >
                        <Box
                          display="flex"
                          gap={1}
                          alignItems="center"
                          color={color}
                        >
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

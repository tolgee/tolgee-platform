import {
  Checkbox,
  ListItemText,
  MenuItemProps,
  Radio,
  styled,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import clsx from 'clsx';
import React from 'react';
import { ButtonToggle } from 'tg.component/ButtonToggle';
import { CompactMenuItem } from 'tg.component/ListComponents';

const StyledMenuItem = styled(CompactMenuItem)`
  display: grid;
  grid-template-columns: auto 1fr auto auto;
  align-items: center;
  padding-left: 4px !important;
  & .hidden {
    opacity: 0;
    transition: opacity ease-in 0.1s;
  }
  &:hover .hidden {
    opacity: 1;
  }
  gap: 8px;
`;

const StyledListItemText = styled(ListItemText)`
  overflow: hidden;
`;

const StyledCheckbox = styled(Checkbox)`
  margin: -8px -8px -8px 0px;
  &.excluded {
    color: ${({ theme }) => theme.palette.tokens.icon.primary};
  }
`;

const StyledRadio = styled(Radio)`
  margin: -8px -8px -8px 0px;
  &.excluded {
    color: ${({ theme }) => theme.palette.tokens.icon.primary};
  }
`;

type Props = MenuItemProps & {
  label: React.ReactNode;
  selected: boolean;
  excluded?: boolean;
  onExclude?: () => void;
  exclusive?: boolean;
  indicator?: React.ReactNode;
};

export const FilterItem = React.forwardRef(function FilterItem(
  {
    label,
    excluded,
    selected,
    onExclude,
    exclusive,
    indicator,
    ...other
  }: Props,
  ref
) {
  const { t } = useTranslate();
  return (
    <StyledMenuItem
      ref={ref as any}
      data-cy="filter-item"
      selected={selected}
      {...other}
    >
      {exclusive ? (
        <StyledRadio
          checked={Boolean(selected || excluded)}
          size="small"
          className={clsx({ excluded })}
        />
      ) : (
        <StyledCheckbox
          checked={Boolean(selected || excluded)}
          size="small"
          className={clsx({ excluded })}
        />
      )}
      <StyledListItemText primary={label} />
      {onExclude && (
        <ButtonToggle
          data-cy="filter-item-exclude"
          active={excluded}
          className={clsx({ hidden: !excluded })}
          onMouseDown={(e) => {
            e.stopPropagation();
          }}
          onClick={(e) => {
            e.stopPropagation();
            onExclude?.();
          }}
        >
          {excluded
            ? t('translation_filter_item_excluded')
            : t('translation_filter_item_exclude')}
        </ButtonToggle>
      )}
      {indicator}
    </StyledMenuItem>
  );
});

import { Menu, PopoverOrigin, styled } from '@mui/material';
import React, { useState, forwardRef } from 'react';
import clsx from 'clsx';
import { AddLabel } from 'tg.views/projects/translations/TranslationsList/Label/Control/AddLabel';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { LabelSelector } from 'tg.views/projects/translations/TranslationsList/Label/Control/LabelSelector';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

type LabelModel = components['schemas']['LabelModel'];

const StyledControl = styled('div')`
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.icon.secondary};
  height: 24px;
  font-size: 14px;

  &:hover,
  &.active,
  &:focus-within {
    cursor: pointer;
    border: 1px solid ${({ theme }) => theme.palette.primary.main};
    color: ${({ theme }) => theme.palette.primary.main};
  }

  &.active {
    opacity: 1;
  }
`;

type LabelControlProps = {
  className?: string;
  existing?: LabelModel[];
  onSelect?: (labelModel: LabelModel) => void;
  menuAnchorOrigin?: PopoverOrigin;
  menuStyle?: React.CSSProperties;
};

export const LabelControl = forwardRef<HTMLDivElement, LabelControlProps>(
  (props, ref) => {
    const { className, existing, onSelect, menuAnchorOrigin, menuStyle } =
      props;
    const { isEnabled } = useEnabledFeatures();
    const labelsEnabled = isEnabled('TRANSLATION_LABELS');
    if (!labelsEnabled) {
      return null;
    }
    const labels = useTranslationsSelector((c) => c.labels) || [];

    const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

    if (labels.length === 0) {
      return null;
    }

    const handleOpen = (event: React.MouseEvent<HTMLElement>) => {
      setAnchorEl(event.currentTarget);
    };

    const handleClose = () => {
      setAnchorEl(null);
    };

    const handleKeyUp = (e: React.KeyboardEvent<HTMLInputElement>) => {
      if (e.key === 'Escape') {
        handleClose();
      }
    };

    const extendedAddLabelControl = existing?.length === 0;

    return (
      <StyledControl
        ref={ref}
        className={clsx(Boolean(anchorEl) && 'active', className)}
        data-cy="translation-label-control"
      >
        <AddLabel
          onClick={stopBubble(handleOpen)}
          showText={extendedAddLabelControl}
          className="clickable"
          data-cy="translation-label-add"
        />
        <Menu
          open={Boolean(anchorEl)}
          onClose={stopBubble(handleClose)}
          anchorEl={anchorEl}
          anchorOrigin={
            menuAnchorOrigin || {
              vertical: 'bottom',
              horizontal: 'left',
            }
          }
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          slotProps={{
            paper: { style: menuStyle ? menuStyle : { marginTop: 8 } },
          }}
          onKeyUp={handleKeyUp}
        >
          <LabelSelector
            onSelect={(label: LabelModel) => {
              onSelect?.(label);
            }}
            existing={existing}
          />
        </Menu>
      </StyledControl>
    );
  }
);

LabelControl.displayName = 'LabelControl'; // Optional but useful for debugging

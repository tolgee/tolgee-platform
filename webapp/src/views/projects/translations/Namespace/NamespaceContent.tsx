import React, { useState } from 'react';
import { Menu, MenuItem, styled } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

import { useNamespaceFilter } from './useNamespaceFilter';
import { NamespaceRenameDialog } from './NamespaceRenameDialog';

const StyledNamespace = styled('div')`
  display: flex;
  align-items: center;
  cursor: pointer;
  background: ${({ theme }) => theme.palette.background.default};
  padding: ${({ theme }) => theme.spacing(0, 1)};
  padding-bottom: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  height: 24px;
  position: relative;
  top: -4px;
  border-radius: 12px;
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 0px 7px -1px #000000'
      : '0px 0px 7px -2px #000000'};
  z-index: 1;
`;

const StyledMoreArrow = styled('div')`
  display: flex;
  align-items: center;
  padding-left: 2px;
`;

type Props = {
  namespace: string;
  sticky?: boolean;
};

export const NamespaceContent = React.forwardRef<HTMLDivElement, Props>(
  function NamespaceContent({ namespace, sticky }, ref) {
    const t = useTranslate();
    const { toggle, isActive } = useNamespaceFilter(namespace);
    const [open, setOpen] = useState<undefined | HTMLElement>(undefined);
    const [renameOpen, setRenameOpen] = useState(false);

    const handleClose = () => {
      setOpen(undefined);
    };

    return (
      <>
        <StyledNamespace
          ref={ref}
          role="button"
          onClick={toggle}
          style={{
            top: sticky ? 0 : -4,
            borderRadius: sticky ? '0px 0px 12px 12px' : 12,
          }}
        >
          <div>{namespace || t('namespace_default')}</div>
          <StyledMoreArrow>
            <ArrowDropDown
              fontSize="small"
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                setOpen(e.target as HTMLDivElement);
              }}
            />
          </StyledMoreArrow>
        </StyledNamespace>
        {open && (
          <Menu
            id="basic-menu"
            anchorEl={open}
            autoFocus={false}
            open={Boolean(open)}
            onClose={handleClose}
            MenuListProps={{
              'aria-labelledby': 'basic-button',
            }}
          >
            <MenuItem
              onClick={() => {
                toggle();
                handleClose();
              }}
            >
              {isActive
                ? t('namespace_menu_filter_cancel')
                : t('namespace_menu_filter', { namespace })}
            </MenuItem>
            <MenuItem
              onClick={() => {
                setRenameOpen(true);
                handleClose();
              }}
            >
              {t('namespace_menu_rename')}
            </MenuItem>
          </Menu>
        )}
        {renameOpen && (
          <NamespaceRenameDialog
            namespace={namespace}
            onClose={() => setRenameOpen(false)}
          />
        )}
      </>
    );
  }
);

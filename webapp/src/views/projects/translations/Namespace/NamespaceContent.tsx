import React, { useState } from 'react';
import { Menu, MenuItem, styled } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

import { useNamespaceFilter } from './useNamespaceFilter';
import { NamespaceRenameDialog } from './NamespaceRenameDialog';
import { NsBannerRecord } from '../context/useNsBanners';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';

const StyledNamespace = styled('div')`
  display: flex;
  align-items: center;
  cursor: pointer;
  background: ${({ theme }) => theme.palette.background.default};
  padding: ${({ theme }) => theme.spacing(0, 1.5, 0, 1.5)};
  padding-bottom: 1px;
  height: 24px;
  position: relative;
  top: -4px;
  border-radius: 12px;
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 0px 7px -1px #000000'
      : '0px 0px 7px -2px #000000'};
  z-index: 1;
  max-width: 100%;
`;

const StyledContent = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledMoreArrow = styled('div')`
  display: flex;
  align-items: center;
  padding-left: 2px;
  margin-right: ${({ theme }) => theme.spacing(-0.5)};
`;

type Props = {
  namespace: NsBannerRecord;
  sticky?: boolean;
};

export const NamespaceContent = React.forwardRef<HTMLDivElement, Props>(
  function NamespaceContent({ namespace, sticky }, ref) {
    const t = useTranslate();
    const { toggle, isActive } = useNamespaceFilter(namespace.name);
    const [open, setOpen] = useState<undefined | HTMLElement>(undefined);
    const [renameOpen, setRenameOpen] = useState(false);
    const permission = useProjectPermissions();
    const canRename = permission.satisfiesPermission(
      ProjectPermissionType.EDIT
    );

    const handleClose = () => {
      setOpen(undefined);
    };

    return (
      <>
        <StyledNamespace
          ref={ref}
          onClick={toggle}
          style={{
            top: sticky ? 0 : -4,
            borderRadius: sticky ? '0px 0px 12px 12px' : 12,
          }}
        >
          <StyledContent role="button" data-cy="namespaces-banner-content">
            {namespace.name || t('namespace_default')}
          </StyledContent>
          {namespace.id !== undefined && (
            <StyledMoreArrow
              role="button"
              data-cy="namespaces-banner-menu-button"
            >
              <ArrowDropDown
                fontSize="small"
                onClick={(e) => {
                  e.preventDefault();
                  e.stopPropagation();
                  setOpen(e.target as HTMLDivElement);
                }}
              />
            </StyledMoreArrow>
          )}
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
            data-cy="namespaces-banner-menu"
          >
            <MenuItem
              data-cy="namespaces-banner-menu-option"
              onClick={() => {
                toggle();
                handleClose();
              }}
            >
              {isActive
                ? t('namespace_menu_filter_cancel')
                : t('namespace_menu_filter', {
                    namespace: namespace.name || t('namespace_default'),
                  })}
            </MenuItem>
            {canRename && (
              <MenuItem
                data-cy="namespaces-banner-menu-option"
                onClick={() => {
                  setRenameOpen(true);
                  handleClose();
                }}
              >
                {t('namespace_menu_rename')}
              </MenuItem>
            )}
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

import React, { useState } from 'react';
import { Menu, MenuItem, styled } from '@mui/material';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { useTranslate } from '@tolgee/react';

import { useNamespaceFilter } from './useNamespaceFilter';
import { NamespaceRenameDialog } from './NamespaceRenameDialog';
import { NsBannerRecord } from '../context/useNsBanners';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

const StyledNamespace = styled('div')`
  display: flex;
  align-items: center;
  cursor: pointer;
  background: ${({ theme }) => theme.palette.background.default};
  padding: ${({ theme }) => theme.spacing(0, 1.5, 0, 1.5)};
  padding-bottom: 1px;
  height: 24px;
  position: relative;
  border-radius: 12px;
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 0px 7px -1px #000000'
      : '0px 0px 7px -2px #00000097'};
  z-index: 1;
  max-width: 100%;
  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[50]};
  }
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
  maxWidth: number | undefined;
  hideShadow?: boolean;
};

export const NamespaceContent = React.forwardRef<HTMLDivElement, Props>(
  function NamespaceContent({ namespace, sticky, maxWidth, hideShadow }, ref) {
    const { t } = useTranslate();
    const { toggle, isActive } = useNamespaceFilter(namespace.name);
    const [open, setOpen] = useState<undefined | HTMLElement>(undefined);
    const [renameOpen, setRenameOpen] = useState(false);
    const permission = useProjectPermissions();
    const canRename =
      permission.satisfiesPermission('keys.edit') &&
      namespace.name &&
      namespace.id !== undefined;

    const handleClose = () => {
      setOpen(undefined);
    };

    return (
      <>
        <StyledNamespace
          ref={ref}
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            setOpen(e.currentTarget as HTMLDivElement);
          }}
          style={{
            top: sticky ? 0 : -12,
            borderRadius: sticky ? '0px 0px 12px 12px' : 12,
            maxWidth: `calc(${maxWidth}px - 5px)`,
            boxShadow: hideShadow ? 'none' : undefined,
          }}
        >
          <StyledContent role="button" data-cy="namespaces-banner-content">
            {namespace.name || t('namespace_default')}
          </StyledContent>
          <StyledMoreArrow
            role="button"
            data-cy="namespaces-banner-menu-button"
          >
            <ArrowDropDown fontSize="small" />
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

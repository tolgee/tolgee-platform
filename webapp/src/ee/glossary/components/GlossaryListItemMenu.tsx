import { IconButton, Menu, MenuItem, Tooltip } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import React, { FC } from 'react';
import { DotsVertical } from '@untitled-ui/icons-react';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';

type GlossaryModel = components['schemas']['GlossaryModel'];

type Props = {
  glossary: GlossaryModel;
  organizationSlug: string;
};

export const GlossaryListItemMenu: FC<Props> = ({
  glossary,
  organizationSlug,
}) => {
  const { t } = useTranslate();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const canManage = true; // TODO: Permissions

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  return (
    <>
      <Tooltip title={t('glossaries_list_more_button')}>
        <IconButton
          onClick={(e) => {
            e.stopPropagation();
            handleOpen(e);
          }}
          data-cy="glossaries-list-more-button"
          aria-label={t('glossaries_list_more_button')}
          size="small"
        >
          <DotsVertical />
        </IconButton>
      </Tooltip>

      <Menu
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        id="glossary-item-menu"
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        onClick={stopBubble()}
      >
        <MenuItem data-cy="glossary-view-button">
          {/*TODO: delete glossary*/}
          <T keyName="glossary_view_button" />
        </MenuItem>
        {canManage && (
          <MenuItem data-cy="glossary-edit-button">
            {/*TODO: open edit screen*/}
            <T keyName="glossary_edit_button" />
          </MenuItem>
        )}
        {canManage && (
          <MenuItem data-cy="glossary-delete-button">
            {/*TODO: delete glossary*/}
            <T keyName="glossary_delete_button" />
          </MenuItem>
        )}
      </Menu>
    </>
  );
};

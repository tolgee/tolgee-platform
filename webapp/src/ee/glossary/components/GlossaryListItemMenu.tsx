import { IconButton, Menu, MenuItem, Tooltip } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import React, { FC } from 'react';
import { DotsVertical } from '@untitled-ui/icons-react';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { GlossaryEditDialog } from 'tg.ee.module/glossary/views/GlossaryEditDialog';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { GlossaryContext } from 'tg.ee.module/glossary/hooks/GlossaryContext';

type SimpleGlossaryModel = components['schemas']['SimpleGlossaryModel'];

type Props = {
  glossary: SimpleGlossaryModel;
};

export const GlossaryListItemMenu: FC<Props> = ({ glossary }) => {
  const { preferredOrganization } = usePreferredOrganization();

  const { t } = useTranslate();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const [isEditing, setIsEditing] = React.useState(false);

  const canManage = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );

  const deleteMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/glossaries',
  });

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const onDelete = () => {
    setAnchorEl(null);
    confirmation({
      title: <T keyName="delete_glossary_confirmation_title" />,
      message: <T keyName="delete_glossary_confirmation_message" />,
      hardModeText: glossary.name.toUpperCase(),
      onConfirm() {
        deleteMutation.mutate({
          path: {
            organizationId: preferredOrganization!.id,
            glossaryId: glossary.id,
          },
        });
      },
    });
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
        <MenuItem
          data-cy="glossary-view-button"
          component={Link}
          to={LINKS.ORGANIZATION_GLOSSARY.build({
            [PARAMS.GLOSSARY_ID]: glossary.id,
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
          })}
        >
          <T keyName="glossary_view_button" />
        </MenuItem>
        {canManage && (
          <MenuItem
            data-cy="glossary-edit-button"
            onClick={() => {
              setAnchorEl(null);
              setIsEditing(true);
            }}
          >
            <T keyName="glossary_edit_button" />
          </MenuItem>
        )}
        {canManage && (
          <MenuItem data-cy="glossary-delete-button" onClick={onDelete}>
            <T keyName="glossary_delete_button" />
          </MenuItem>
        )}
      </Menu>

      {isEditing && (
        <GlossaryContext
          organizationId={preferredOrganization?.id}
          glossaryId={glossary.id}
          fallback={null}
        >
          <GlossaryEditDialog
            open={isEditing}
            onClose={() => setIsEditing(false)}
            onFinished={() => setIsEditing(false)}
          />
        </GlossaryContext>
      )}
    </>
  );
};

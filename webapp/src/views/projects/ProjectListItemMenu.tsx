import { IconButton, Menu, MenuItem, Tooltip } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import React, { FC } from 'react';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { DotsVertical } from '@untitled-ui/icons-react';
import { components } from 'tg.service/apiSchema.generated';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useLeaveProject } from './useLeaveProject';

export const ProjectListItemMenu: FC<{
  projectId: number;
  computedPermission: components['schemas']['ProjectWithStatsModel']['computedPermission'];
  projectName: string;
}> = (props) => {
  const { t } = useTranslate();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const { leave } = useLeaveProject();

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  return (
    <>
      <Tooltip title={t('project_list_more_button')}>
        <IconButton
          onClick={(e) => {
            e.stopPropagation();
            handleOpen(e);
          }}
          data-cy="project-list-more-button"
          aria-label={t('project_list_more_button')}
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
        id="project-item-menu"
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
        onClick={stopBubble()}
      >
        {props.computedPermission?.scopes?.includes('admin') && (
          <MenuItem
            component={Link}
            to={LINKS.PROJECT_EDIT.build({
              [PARAMS.PROJECT_ID]: props.projectId,
            })}
            data-cy="project-settings-button"
          >
            <T keyName="project_settings_button" />
          </MenuItem>
        )}
        <MenuItem
          data-cy="project-leave-button"
          onClick={() => {
            leave(props.projectName, props.projectId);
          }}
        >
          <T keyName="project_leave_button" />
        </MenuItem>
      </Menu>
    </>
  );
};

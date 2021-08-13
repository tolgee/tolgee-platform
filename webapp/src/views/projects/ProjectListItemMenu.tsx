import { IconButton, Menu, MenuItem, Tooltip } from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';
import React, { FC } from 'react';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { MoreVert } from '@material-ui/icons';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';
import { ProjectPermissionType } from 'tg.service/response.types';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';

const messaging = container.resolve(MessageService);

export const ProjectListItemMenu: FC<{
  projectId: number;
  computedPermissions: components['schemas']['ProjectWithStatsModel']['computedPermissions'];
  projectName: string;
}> = (props) => {
  const t = useTranslate();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

  const handleOpen = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const leaveLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/leave',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  return (
    <>
      <Tooltip title={t('project_list_more_button', undefined, true)}>
        <IconButton
          onClick={(e) => {
            e.stopPropagation();
            handleOpen(e);
          }}
          data-cy="project-list-more-button"
          aria-label={t('project_list_more_button')}
          size="small"
        >
          <MoreVert />
        </IconButton>
      </Tooltip>

      <Menu
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
        id="project-item-menu"
        anchorEl={anchorEl}
        getContentAnchorEl={null}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        {props.computedPermissions === ProjectPermissionType.MANAGE && (
          <MenuItem
            component={Link}
            to={LINKS.PROJECT_EDIT.build({
              [PARAMS.PROJECT_ID]: props.projectId,
            })}
            data-cy="project-settings-button"
          >
            <T>project_settings_button</T>
          </MenuItem>
        )}
        <MenuItem
          data-cy="project-leave-button"
          onClick={() => {
            confirmation({
              title: <T>leave_project_confirmation_title</T>,
              message: <T>leave_project_confirmation_message</T>,
              hardModeText: props.projectName.toUpperCase(),
              onConfirm() {
                leaveLoadable.mutate(
                  {
                    path: {
                      projectId: props.projectId,
                    },
                  },
                  {
                    onSuccess() {
                      messaging.success(<T>project_successfully_left</T>);
                    },
                    onError(e) {
                      switch (e.code) {
                        case 'cannot_leave_owning_project':
                          messaging.error(
                            <T>cannot_leave_owning_project_error_message</T>
                          );
                          break;
                        case 'cannot_leave_project_with_organization_role':
                          messaging.error(
                            <T>
                              cannot_leave_project_with_organization_role_error_message
                            </T>
                          );
                          break;
                        default:
                          messaging.error(
                            <T parameters={{ code: e.code }}>
                              unexpected_error_message
                            </T>
                          );
                      }
                    },
                  }
                );
              },
            });
          }}
        >
          <T>project_leave_button</T>
        </MenuItem>
      </Menu>
    </>
  );
};

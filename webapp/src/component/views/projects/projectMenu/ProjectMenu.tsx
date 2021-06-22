import { Divider } from '@material-ui/core';
import List from '@material-ui/core/List';
import { useSelector } from 'react-redux';
import { SideMenuItem } from './SideMenuItem';
import { LINKS, PARAMS } from '../../../../constants/links';
import LanguageIcon from '@material-ui/icons/Language';
import { useProject } from '../../../../hooks/useProject';
import { ProjectPermissionType } from '../../../../service/response.types';
import { useConfig } from '../../../../hooks/useConfig';
import VpnKeyIcon from '@material-ui/icons/VpnKey';
import { useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';
import { GlobalActions } from '../../../../store/global/GlobalActions';
import { AppState } from '../../../../store';
import { SideMenu } from './SideMenu';
import {
  ProjectsIcon,
  SettingsIcon,
  ImportIcon,
  ExportIcon,
  UserSettingIcon,
  UserAddIcon,
} from '../../../CustomIcons';

const actions = container.resolve(GlobalActions);

export const ProjectMenu = ({ id }) => {
  const projectDTO = useProject();
  const config = useConfig();

  const open = useSelector((state: AppState) => state.global.sideMenuOpen);

  const t = useTranslate();

  return (
    <SideMenu
      onSideMenuToggle={() => actions.toggleSideMenu.dispatch()}
      open={open}
    >
      <div data-cy="project-menu-items">
        <List>
          <SideMenuItem
            linkTo={LINKS.PROJECTS.build({ [PARAMS.PROJECT_ID]: id })}
            icon={<ProjectsIcon />}
            text={t('project_menu_projects')}
          />
        </List>
        <Divider />
        <List>
          <SideMenuItem
            linkTo={LINKS.PROJECT_TRANSLATIONS.build({
              [PARAMS.PROJECT_ID]: id,
            })}
            icon={<LanguageIcon />}
            text={t('project_menu_translations')}
          />
        </List>
        <Divider />
        <List>
          {projectDTO.computedPermissions === ProjectPermissionType.MANAGE && (
            <>
              <SideMenuItem
                linkTo={LINKS.PROJECT_EDIT.build({
                  [PARAMS.PROJECT_ID]: id,
                })}
                icon={<SettingsIcon />}
                text={t('project_menu_project_settings')}
              />

              {config.authentication && (
                <>
                  <SideMenuItem
                    linkTo={LINKS.PROJECT_INVITATION.build({
                      [PARAMS.PROJECT_ID]: id,
                    })}
                    icon={<UserAddIcon />}
                    text={t('project_menu_invite_user')}
                  />
                  <SideMenuItem
                    linkTo={LINKS.PROJECT_PERMISSIONS.build({
                      [PARAMS.PROJECT_ID]: id,
                    })}
                    icon={<UserSettingIcon />}
                    text={t('project_menu_permissions')}
                  />
                </>
              )}

              <SideMenuItem
                linkTo={LINKS.PROJECT_IMPORT.build({
                  [PARAMS.PROJECT_ID]: id,
                })}
                icon={<ImportIcon />}
                text={t('project_menu_import')}
              />
            </>
          )}
          <SideMenuItem
            linkTo={LINKS.PROJECT_EXPORT.build({
              [PARAMS.PROJECT_ID]: id,
            })}
            icon={<ExportIcon />}
            text={t('project_menu_export')}
          />
        </List>
        {!config.authentication && (
          <>
            <Divider />
            <List>
              <SideMenuItem
                linkTo={LINKS.USER_API_KEYS.build()}
                icon={<VpnKeyIcon />}
                text={t('project_menu_api_keys')}
              />
            </List>
          </>
        )}
      </div>
    </SideMenu>
  );
};

import { Divider } from '@material-ui/core';
import List from '@material-ui/core/List';
import { SideMenuItem } from '../../layout/sideMenu/SideMenuItem';
import { LINKS, PARAMS } from '../../../constants/links';
import DynamicFeedIcon from '@material-ui/icons/DynamicFeed';
import LanguageIcon from '@material-ui/icons/Language';
import SettingsIcon from '@material-ui/icons/Settings';
import FlagIcon from '@material-ui/icons/Flag';
import PersonAddIcon from '@material-ui/icons/PersonAdd';
import SupervisedUserCircleIcon from '@material-ui/icons/SupervisedUserCircle';
import * as React from 'react';
import { useProject } from '../../../hooks/useProject';
import { ProjectPermissionType } from '../../../service/response.types';
import ImportExportIcon from '@material-ui/icons/ImportExport';
import SaveAltIcon from '@material-ui/icons/SaveAlt';
import { useConfig } from '../../../hooks/useConfig';
import VpnKeyIcon from '@material-ui/icons/VpnKey';
import { useTranslate } from '@tolgee/react';

export const ProjectMenu = ({ id }) => {
  let projectDTO = useProject();
  let config = useConfig();

  const t = useTranslate();

  return (
    <div data-cy="project-menu-items">
      <List>
        <SideMenuItem
          linkTo={LINKS.PROJECTS.build({ [PARAMS.REPOSITORY_ID]: id })}
          icon={<DynamicFeedIcon />}
          text={t('project_menu_projects')}
        />
      </List>
      <Divider />
      <List>
        <SideMenuItem
          linkTo={LINKS.REPOSITORY_TRANSLATIONS.build({
            [PARAMS.REPOSITORY_ID]: id,
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
              linkTo={LINKS.REPOSITORY_EDIT.build({
                [PARAMS.REPOSITORY_ID]: id,
              })}
              icon={<SettingsIcon />}
              text={t('project_menu_project_settings')}
            />
            <SideMenuItem
              linkTo={LINKS.REPOSITORY_LANGUAGES.build({
                [PARAMS.REPOSITORY_ID]: id,
              })}
              icon={<FlagIcon />}
              text={t('project_menu_languages')}
            />

            {config.authentication && (
              <>
                <SideMenuItem
                  linkTo={LINKS.REPOSITORY_INVITATION.build({
                    [PARAMS.REPOSITORY_ID]: id,
                  })}
                  icon={<PersonAddIcon />}
                  text={t('project_menu_invite_user')}
                />
                <SideMenuItem
                  linkTo={LINKS.REPOSITORY_PERMISSIONS.build({
                    [PARAMS.REPOSITORY_ID]: id,
                  })}
                  icon={<SupervisedUserCircleIcon />}
                  text={t('project_menu_permissions')}
                />
              </>
            )}

            <SideMenuItem
              linkTo={LINKS.REPOSITORY_IMPORT.build({
                [PARAMS.REPOSITORY_ID]: id,
              })}
              icon={<ImportExportIcon />}
              text={t('project_menu_import')}
            />
          </>
        )}
        <SideMenuItem
          linkTo={LINKS.REPOSITORY_EXPORT.build({ [PARAMS.REPOSITORY_ID]: id })}
          icon={<SaveAltIcon />}
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
  );
};

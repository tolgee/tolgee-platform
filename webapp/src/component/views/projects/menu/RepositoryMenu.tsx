import { Divider } from '@material-ui/core';
import List from '@material-ui/core/List';
import { useSelector } from 'react-redux';
import { SideMenuItem } from './SideMenuItem';
import { LINKS, PARAMS } from '../../../../constants/links';
import DynamicFeedIcon from '@material-ui/icons/DynamicFeed';
import DashboardIcon from '@material-ui/icons/Dashboard';
import LanguageIcon from '@material-ui/icons/Language';
import SettingsIcon from '@material-ui/icons/Settings';
import FlagIcon from '@material-ui/icons/Flag';
import PersonAddIcon from '@material-ui/icons/PersonAdd';
import SupervisedUserCircleIcon from '@material-ui/icons/SupervisedUserCircle';
import { useRepository } from '../../../../hooks/useRepository';
import { RepositoryPermissionType } from '../../../../service/response.types';
import ImportExportIcon from '@material-ui/icons/ImportExport';
import SaveAltIcon from '@material-ui/icons/SaveAlt';
import { useConfig } from '../../../../hooks/useConfig';
import VpnKeyIcon from '@material-ui/icons/VpnKey';
import { useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';
import { GlobalActions } from '../../../../store/global/GlobalActions';
import { AppState } from '../../../../store';
import { SideMenu } from './SideMenu';

const actions = container.resolve(GlobalActions);

export const RepositoryMenu = ({ id }) => {
  const repositoryDTO = useRepository();
  const config = useConfig();

  const open = useSelector((state: AppState) => state.global.sideMenuOpen);

  const t = useTranslate();

  return (
    <SideMenu
      onSideMenuToggle={() => actions.toggleSideMenu.dispatch()}
      open={open}
    >
      <div data-cy="repository-menu-items">
        <List>
          <SideMenuItem
            linkTo={LINKS.REPOSITORIES.build({ [PARAMS.REPOSITORY_ID]: id })}
            icon={<DynamicFeedIcon />}
            text={t('repository_menu_repositories')}
          />
        </List>
        <Divider />
        <List>
          <SideMenuItem
            linkTo={LINKS.REPOSITORY.build({
              [PARAMS.REPOSITORY_ID]: id,
            })}
            icon={<DashboardIcon />}
            text={t('repository_menu_overview')}
          />
        </List>
        <List>
          <SideMenuItem
            linkTo={LINKS.REPOSITORY_TRANSLATIONS.build({
              [PARAMS.REPOSITORY_ID]: id,
            })}
            icon={<LanguageIcon />}
            text={t('repository_menu_translations')}
          />
        </List>
        <Divider />
        <List>
          {repositoryDTO.computedPermissions ===
            RepositoryPermissionType.MANAGE && (
            <>
              <SideMenuItem
                linkTo={LINKS.REPOSITORY_EDIT.build({
                  [PARAMS.REPOSITORY_ID]: id,
                })}
                icon={<SettingsIcon />}
                text={t('repository_menu_repository_settings')}
              />
              <SideMenuItem
                linkTo={LINKS.REPOSITORY_LANGUAGES.build({
                  [PARAMS.REPOSITORY_ID]: id,
                })}
                icon={<FlagIcon />}
                text={t('repository_menu_languages')}
              />

              {config.authentication && (
                <>
                  <SideMenuItem
                    linkTo={LINKS.REPOSITORY_INVITATION.build({
                      [PARAMS.REPOSITORY_ID]: id,
                    })}
                    icon={<PersonAddIcon />}
                    text={t('repository_menu_invite_user')}
                  />
                  <SideMenuItem
                    linkTo={LINKS.REPOSITORY_PERMISSIONS.build({
                      [PARAMS.REPOSITORY_ID]: id,
                    })}
                    icon={<SupervisedUserCircleIcon />}
                    text={t('repository_menu_permissions')}
                  />
                </>
              )}

              <SideMenuItem
                linkTo={LINKS.REPOSITORY_IMPORT.build({
                  [PARAMS.REPOSITORY_ID]: id,
                })}
                icon={<ImportExportIcon />}
                text={t('repository_menu_import')}
              />
            </>
          )}
          <SideMenuItem
            linkTo={LINKS.REPOSITORY_EXPORT.build({
              [PARAMS.REPOSITORY_ID]: id,
            })}
            icon={<SaveAltIcon />}
            text={t('repository_menu_export')}
          />
        </List>
        {!config.authentication && (
          <>
            <Divider />
            <List>
              <SideMenuItem
                linkTo={LINKS.USER_API_KEYS.build()}
                icon={<VpnKeyIcon />}
                text={t('repository_menu_api_keys')}
              />
            </List>
          </>
        )}
      </div>
    </SideMenu>
  );
};

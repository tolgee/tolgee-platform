import { Devices, PersonOutline, VpnKey } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';
import LanguageIcon from '@mui/icons-material/Language';
import {
  ExportIcon,
  ImportIcon,
  ProjectsIcon,
  SettingsIcon,
  TranslationIcon,
} from 'tg.component/CustomIcons';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { useProject } from 'tg.hooks/useProject';
import { ProjectPermissionType } from 'tg.service/response.types';

import { SideMenu } from './SideMenu';
import { SideMenuItem } from './SideMenuItem';
import { SideLogo } from './SideLogo';
import { useTopBarHidden } from 'tg.component/layout/TopBar/TopBarContext';
import DashboardIcon from '@mui/icons-material/Dashboard';

export const ProjectMenu = ({ id }) => {
  const projectDTO = useProject();
  const config = useConfig();

  const t = useTranslate();

  const topBarHidden = useTopBarHidden();

  return (
    <SideMenu>
      <SideLogo hidden={!topBarHidden} />
      <SideMenuItem
        linkTo={LINKS.PROJECTS.build({ [PARAMS.PROJECT_ID]: id })}
        icon={<ProjectsIcon />}
        text={t('project_menu_projects')}
      />
      <SideMenuItem
        linkTo={LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: id })}
        icon={<DashboardIcon />}
        text={t('project_menu_dashboard', 'Project Dashboard')}
      />
      <SideMenuItem
        linkTo={LINKS.PROJECT_TRANSLATIONS.build({
          [PARAMS.PROJECT_ID]: id,
        })}
        icon={<TranslationIcon />}
        text={t('project_menu_translations')}
        matchAsPrefix
      />
      {projectDTO.computedPermissions.type === ProjectPermissionType.MANAGE && (
        <>
          <SideMenuItem
            linkTo={LINKS.PROJECT_EDIT.build({
              [PARAMS.PROJECT_ID]: id,
            })}
            matchAsPrefix
            icon={<SettingsIcon />}
            text={t('project_menu_project_settings')}
          />
          <SideMenuItem
            linkTo={LINKS.PROJECT_LANGUAGES.build({
              [PARAMS.PROJECT_ID]: id,
            })}
            matchAsPrefix
            icon={<LanguageIcon />}
            text={t('project_menu_languages')}
          />
          {config.authentication && (
            <>
              <SideMenuItem
                linkTo={LINKS.PROJECT_PERMISSIONS.build({
                  [PARAMS.PROJECT_ID]: id,
                })}
                icon={<PersonOutline />}
                text={t('project_menu_members')}
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
      <SideMenuItem
        linkTo={LINKS.PROJECT_INTEGRATE.build({
          [PARAMS.PROJECT_ID]: id,
        })}
        icon={<Devices />}
        text={t('project_menu_integrate')}
      />
      {!config.authentication && (
        <SideMenuItem
          linkTo={LINKS.USER_API_KEYS.build()}
          icon={<VpnKey />}
          text={t('project_menu_api_keys')}
        />
      )}
    </SideMenu>
  );
};

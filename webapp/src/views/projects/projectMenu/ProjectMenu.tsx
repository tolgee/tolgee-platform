import { Devices, PersonOutline, VpnKey, Code } from '@mui/icons-material';
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

import { SideMenu } from './SideMenu';
import { SideMenuItem } from './SideMenuItem';
import { SideLogo } from './SideLogo';
import DashboardIcon from '@mui/icons-material/Dashboard';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const ProjectMenu = ({ id }) => {
  const { satisfiesPermission } = useProjectPermissions();
  const config = useConfig();

  const canViewKeys = satisfiesPermission('keys.view');
  const canViewTranslations = satisfiesPermission('translations.view');
  const canEditProject = satisfiesPermission('project.edit');
  const canEditLanguages = satisfiesPermission('languages.edit');
  const canViewUsers =
    config.authentication && satisfiesPermission('members.view');
  const canImport = canViewKeys && satisfiesPermission('translations.edit');
  const canIntegrate = canViewKeys;
  const canPublishCd = satisfiesPermission('content-delivery.publish');
  const canManageWebhooks = satisfiesPermission('webhooks.manage');
  const canViewDeveloper = canPublishCd || canManageWebhooks;

  const { t } = useTranslate();

  const topBarHeight = useGlobalContext((c) => c.topBarHeight);

  return (
    <SideMenu>
      <SideLogo hidden={!topBarHeight} />
      <SideMenuItem
        linkTo={LINKS.PROJECTS.build({ [PARAMS.PROJECT_ID]: id })}
        icon={<ProjectsIcon />}
        text={t('project_menu_projects')}
        data-cy="project-menu-item-projects"
      />
      <SideMenuItem
        linkTo={LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: id })}
        icon={<DashboardIcon />}
        text={t('project_menu_dashboard', 'Project Dashboard')}
        data-cy="project-menu-item-dashboard"
      />
      {canViewKeys && (
        <SideMenuItem
          linkTo={LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: id,
          })}
          icon={<TranslationIcon />}
          text={t('project_menu_translations')}
          data-cy="project-menu-item-translations"
          matchAsPrefix
          quickStart={{ itemKey: 'menu_translations' }}
        />
      )}
      <>
        {canEditProject && (
          <SideMenuItem
            linkTo={LINKS.PROJECT_EDIT.build({
              [PARAMS.PROJECT_ID]: id,
            })}
            matchAsPrefix
            icon={<SettingsIcon />}
            text={t('project_menu_project_settings')}
            data-cy="project-menu-item-settings"
            quickStart={{ itemKey: 'menu_settings' }}
          />
        )}
        {canEditLanguages && (
          <SideMenuItem
            linkTo={LINKS.PROJECT_LANGUAGES.build({
              [PARAMS.PROJECT_ID]: id,
            })}
            matchAsPrefix
            icon={<LanguageIcon />}
            text={t('project_menu_languages')}
            data-cy="project-menu-item-languages"
            quickStart={{
              itemKey: 'menu_languages',
            }}
          />
        )}
        {canViewUsers && (
          <>
            <SideMenuItem
              linkTo={LINKS.PROJECT_PERMISSIONS.build({
                [PARAMS.PROJECT_ID]: id,
              })}
              icon={<PersonOutline />}
              text={t('project_menu_members')}
              data-cy="project-menu-item-members"
              quickStart={{
                itemKey: 'menu_members',
              }}
            />
          </>
        )}
        {canImport && (
          <SideMenuItem
            linkTo={LINKS.PROJECT_IMPORT.build({
              [PARAMS.PROJECT_ID]: id,
            })}
            icon={<ImportIcon />}
            text={t('project_menu_import')}
            data-cy="project-menu-item-import"
            quickStart={{ itemKey: 'menu_import' }}
          />
        )}
      </>

      {canViewTranslations && (
        <SideMenuItem
          linkTo={LINKS.PROJECT_EXPORT.build({
            [PARAMS.PROJECT_ID]: id,
          })}
          icon={<ExportIcon />}
          text={t('project_menu_export')}
          data-cy="project-menu-item-export"
          quickStart={{ itemKey: 'menu_export' }}
        />
      )}
      {canViewDeveloper && (
        <SideMenuItem
          linkTo={(canPublishCd
            ? LINKS.PROJECT_CONTENT_STORAGE
            : LINKS.PROJECT_WEBHOOKS
          ).build({
            [PARAMS.PROJECT_ID]: id,
          })}
          icon={<Code />}
          text={t('project_menu_developer')}
          data-cy="project-menu-item-developer"
          matchAsPrefix={LINKS.PROJECT_DEVELOPER.build({
            [PARAMS.PROJECT_ID]: id,
          })}
        />
      )}
      {canIntegrate && (
        <SideMenuItem
          linkTo={LINKS.PROJECT_INTEGRATE.build({
            [PARAMS.PROJECT_ID]: id,
          })}
          icon={<Devices />}
          text={t('project_menu_integrate')}
          data-cy="project-menu-item-integrate"
          quickStart={{ itemKey: 'menu_integrate' }}
        />
      )}
      {!config.authentication && (
        <SideMenuItem
          linkTo={LINKS.USER_API_KEYS.build()}
          icon={<VpnKey />}
          text={t('project_menu_api_keys')}
          data-cy="project-menu-item-api-keys"
        />
      )}
    </SideMenu>
  );
};

import { useTranslate } from '@tolgee/react';
import {
  ClipboardCheck,
  Code02,
  FileDownload03,
  Globe01,
  HomeLine,
  LayoutAlt04,
  Settings01,
  Translate01,
  UploadCloud02,
  User01,
} from '@untitled-ui/icons-react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';

import { SideMenu } from './SideMenu';
import { SideMenuItem } from './SideMenuItem';
import { SideLogo } from './SideLogo';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { Integration } from 'tg.component/CustomIcons';

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
  const canViewTasks = satisfiesPermission('tasks.view');

  const { t } = useTranslate();

  const topBarHeight = useGlobalContext((c) => c.layout.topBarHeight);

  return (
    <SideMenu>
      <SideLogo hidden={!topBarHeight} />
      <SideMenuItem
        linkTo={LINKS.PROJECTS.build({ [PARAMS.PROJECT_ID]: id })}
        icon={<HomeLine />}
        text={t('project_menu_projects')}
        data-cy="project-menu-item-projects"
      />
      <SideMenuItem
        linkTo={LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: id })}
        icon={<LayoutAlt04 />}
        text={t('project_menu_dashboard', 'Project Dashboard')}
        data-cy="project-menu-item-dashboard"
      />
      {canViewKeys && (
        <SideMenuItem
          linkTo={LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: id,
          })}
          icon={<Translate01 />}
          text={t('project_menu_translations')}
          data-cy="project-menu-item-translations"
          matchAsPrefix
          quickStart={{ itemKey: 'menu_translations' }}
        />
      )}
      {canViewTasks && (
        <SideMenuItem
          linkTo={LINKS.PROJECT_TASKS.build({
            [PARAMS.PROJECT_ID]: id,
          })}
          icon={<ClipboardCheck />}
          text={t('project_menu_tasks')}
          data-cy="project-menu-item-tasks"
          matchAsPrefix
        />
      )}

      {canEditLanguages && (
        <SideMenuItem
          linkTo={LINKS.PROJECT_LANGUAGES.build({
            [PARAMS.PROJECT_ID]: id,
          })}
          matchAsPrefix
          icon={<Globe01 />}
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
            icon={<User01 />}
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
          icon={<UploadCloud02 />}
          text={t('project_menu_import')}
          data-cy="project-menu-item-import"
          quickStart={{ itemKey: 'menu_import' }}
        />
      )}

      {canViewTranslations && (
        <SideMenuItem
          linkTo={LINKS.PROJECT_EXPORT.build({
            [PARAMS.PROJECT_ID]: id,
          })}
          icon={<FileDownload03 />}
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
          icon={<Code02 />}
          text={t('project_menu_developer')}
          data-cy="project-menu-item-developer"
          quickStart={{ itemKey: 'menu_developer' }}
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
          icon={<Integration />}
          text={t('project_menu_integrate')}
          data-cy="project-menu-item-integrate"
          quickStart={{ itemKey: 'menu_integrate' }}
        />
      )}
      {canEditProject && (
        <SideMenuItem
          linkTo={LINKS.PROJECT_EDIT.build({
            [PARAMS.PROJECT_ID]: id,
          })}
          matchAsPrefix
          icon={<Settings01 />}
          text={t('project_menu_project_settings')}
          data-cy="project-menu-item-settings"
          quickStart={{ itemKey: 'menu_settings' }}
        />
      )}
    </SideMenu>
  );
};

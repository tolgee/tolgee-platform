import { useTranslate } from '@tolgee/react';
import {
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
import { Link, LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';

import { SideMenu } from './SideMenu';
import { SideMenuItem, SideMenuItemQuickStart } from './SideMenuItem';
import { SideLogo } from './SideLogo';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { Integration, Stars } from 'tg.component/CustomIcons';
import { FC } from 'react';
import { createAdder } from 'tg.fixtures/pluginAdder';
import { useAddProjectMenuItems } from 'tg.ee';
import { useProject } from 'tg.hooks/useProject';
import { useBranchLinks } from 'tg.component/branching/useBranchLinks';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useAppTriggers, useAppTriggerDispatch } from '../apps/useAppTriggers';
import { AppIcon } from '../apps/AppIcon';

export const ProjectMenu = () => {
  const project = useProject();
  const { withBranchLink } = useBranchLinks();
  const { satisfiesPermission } = useProjectPermissions();
  const config = useConfig();
  const canPublishCd = satisfiesPermission('content-delivery.publish');
  const projectIdParams = { [PARAMS.PROJECT_ID]: project.id };

  const { t } = useTranslate();

  const topBarHeight = useGlobalContext((c) => c.layout.topBarHeight);

  const baseItems = [
    {
      id: 'projects',
      condition: () => true,
      link: LINKS.PROJECTS,
      icon: HomeLine,
      text: t('project_menu_projects'),
      dataCy: 'project-menu-item-projects',
    },
    {
      id: 'dashboard',
      condition: () => true,
      link: LINKS.PROJECT_DASHBOARD,
      icon: LayoutAlt04,
      text: t('project_menu_dashboard', 'Project Dashboard'),
      dataCy: 'project-menu-item-dashboard',
    },
    {
      id: 'translations',
      condition: ({ satisfiesPermission }) => satisfiesPermission('keys.view'),
      link: LINKS.PROJECT_TRANSLATIONS,
      icon: Translate01,
      text: t('project_menu_translations'),
      dataCy: 'project-menu-item-translations',
      matchAsPrefix: true,
      quickStart: { itemKey: 'menu_translations' },
    },
    {
      id: 'languages',
      condition: ({ satisfiesPermission }) =>
        satisfiesPermission('languages.edit'),
      link: LINKS.PROJECT_LANGUAGES,
      icon: Globe01,
      text: t('project_menu_languages'),
      dataCy: 'project-menu-item-languages',
      matchAsPrefix: true,
      quickStart: { itemKey: 'menu_languages' },
    },
    {
      id: 'members',
      condition: ({ satisfiesPermission }) =>
        config.authentication && satisfiesPermission('members.view'),
      link: LINKS.PROJECT_PERMISSIONS,
      icon: User01,
      text: t('project_menu_members'),
      dataCy: 'project-menu-item-members',
      quickStart: { itemKey: 'menu_members' },
    },
    {
      id: 'import',
      condition: ({ satisfiesPermission }) =>
        satisfiesPermission('translations.edit') &&
        satisfiesPermission('keys.view'),
      link: LINKS.PROJECT_IMPORT,
      icon: UploadCloud02,
      text: t('project_menu_import'),
      dataCy: 'project-menu-item-import',
      quickStart: { itemKey: 'menu_import' },
    },
    {
      id: 'export',
      condition: ({ satisfiesPermission }) =>
        satisfiesPermission('translations.view'),
      link: LINKS.PROJECT_EXPORT,
      icon: FileDownload03,
      text: t('project_menu_export'),
      dataCy: 'project-menu-item-export',
      quickStart: { itemKey: 'menu_export' },
    },
    {
      id: 'developer',
      condition: ({ satisfiesPermission }) =>
        satisfiesPermission('content-delivery.publish') ||
        satisfiesPermission('webhooks.manage'),
      link: canPublishCd
        ? LINKS.PROJECT_CONTENT_STORAGE
        : LINKS.PROJECT_WEBHOOKS,
      icon: Code02,
      text: t('project_menu_developer'),
      dataCy: 'project-menu-item-developer',
      quickStart: { itemKey: 'menu_developer' },
      matchAsPrefix: LINKS.PROJECT_DEVELOPER.build(projectIdParams),
    },
    {
      id: 'integrate',
      condition: ({ satisfiesPermission }) => satisfiesPermission('keys.view'),
      link: LINKS.PROJECT_INTEGRATE,
      icon: Integration,
      text: t('project_menu_integrate'),
      dataCy: 'project-menu-item-integrate',
      quickStart: { itemKey: 'menu_integrate' },
    },
    {
      id: 'ai',
      condition: ({ satisfiesPermission }) =>
        satisfiesPermission('prompts.view') && config.llm.enabled,
      link: LINKS.PROJECT_CONTEXT_DATA,
      icon: Stars,
      text: t('project_menu_ai'),
      matchAsPrefix: LINKS.PROJECT_AI.build(projectIdParams),
      dataCy: 'project-menu-item-ai',
    },
    {
      id: 'settings',
      condition: ({ satisfiesPermission }) =>
        satisfiesPermission('project.edit') ||
        satisfiesPermission('translation-labels.manage'),
      link: LINKS.PROJECT_EDIT,
      icon: Settings01,
      text: t('project_menu_project_settings'),
      dataCy: 'project-menu-item-settings',
      matchAsPrefix: true,
      quickStart: { itemKey: 'menu_settings' },
    },
  ] satisfies ProjectMenuItem[];

  const addEeItems = useAddProjectMenuItems();

  const items = addEeItems(baseItems);

  const projectApps = useApiQuery({
    url: '/v2/projects/{projectId}/apps',
    method: 'get',
    path: { projectId: project.id },
  });

  const enabledAppPages = (projectApps.data?._embedded?.projectApps ?? [])
    .filter((app) => app.enabled)
    .flatMap((app) =>
      (app.modules?.['project-dashboard-page'] ?? []).map((module) => ({
        installId: app.id,
        appName: app.name,
        moduleKey: module.key,
        title: module.title,
        icon: module.icon,
      }))
    );

  return (
    <SideMenu>
      <SideLogo hidden={!topBarHeight} />
      {items.map((item, index) => {
        if (!item.condition({ config, satisfiesPermission, project }))
          return null;
        const { dataCy, icon: Icon, link, ...rest } = item;
        return (
          <SideMenuItem
            key={item.id}
            linkTo={withBranchLink(link, {
              [PARAMS.PROJECT_ID]: project.id,
              [PARAMS.ORGANIZATION_SLUG]: project.organizationOwner?.slug || '',
            })}
            {...rest}
            icon={<Icon />}
            data-cy={dataCy}
          />
        );
      })}
      {enabledAppPages.map((page) => (
        <SideMenuItem
          key={`app-${page.installId}-${page.moduleKey}`}
          linkTo={LINKS.PROJECT_APP_PAGE.build({
            [PARAMS.PROJECT_ID]: project.id,
            [PARAMS.APP_INSTALL_ID]: page.installId,
            [PARAMS.APP_MODULE_KEY]: page.moduleKey,
          })}
          text={page.title}
          icon={<AppIcon icon={page.icon} fontSize="1em" />}
          data-cy={`project-menu-item-app-${page.installId}-${page.moduleKey}`}
        />
      ))}
      <ProjectMenuActionItems />
    </SideMenu>
  );
};

const ProjectMenuActionItems = () => {
  const project = useProject();
  const triggers = useAppTriggers(project.id, 'project-menu-action');
  const dispatch = useAppTriggerDispatch();
  return (
    <>
      {triggers.map((trigger) => (
        <SideMenuItem
          key={`app-action-${trigger.install.id}-${trigger.item.key}`}
          onClick={() =>
            dispatch(trigger, { templateVars: { projectId: project.id } })
          }
          text={trigger.item.title ?? trigger.item.key}
          icon={<AppIcon icon={trigger.item.icon ?? '🔘'} fontSize="1em" />}
          data-cy={`project-menu-item-app-action-${trigger.install.id}-${trigger.item.key}`}
        />
      ))}
    </>
  );
};

export type ProjectMenuItem = {
  id: string;
  condition: (props: ConditionProps) => boolean;
  link: Link;
  icon: FC<React.PropsWithChildren<unknown>>;
  text: string;
  dataCy: string;
  matchAsPrefix?: boolean | string;
  quickStart?: SideMenuItemQuickStart;
};

type ConditionProps = {
  config: ReturnType<typeof useConfig>;
  satisfiesPermission: ReturnType<
    typeof useProjectPermissions
  >['satisfiesPermission'];
  project: ReturnType<typeof useProject>;
};

export const addProjectMenuItems = createAdder<ProjectMenuItem>({
  referencingProperty: 'id',
});

export type ProjectMenuItemsAdder = ReturnType<typeof addProjectMenuItems>;

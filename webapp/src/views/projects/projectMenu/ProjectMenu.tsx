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
import { Integration } from 'tg.component/CustomIcons';
import { FC } from 'react';
import { createAdder } from 'tg.fixtures/pluginAdder';
import { useAddProjectMenuItems } from 'tg.ee';
import { useProject } from 'tg.hooks/useProject';

export const ProjectMenu = () => {
  const project = useProject();
  const { satisfiesPermission } = useProjectPermissions();
  const config = useConfig();
  const canPublishCd = satisfiesPermission('content-delivery.publish');

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
    // {
    //   id: 'glossaries',
    //   condition: () => true,
    //   link: LINKS.ORGANIZATION_GLOSSARIES,
    //   icon: BookClosed,
    //   text: t('project_menu_glossaries'),
    //   dataCy: 'project-menu-item-glossaries',
    //   matchAsPrefix: true,
    //   // TODO: quickstart?
    // },
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
      matchAsPrefix: LINKS.PROJECT_DEVELOPER.build({
        [PARAMS.PROJECT_ID]: project.id,
      }),
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
      id: 'settings',
      condition: ({ satisfiesPermission }) =>
        satisfiesPermission('project.edit'),
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

  return (
    <SideMenu>
      <SideLogo hidden={!topBarHeight} />
      {items.map((item, index) => {
        if (!item.condition({ config, satisfiesPermission })) return null;
        const { dataCy, icon: Icon, link, ...rest } = item;
        return (
          <SideMenuItem
            key={item.id}
            linkTo={link.build({
              [PARAMS.PROJECT_ID]: project.id,
              [PARAMS.ORGANIZATION_SLUG]: project.organizationOwner?.slug || '',
            })}
            {...rest}
            icon={<Icon />}
            data-cy={dataCy}
          />
        );
      })}
    </SideMenu>
  );
};

export type ProjectMenuItem = {
  id: string;
  condition: (props: ConditionProps) => boolean;
  link: Link;
  icon: FC;
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
};

export const addProjectMenuItems = createAdder<ProjectMenuItem>({
  referencingProperty: 'id',
});

export type ProjectMenuItemsAdder = ReturnType<typeof addProjectMenuItems>;

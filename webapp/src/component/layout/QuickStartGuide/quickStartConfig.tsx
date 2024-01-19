import { T } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { ItemType } from './types';

export const items: ItemType[] = [
  {
    step: 'new_project',
    name: <T keyName="guide_new_project" />,
    actions: () => [
      {
        link: LINKS.PROJECTS.build(),
        label: <T keyName="guide_new_project_demo" />,
        highlightItems: ['demo_project'],
      },
      {
        link: LINKS.PROJECTS.build(),
        label: <T keyName="guide_new_project_create" />,
        highlightItems: ['add_project', 'add_project_submit'],
      },
    ],
  },
  {
    step: 'languages',
    name: <T keyName="guide_languages" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_languages_set_up" />,
        highlightItems: [
          'menu_languages',
          'add_language',
          'machine_translation_tab',
          'machine_translation',
        ],
      },
    ],
  },
  {
    step: 'members',
    name: <T keyName="guide_members" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_members_invite" />,
        highlightItems: ['menu_members', 'invitations', 'members'],
      },
    ],
  },
  {
    step: 'keys',
    name: <T keyName="guide_keys" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_keys_add" />,
        highlightItems: ['menu_translations', 'add_key'],
      },
      {
        label: <T keyName="guide_keys_import" />,
        highlightItems: ['menu_import', 'pick_import_file'],
      },
    ],
  },
  {
    step: 'use',
    name: <T keyName="guide_use" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_use_integrate" />,
        highlightItems: ['menu_integrate', 'integrate_form'],
      },
    ],
  },
  {
    step: 'production',
    name: <T keyName="guide_production" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_production_content_delivery" />,
        highlightItems: ['menu_developer', 'content_delivery_page'],
      },
      {
        label: <T keyName="guide_use_export" />,
        highlightItems: ['menu_export', 'export_form'],
      },
    ],
  },
];

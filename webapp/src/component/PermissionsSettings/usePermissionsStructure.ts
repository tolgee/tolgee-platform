import { useTranslate } from '@tolgee/react';
import { HierarchyType } from './types';
import { Scope } from 'tg.fixtures/permissions';

export function limitStructureToOptions(
  structure: HierarchyType[],
  options: Scope[]
) {
  const result = [] as HierarchyType[];

  structure.forEach((item) => {
    const children = limitStructureToOptions(item.children || [], options);
    if (
      (item.value === undefined && children.length) ||
      (item.value !== undefined && options.includes(item.value))
    ) {
      result.push({
        ...item,
        children,
      });
    } else if (item.children) {
      result.push(...children);
    }
  });

  return result;
}

export const usePermissionsStructure = (options?: Scope[]) => {
  const { t } = useTranslate();

  return {
    value: 'admin',
    children: [
      {
        label: t('permissions_item_keys'),
        children: [
          {
            value: 'keys.view',
          },
          {
            value: 'keys.create',
          },
          {
            value: 'keys.edit',
          },
          {
            value: 'keys.delete',
          },
        ],
      },
      {
        label: t('permissions_item_translations'),
        children: [
          {
            value: 'translations.view',
          },
          {
            value: 'translations.edit',
          },
          {
            value: 'translations.state-edit',
          },
          {
            label: t('permissions_item_translations_comments'),
            children: [
              {
                value: 'translation-comments.add',
              },
              {
                value: 'translation-comments.edit',
              },
              {
                value: 'translation-comments.set-state',
              },
            ],
          },
        ],
      },
      {
        label: t('permissions_item_screenshots'),
        children: [
          {
            value: 'screenshots.view',
          },
          {
            value: 'screenshots.upload',
          },
          {
            value: 'screenshots.delete',
          },
        ],
      },
      {
        label: t('permissions_item_batch_operations'),
        children: [
          { value: 'batch-jobs.view' },
          { value: 'batch-jobs.cancel' },
          { value: 'translations.batch-by-tm' },
          { value: 'translations.batch-machine' },
        ],
      },
      {
        label: t('permissions_item_members'),
        children: [
          {
            value: 'members.view',
          },
          {
            value: 'members.edit',
          },
        ],
      },
      {
        label: t('permissions_content_delivery'),
        children: [
          {
            value: 'content-delivery.publish',
          },
          {
            value: 'content-delivery.manage',
          },
        ],
      },
      {
        label: t('permissions_tasks'),
        children: [
          {
            value: 'tasks.view',
          },
          {
            value: 'tasks.edit',
          },
        ],
      },
      {
        label: t('permissions_prompts'),
        children: [
          {
            value: 'prompts.view',
          },
          {
            value: 'prompts.edit',
          },
        ],
      },
      {
        value: 'webhooks.manage',
      },
      {
        value: 'project.edit',
      },
      {
        value: 'activity.view',
      },
      {
        value: 'languages.edit',
      },
    ],
  } as HierarchyType;
};

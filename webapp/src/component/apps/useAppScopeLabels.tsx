import { useTranslate } from '@tolgee/react';
import { PermissionModelScope } from 'tg.component/PermissionsSettings/types';

/**
 * Self-contained scope labels for the apps UI. The `permissions_item_*` translations used by the
 * permission settings hierarchy are fragments ("Edit") that only make sense under their group
 * heading ("Keys") — rendered standalone in a chip they collapse into "Edit / View / View".
 */
export const useAppScopeLabels = () => {
  const { t } = useTranslate();

  const labels: Partial<Record<PermissionModelScope, string>> = {
    admin: t('app_scope_label_admin', 'Administer project'),
    'translations.view': t(
      'app_scope_label_translations_view',
      'View translations'
    ),
    'translations.edit': t(
      'app_scope_label_translations_edit',
      'Edit translations'
    ),
    'translations.suggest': t(
      'app_scope_label_translations_suggest',
      'Suggest translations'
    ),
    'translation-suggestions.manage': t(
      'app_scope_label_translation_suggestions_manage',
      'Manage translation suggestions'
    ),
    'translation-comments.add': t(
      'app_scope_label_translation_comments_add',
      'Add translation comments'
    ),
    'translation-comments.edit': t(
      'app_scope_label_translation_comments_edit',
      'Edit translation comments'
    ),
    'translation-comments.set-state': t(
      'app_scope_label_translation_comments_set_state',
      'Resolve translation comments'
    ),
    'translations.state-edit': t(
      'app_scope_label_translations_state_edit',
      'Change translation states'
    ),
    'screenshots.view': t(
      'app_scope_label_screenshots_view',
      'View screenshots'
    ),
    'screenshots.upload': t(
      'app_scope_label_screenshots_upload',
      'Upload screenshots'
    ),
    'screenshots.delete': t(
      'app_scope_label_screenshots_delete',
      'Delete screenshots'
    ),
    'keys.create': t('app_scope_label_keys_create', 'Create keys'),
    'keys.edit': t('app_scope_label_keys_edit', 'Edit keys'),
    'keys.view': t('app_scope_label_keys_view', 'View keys'),
    'keys.delete': t('app_scope_label_keys_delete', 'Delete keys'),
    'project.edit': t('app_scope_label_project_edit', 'Edit project settings'),
    'members.view': t('app_scope_label_members_view', 'View members'),
    'members.edit': t('app_scope_label_members_edit', 'Manage members'),
    'languages.edit': t('app_scope_label_languages_edit', 'Manage languages'),
    'activity.view': t('app_scope_label_activity_view', 'View activity'),
    'batch-jobs.view': t('app_scope_label_batch_jobs_view', 'View batch jobs'),
    'batch-jobs.cancel': t(
      'app_scope_label_batch_jobs_cancel',
      'Cancel batch jobs'
    ),
    'translations.batch-by-tm': t(
      'app_scope_label_translations_batch_by_tm',
      'Batch translate by translation memory'
    ),
    'translations.batch-machine': t(
      'app_scope_label_translations_batch_machine',
      'Batch translate by machine translation'
    ),
    'webhooks.manage': t('app_scope_label_webhooks_manage', 'Manage webhooks'),
    'content-delivery.manage': t(
      'app_scope_label_content_delivery_manage',
      'Manage content delivery'
    ),
    'content-delivery.publish': t(
      'app_scope_label_content_delivery_publish',
      'Publish content delivery'
    ),
    'tasks.view': t('app_scope_label_tasks_view', 'View tasks'),
    'tasks.edit': t('app_scope_label_tasks_edit', 'Edit tasks'),
    'prompts.view': t('app_scope_label_prompts_view', 'View prompts'),
    'prompts.edit': t('app_scope_label_prompts_edit', 'Edit prompts'),
    'translation-labels.manage': t(
      'app_scope_label_translation_labels_manage',
      'Manage labels'
    ),
    'translation-labels.assign': t(
      'app_scope_label_translation_labels_assign',
      'Assign labels'
    ),
    'all.view': t('app_scope_label_all_view', 'View everything'),
    'branch.management': t(
      'app_scope_label_branch_management',
      'Manage branches'
    ),
    'branch.protected-modify': t(
      'app_scope_label_branch_protected_modify',
      'Modify protected branches'
    ),
    'organization-quotas.view': t(
      'app_scope_label_organization_quotas_view',
      'View organization quotas'
    ),
  };

  return {
    getScopeLabel: (scope: string) =>
      labels[scope as PermissionModelScope] ?? scope,
  };
};

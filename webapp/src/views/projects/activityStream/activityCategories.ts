import { components } from 'tg.service/apiSchema.generated';
import { Feature } from 'tg.service/apiSchemaTypes';
import { Scope } from 'tg.fixtures/permissions';

type ProjectModel = components['schemas']['ProjectModel'];

export const ACTIVITY_ACTIONS = {
  KEYS: {
    KEY_CREATED: ['CREATE_KEY'],
    KEY_EDITED: [
      'COMPLEX_EDIT',
      'KEY_NAME_EDIT',
      'KEY_TAGS_EDIT',
      'KEY_CHARACTER_LIMIT_EDIT',
      'NAMESPACE_EDIT',
      'BATCH_TAG_KEYS',
      'BATCH_UNTAG_KEYS',
      'BATCH_SET_KEYS_NAMESPACE',
      'COMPLEX_TAG_OPERATION',
    ],
    KEY_DELETED: [
      'KEY_SOFT_DELETE',
      'KEY_HARD_DELETE',
      'BATCH_KEY_HARD_DELETE',
    ],
    KEY_RESTORED: ['KEY_RESTORE', 'BATCH_KEY_RESTORE'],
  },
  TRANSLATIONS: {
    TRANSLATION_EDITED: ['SET_TRANSLATIONS'],
    TRANSLATION_CLEARED: ['BATCH_CLEAR_TRANSLATIONS'],
    TRANSLATION_COPIED: ['BATCH_COPY_TRANSLATIONS'],
    TRANSLATION_OUTDATED: ['SET_OUTDATED_FLAG'],
  },
  REVIEWS: {
    STATE_CHANGED: ['SET_TRANSLATION_STATE', 'BATCH_SET_TRANSLATION_STATE'],
  },
  MACHINE_TRANSLATION: {
    MACHINE_TRANSLATED: ['AUTO_TRANSLATE', 'BATCH_MACHINE_TRANSLATE'],
    TRANSLATED_FROM_TM: ['BATCH_PRE_TRANSLATE_BY_TM'],
    AUTO_TRANSLATION_DISMISSED: ['DISMISS_AUTO_TRANSLATED_STATE'],
  },
  IMPORTS: {
    IMPORTED: ['IMPORT'],
  },
  COMMENTS: {
    COMMENT_ADDED: ['TRANSLATION_COMMENT_ADD'],
    COMMENT_MENTION: ['COMMENT_MENTION'],
    COMMENT_EDITED: ['TRANSLATION_COMMENT_EDIT'],
    COMMENT_RESOLVED: ['TRANSLATION_COMMENT_SET_STATE'],
    COMMENT_DELETED: ['TRANSLATION_COMMENT_DELETE'],
  },
  SUGGESTIONS: {
    SUGGESTION_ADDED: ['CREATE_SUGGESTION'],
    SUGGESTION_ACCEPTED: ['ACCEPT_SUGGESTION'],
    SUGGESTION_DECLINED: ['DECLINE_SUGGESTION'],
    SUGGESTION_REACTIVATED: ['SUGGESTION_SET_ACTIVE'],
    SUGGESTION_DELETED: ['DELETE_SUGGESTION'],
  },
  SCREENSHOTS: {
    SCREENSHOT_ADDED: ['SCREENSHOT_ADD'],
    SCREENSHOT_REMOVED: ['SCREENSHOT_DELETE'],
  },
  LABELS: {
    LABEL_ASSIGNED: [
      'TRANSLATION_LABEL_ASSIGN',
      'BATCH_ASSIGN_TRANSLATION_LABEL',
      'BATCH_UNASSIGN_TRANSLATION_LABEL',
    ],
    LABEL_MANAGED: [
      'TRANSLATION_LABEL_CREATE',
      'TRANSLATION_LABEL_UPDATE',
      'TRANSLATION_LABEL_DELETE',
    ],
  },
  TASKS: {
    TASK_CREATED: ['TASK_CREATE', 'TASKS_CREATE'],
    TASK_UPDATED: ['TASK_UPDATE', 'TASK_KEYS_UPDATE'],
    TASK_FINISHED: ['TASK_FINISH'],
    TASK_CLOSED: ['TASK_CLOSE'],
    TASK_REOPENED: ['TASK_REOPEN'],
  },
  LANGUAGES: {
    LANGUAGE_ADDED: ['CREATE_LANGUAGE'],
    LANGUAGE_EDITED: ['EDIT_LANGUAGE'],
    LANGUAGE_DELETED: ['DELETE_LANGUAGE'],
  },
  TRANSLATION_MEMORY: {
    TM_PROJECT_SETTINGS: [
      'TRANSLATION_MEMORY_ASSIGN_PROJECT',
      'TRANSLATION_MEMORY_UNASSIGN_PROJECT',
      'TRANSLATION_MEMORY_UPDATE_PROJECT_CONFIG',
    ],
  },
  PROJECT: {
    PROJECT_SETTINGS: ['CREATE_PROJECT', 'EDIT_PROJECT'],
    WEBHOOKS: [
      'WEBHOOK_CONFIG_CREATE',
      'WEBHOOK_CONFIG_UPDATE',
      'WEBHOOK_CONFIG_DELETE',
    ],
    CONTENT_DELIVERY: [
      'CONTENT_DELIVERY_CONFIG_CREATE',
      'CONTENT_DELIVERY_CONFIG_UPDATE',
      'CONTENT_DELIVERY_CONFIG_DELETE',
      'CONTENT_STORAGE_CREATE',
      'CONTENT_STORAGE_UPDATE',
      'CONTENT_STORAGE_DELETE',
    ],
    AI_PROMPTS: ['AI_PROMPT_CREATE', 'AI_PROMPT_UPDATE', 'AI_PROMPT_DELETE'],
    BRANCHES: [
      'BRANCH_CREATE',
      'BRANCH_RENAME',
      'BRANCH_DELETE',
      'BRANCH_PROTECTION_CHANGE',
      'BRANCH_MERGE',
    ],
    QA_ISSUES: ['QA_ISSUE_IGNORE', 'QA_ISSUE_UNIGNORE'],
  },
} as const;

export type ActivityCategory = keyof typeof ACTIVITY_ACTIONS;

type ActionsOf<C extends ActivityCategory> = keyof (typeof ACTIVITY_ACTIONS)[C];
export type ActivityAction = {
  [C in ActivityCategory]: ActionsOf<C>;
}[ActivityCategory];

export const ACTIVITY_CATEGORIES = Object.fromEntries(
  Object.entries(ACTIVITY_ACTIONS).map(([c, actions]) => [
    c,
    Object.values(actions as Record<string, readonly string[]>).flat(),
  ])
) as unknown as Record<ActivityCategory, readonly string[]>;

export type ActionRequirement = {
  feature?: Feature;
  permission?: Scope;
  project?: (project: ProjectModel) => boolean;
};

export const ACTION_REQUIREMENTS: Partial<
  Record<ActivityAction, ActionRequirement>
> = {
  SUGGESTION_ADDED: { project: (p) => p.suggestionsMode !== 'DISABLED' },
  SUGGESTION_ACCEPTED: { project: (p) => p.suggestionsMode !== 'DISABLED' },
  SUGGESTION_DECLINED: { project: (p) => p.suggestionsMode !== 'DISABLED' },
  SUGGESTION_REACTIVATED: { project: (p) => p.suggestionsMode !== 'DISABLED' },
  SUGGESTION_DELETED: { project: (p) => p.suggestionsMode !== 'DISABLED' },
  SCREENSHOT_ADDED: { permission: 'screenshots.view' },
  SCREENSHOT_REMOVED: { permission: 'screenshots.view' },
  LABEL_ASSIGNED: { feature: 'TRANSLATION_LABELS' },
  LABEL_MANAGED: { feature: 'TRANSLATION_LABELS' },
  TASK_CREATED: { feature: 'TASKS', permission: 'tasks.view' },
  TASK_UPDATED: { feature: 'TASKS', permission: 'tasks.view' },
  TASK_FINISHED: { feature: 'TASKS', permission: 'tasks.view' },
  TASK_CLOSED: { feature: 'TASKS', permission: 'tasks.view' },
  TASK_REOPENED: { feature: 'TASKS', permission: 'tasks.view' },
  TM_PROJECT_SETTINGS: {
    feature: 'TRANSLATION_MEMORY',
    permission: 'project.edit',
  },
  PROJECT_SETTINGS: { permission: 'project.edit' },
  WEBHOOKS: { feature: 'WEBHOOKS', permission: 'webhooks.manage' },
  CONTENT_DELIVERY: { permission: 'content-delivery.manage' },
  AI_PROMPTS: {
    feature: 'AI_PROMPT_CUSTOMIZATION',
    permission: 'prompts.view',
  },
  BRANCHES: { feature: 'BRANCHING', project: (p) => p.useBranching },
  QA_ISSUES: { feature: 'QA_CHECKS', project: (p) => p.useQaChecks },
};

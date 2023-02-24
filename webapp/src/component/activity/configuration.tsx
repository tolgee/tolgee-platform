import { getDiffVersion } from './activityTools';
import {
  EntityOptions,
  Reference,
  ActivityOptions,
  EntityEnum,
  KeyReferenceData,
  ActivityTypeEnum,
} from './types';

const getKeyWithLanguages = (relations: any): KeyReferenceData | undefined => {
  if (relations?.key?.data?.name) {
    const language = relations.language?.data as any;
    const key = relations.key;
    const namespace = key.relations?.namespace?.data?.name as string;
    return {
      type: 'key',
      keyName: key.data.name as unknown as string,
      namespace,
      exists: key.exists,
      id: key.entityId,
      languages: language ? [language] : [],
    };
  }
  return undefined;
};

export const entitiesConfiguration: Record<EntityEnum, EntityOptions> = {
  Translation: {
    label: 'activity_entity_translation',
    description: ['key'],
    fields: {
      text: { type: 'text' },
      state: { type: 'translation_state' },
      autoTranslation: {
        type: 'translation_auto',
        compute: ({ auto, mtProvider }) => mtProvider || auto || undefined,
      },
      outdated: { type: 'outdated' },
    },
    references: ({ relations }) => {
      const result: Reference[] = [];
      const keyRef = getKeyWithLanguages(relations);
      if (keyRef) {
        result.push(keyRef);
      }
      return result;
    },
  },
  Language: {
    label: 'activity_entity_language',
    fields: {
      tag: { label: 'activity_entity_translation.tag' },
      name: { label: 'activity_entity_translation.name' },
      flagEmoji: {
        label: 'activity_entity_translation.flag_emoji',
        type: 'language_flag',
      },
    },
    references: ({ modifications, description }) => {
      const result: Reference[] = [];
      const newData = getDiffVersion('new', modifications);
      const data = {
        ...(Object.values(newData).some(Boolean)
          ? newData
          : getDiffVersion('old', modifications)),
        ...description,
      };
      if (data.tag && data.name) {
        result.push({
          type: 'language',
          language: data,
        });
      }

      return result;
    },
  },
  Key: {
    label: 'activity_entity_key',
    fields: {
      name: { label: 'activity_entity_key.name' },
      namespace: { type: 'namespace', label: 'activity_entity_key.namespace' },
    },
    references: ({
      modifications,
      entityId,
      exists,
      description,
      relations,
    }) => {
      const result: Reference[] = [];
      const newData = getDiffVersion('new', modifications);
      const data = newData.name
        ? newData
        : getDiffVersion('old', modifications);
      const keyName = data.name || description?.name;
      const namespace = relations?.namespace?.data?.name;
      if (keyName) {
        result.push({
          type: 'key',
          exists,
          id: entityId,
          keyName: keyName,
          namespace: namespace as unknown as string,
        });
      }
      return result;
    },
  },
  KeyMeta: {
    label: 'activity_entity_key_meta',
    fields: {
      tags: { type: 'key_tags', label: 'activity_entity_key_meta.tags' },
    },
    references: ({ relations }) => {
      const result: Reference[] = [];
      const keyRef = getKeyWithLanguages(relations);
      if (keyRef) {
        result.push(keyRef);
      }
      return result;
    },
  },
  TranslationComment: {
    label: 'activity_entity_translation_comment',
    description: ['comment'],
    fields: {
      text: {
        type: 'text',
      },
      state: {
        type: 'comment_state',
      },
    },
    references: ({ relations, description }) => {
      const result: Reference[] = [];
      // @ts-ignore
      const translationRelations = relations?.translation?.relations;
      const keyRef = getKeyWithLanguages(translationRelations);
      if (keyRef) {
        result.push(keyRef);
      }
      if (description?.text) {
        result.push({ type: 'comment', text: description.text as any });
      }
      return result;
    },
  },
  Screenshot: {
    label: 'activity_entity_screenshot',
    fields: {},
    references: ({ relations }) => {
      const result: Reference[] = [];
      const keyRef = getKeyWithLanguages(relations);
      if (keyRef) {
        result.push(keyRef);
      }
      return result;
    },
  },
  Project: {
    label: 'activity_entity_project',
    fields: {
      name: { label: 'activity_entity_project.name' },
      baseLanguage: {
        type: 'project_language',
        label: 'activity_entity_project.language',
      },
    },
  },
  Namespace: {
    label: 'activity_entity_namespace',
    fields: {
      name: { type: 'namespace', label: 'activity_entity_namespace.name' },
    },
  },
};

export const actionsConfiguration: Partial<
  Record<ActivityTypeEnum, ActivityOptions>
> = {
  CREATE_PROJECT: {
    label: 'activity_create_project',
  },
  SET_TRANSLATIONS: {
    label: 'activity_set_translation',
    entities: { Translation: true },
  },
  DISMISS_AUTO_TRANSLATED_STATE: {
    label: 'activity_dismiss_auto_translated_state',
    entities: { Translation: true },
  },
  SET_TRANSLATION_STATE: {
    label: 'activity_set_translation_state',
    entities: { Translation: true },
  },
  SET_OUTDATED_FLAG: {
    label: 'activity_set_outdated_flag',
    entities: { Translation: true },
  },
  KEY_DELETE: {
    label: 'activity_key_delete',
    entities: { Key: [] },
    titleReferences: [],
  },
  KEY_NAME_EDIT: {
    label: 'activity_key_name_edit',
    entities: { Key: ['name'] },
  },
  CREATE_KEY: {
    label: 'activity_create_key',
    entities: {
      Translation: ['text', 'autoTranslation'],
      KeyMeta: true,
      Key: [],
    },
  },
  COMPLEX_EDIT: {
    label: 'activity_complex_edit',
    entities: {
      Translation: true,
      Key: ['name', 'namespace'],
      Screenshot: true,
    },
  },
  DELETE_LANGUAGE: {
    label: 'activity_delete_language',
    entities: { Language: [] },
  },
  EDIT_LANGUAGE: {
    label: 'activity_edit_language',
    entities: { Language: true },
  },
  CREATE_LANGUAGE: {
    label: 'activity_create_language',
    entities: { Language: [] },
  },
  SCREENSHOT_ADD: {
    label: 'activity_screenshot_add',
    entities: { Screenshot: [] },
  },
  SCREENSHOT_DELETE: {
    label: 'activity_screenshot_delete',
    entities: { Screenshot: [] },
  },
  TRANSLATION_COMMENT_ADD: {
    label: 'activity_translation_comment_add',
    entities: { TranslationComment: ['text'] },
  },
  TRANSLATION_COMMENT_SET_STATE: {
    label: 'activity_translation_comment_set_state',
    entities: { TranslationComment: true },
    titleReferences: ['key'],
  },
  TRANSLATION_COMMENT_DELETE: {
    label: 'activity_translation_comment_delete',
    entities: { TranslationComment: ['text'] },
  },
  IMPORT: {
    label: 'activity_import',
  },
  KEY_TAGS_EDIT: {
    label: 'activity_key_tags_edit',
    entities: { KeyMeta: true },
  },
  TRANSLATION_HISTORY_ADD: {
    label: 'activity_translation_history_add',
  },
  TRANSLATION_HISTORY_MODIFY: {
    label: 'activity_translation_history_modify',
  },
  EDIT_PROJECT: {
    label: 'activity_edit_project',
    entities: { Project: true },
  },
  NAMESPACE_EDIT: {
    label: 'activity_edit_namespace',
    entities: { Namespace: true },
  },
};

import { getDiffVersion } from './activityTools';
import { T } from '@tolgee/react';
import {
  ActivityOptions,
  ActivityTypeEnum,
  EntityEnum,
  EntityOptions,
  KeyReferenceData,
  Reference,
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
    label(params) {
      return <T keyName="activity_entity_translation" params={params} />;
    },
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
    label(params) {
      return <T keyName="activity_entity_language" params={params} />;
    },
    fields: {
      tag: {
        label(params) {
          return (
            <T keyName="activity_entity_translation.tag" params={params} />
          );
        },
      },
      name: {
        label(params) {
          return (
            <T keyName="activity_entity_translation.name" params={params} />
          );
        },
      },
      flagEmoji: {
        label(params) {
          return (
            <T
              keyName="activity_entity_translation.flag_emoji"
              params={params}
            />
          );
        },
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
    label(params) {
      return <T keyName="activity_entity_key" params={params} />;
    },
    fields: {
      name: {
        label(params) {
          return <T keyName="activity_entity_key.name" params={params} />;
        },
      },
      namespace: {
        type: 'namespace',
        label(params) {
          return <T keyName="activity_entity_key.namespace" params={params} />;
        },
      },
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
    label(params) {
      return <T keyName="activity_entity_key_meta" params={params} />;
    },
    fields: {
      tags: {
        type: 'key_tags',
        label(params) {
          return <T keyName="activity_entity_key_meta.tags" params={params} />;
        },
      },
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
    label(params) {
      return (
        <T keyName="activity_entity_translation_comment" params={params} />
      );
    },
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
    label(params) {
      return <T keyName="activity_entity_screenshot" params={params} />;
    },
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
    label(params) {
      return <T keyName="activity_entity_project" params={params} />;
    },
    fields: {
      name: {
        label(params) {
          return <T keyName="activity_entity_project.name" params={params} />;
        },
      },
      baseLanguage: {
        type: 'project_language',
        label(params) {
          return (
            <T keyName="activity_entity_project.language" params={params} />
          );
        },
      },
    },
  },
  Namespace: {
    label(params) {
      return <T keyName="activity_entity_namespace" params={params} />;
    },
    fields: {
      name: {
        type: 'namespace',
        label(params) {
          return <T keyName="activity_entity_namespace.name" params={params} />;
        },
      },
    },
  },
  Params: {
    label() {
      return <T keyName="activity_entity_params" />;
    },
    fields: {
      sourceLanguageId: {
        type: 'batch_language_id',
        label() {
          return <T keyName="activity_entity_params.source_language_id" />;
        },
      },
      targetLanguageIds: {
        type: 'batch_language_ids',
        label() {
          return <T keyName="activity_entity_params.target_language_ids" />;
        },
      },
      languageIds: {
        type: 'batch_language_ids',
        label() {
          return <T keyName="activity_entity_params.target_language_ids" />;
        },
      },
      state: {
        type: 'batch_translation_state',
        label() {
          return <T keyName="activity_entity_params.state" />;
        },
      },
      tags: {
        type: 'batch_key_tag_list',
        label() {
          return <T keyName="activity_entity_params.tags" />;
        },
      },
      namespace: {
        type: 'batch_namespace',
        label() {
          return <T keyName="activity_entity_params.namespace" />;
        },
      },
    },
  },
};

export const actionsConfiguration: Partial<
  Record<ActivityTypeEnum, ActivityOptions>
> = {
  CREATE_PROJECT: {
    label(params) {
      return <T keyName="activity_create_project" params={params} />;
    },
  },
  SET_TRANSLATIONS: {
    label(params) {
      return <T keyName="activity_set_translation" params={params} />;
    },
    entities: { Translation: true },
  },
  DISMISS_AUTO_TRANSLATED_STATE: {
    label(params) {
      return (
        <T keyName="activity_dismiss_auto_translated_state" params={params} />
      );
    },
    entities: { Translation: true },
  },
  SET_TRANSLATION_STATE: {
    label(params) {
      return <T keyName="activity_set_translation_state" params={params} />;
    },
    entities: { Translation: true },
  },
  SET_OUTDATED_FLAG: {
    label(params) {
      return <T keyName="activity_set_outdated_flag" params={params} />;
    },
    entities: { Translation: true },
  },
  KEY_DELETE: {
    label(params) {
      return <T keyName="activity_key_delete" params={params} />;
    },
    entities: { Key: [] },
    titleReferences: [],
  },
  KEY_NAME_EDIT: {
    label(params) {
      return <T keyName="activity_key_name_edit" params={params} />;
    },
    entities: { Key: ['name'] },
  },
  CREATE_KEY: {
    label(params) {
      return <T keyName="activity_create_key" params={params} />;
    },
    entities: {
      Translation: ['text', 'autoTranslation'],
      KeyMeta: true,
      Key: [],
    },
  },
  COMPLEX_EDIT: {
    label(params) {
      return <T keyName="activity_complex_edit" params={params} />;
    },
    entities: {
      Translation: true,
      Key: ['name', 'namespace'],
      Screenshot: true,
    },
  },
  DELETE_LANGUAGE: {
    label(params) {
      return <T keyName="activity_delete_language" params={params} />;
    },
    entities: { Language: [] },
  },
  EDIT_LANGUAGE: {
    label(params) {
      return <T keyName="activity_edit_language" params={params} />;
    },
    entities: { Language: true },
  },
  CREATE_LANGUAGE: {
    label(params) {
      return <T keyName="activity_create_language" params={params} />;
    },
    entities: { Language: [] },
  },
  SCREENSHOT_ADD: {
    label(params) {
      return <T keyName="activity_screenshot_add" params={params} />;
    },
    entities: { Screenshot: [] },
  },
  SCREENSHOT_DELETE: {
    label(params) {
      return <T keyName="activity_screenshot_delete" params={params} />;
    },
    entities: { Screenshot: [] },
  },
  TRANSLATION_COMMENT_ADD: {
    label(params) {
      return <T keyName="activity_translation_comment_add" params={params} />;
    },
    entities: { TranslationComment: ['text'] },
  },
  TRANSLATION_COMMENT_SET_STATE: {
    label(params) {
      return (
        <T keyName="activity_translation_comment_set_state" params={params} />
      );
    },
    entities: { TranslationComment: true },
    titleReferences: ['key'],
  },
  TRANSLATION_COMMENT_DELETE: {
    label(params) {
      return (
        <T keyName="activity_translation_comment_delete" params={params} />
      );
    },
    entities: { TranslationComment: ['text'] },
  },
  IMPORT: {
    label(params) {
      return <T keyName="activity_import" params={params} />;
    },
  },
  KEY_TAGS_EDIT: {
    label(params) {
      return <T keyName="activity_key_tags_edit" params={params} />;
    },
    entities: { KeyMeta: true },
  },
  TRANSLATION_HISTORY_ADD: {
    label(params) {
      return <T keyName="activity_translation_history_add" params={params} />;
    },
  },
  TRANSLATION_HISTORY_MODIFY: {
    label(params) {
      return (
        <T keyName="activity_translation_history_modify" params={params} />
      );
    },
  },
  EDIT_PROJECT: {
    label(params) {
      return <T keyName="activity_edit_project" params={params} />;
    },
    entities: { Project: true },
  },
  NAMESPACE_EDIT: {
    label(params) {
      return <T keyName="activity_edit_namespace" params={params} />;
    },
    entities: { Namespace: true },
  },
  BATCH_PRE_TRANSLATE_BY_TM: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_pre_translate_by_tm"
          params={params}
        />
      );
    },
    entities: { Params: true },
  },
  BATCH_MACHINE_TRANSLATE: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_machine_translate"
          params={params}
        />
      );
    },
    entities: { Params: true },
  },
  BATCH_SET_TRANSLATION_STATE: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_set_translation_state"
          params={params}
        />
      );
    },
    entities: { Params: true },
    compactFieldCount: 2,
  },
  BATCH_COPY_TRANSLATIONS: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_copy_translations"
          params={params}
        />
      );
    },
    entities: { Params: ['targetLanguageIds'] },
  },
  BATCH_CLEAR_TRANSLATIONS: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_clear_translations"
          params={params}
        />
      );
    },
    entities: { Params: true },
  },
  BATCH_TAG_KEYS: {
    label(params) {
      return <T keyName="activity_batch_operation_tag_keys" params={params} />;
    },
    entities: { Params: true },
  },
  BATCH_UNTAG_KEYS: {
    label(params) {
      return (
        <T keyName="activity_batch_operation_untag_keys" params={params} />
      );
    },
    entities: { Params: true },
  },
  BATCH_SET_KEYS_NAMESPACE: {
    label(params) {
      return (
        <T
          keyName="activity_batch_operation_set_keys_namespace"
          params={params}
        />
      );
    },
    entities: { Params: true },
  },
};

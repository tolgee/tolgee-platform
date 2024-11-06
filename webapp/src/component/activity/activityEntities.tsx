import { getDiffVersion } from './activityTools';
import { T } from '@tolgee/react';
import {
  ContentDeliveryConfigReferenceData,
  EntityEnum,
  EntityOptions,
  KeyReferenceData,
  Reference,
  WebhookConfigReferenceData,
} from './types';

export const activityEntities: Record<EntityEnum, EntityOptions> = {
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
      description: {
        type: 'text',
        label(params) {
          return (
            <T keyName="activity_entity_key_meta.description" params={params} />
          );
        },
      },
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
      defaultNamespace: {
        type: 'default_namespace',
        label(params) {
          return <T keyName="activity_entity_project.default_namespace" />;
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
  ContentDeliveryConfig: {
    label() {
      return <T keyName="activity_entity_content_delivery_config" />;
    },
    references: ({ modifications, description }) => {
      const name = (modifications?.['name']?.new ||
        modifications?.['name']?.old ||
        description?.['name']) as unknown as string | null | undefined;
      return name
        ? ([
            { type: 'content_delivery_config', name: name },
          ] as ContentDeliveryConfigReferenceData[])
        : undefined;
    },
    fields: {
      name: {
        type: 'text',
        label() {
          return <T keyName="activity_entity_content_delivery_config.name" />;
        },
      },
      format: {
        type: 'text',
        label() {
          return <T keyName="activity_entity_content_delivery_config.format" />;
        },
      },

      filterState: {
        type: 'state_array',
        label() {
          return (
            <T keyName="activity_entity_content_delivery_config.filterState" />
          );
        },
      },
      languages: {
        type: 'language_tags',
        label() {
          return (
            <T keyName="activity_entity_content_delivery_config.languageTags" />
          );
        },
      },
      supportArrays: {
        type: 'batch_boolean',
        label() {
          return (
            <T keyName="activity_entity_content_delivery_config.supportArrays" />
          );
        },
      },
    },
  },
  ContentStorage: {
    label() {
      return <T keyName="activity_entity_content_storage" />;
    },
    references: ({ modifications, description }) => {
      const name = (modifications?.['name']?.new ||
        modifications?.['name']?.old ||
        description?.['name']) as unknown as string | null | undefined;
      return name
        ? ([
            { type: 'content_delivery_config', name: name },
          ] as ContentDeliveryConfigReferenceData[])
        : undefined;
    },
    fields: {
      name: {
        type: 'text',
        label() {
          return <T keyName="activity_entity_content_delivery_config.name" />;
        },
      },
    },
  },
  WebhookConfig: {
    label() {
      return <T keyName="activity_entity_webhook_config" />;
    },
    references: ({ modifications, description }) => {
      const name = (modifications?.['url']?.new ||
        modifications?.['url']?.old ||
        description?.['url']) as unknown as string | null | undefined;
      return name
        ? ([
            { type: 'webhook_config', url: name },
          ] as WebhookConfigReferenceData[])
        : undefined;
    },
    fields: {
      url: {
        type: 'text',
        label() {
          return <T keyName="activity_entity_content_delivery_config.name" />;
        },
      },
    },
  },
  Task: {
    label() {
      return <T keyName="activity_entity_task" />;
    },
    fields: {
      name: {
        type: 'text',
        label() {
          return <T keyName="activity_entity_task.name" />;
        },
      },
      type: {
        type: 'task_type',
        label() {
          return <T keyName="activity_entity_task.type" />;
        },
      },
      state: {
        type: 'task_state',
        label() {
          return <T keyName="activity_entity_task.state" />;
        },
      },
      description: {
        type: 'text',
        label() {
          return <T keyName="activity_entity_task.description" />;
        },
      },
      dueDate: {
        type: 'date',
        label() {
          return <T keyName="activity_entity_task.due_date" />;
        },
      },
    },
    references: (props) => {
      const result: Reference[] = [];
      const name = props.description?.name ?? props.modifications?.name?.new;
      const taskType =
        props.description?.type ?? props.modifications?.description?.new;
      const number =
        props.description?.number ?? props.modifications?.number?.new;
      if (name && taskType && number) {
        result.push({
          type: 'task',
          taskType: taskType as any,
          name: name as unknown as string,
          number: Number(number),
        });
      }
      return result;
    },
  },
};

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

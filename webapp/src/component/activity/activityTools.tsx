import { components } from 'tg.service/apiSchema.generated';
import { actionsConfiguration } from './configuration';
import {
  Activity,
  ActivityModel,
  DiffValue,
  Entity,
  EntityEnum,
  EntityOptions,
  Field,
  FieldOptionsObj,
  Reference,
} from './types';
import { activityEntities } from './activityEntities';

type ModifiedEntityModel = components['schemas']['ModifiedEntityModel'];

export const getDiffVersion = (
  version: 'new' | 'old',
  modifications: ModifiedEntityModel['modifications']
) => {
  const result = {};
  Object.entries(modifications || {}).forEach(([key, value]) => {
    result[key] = value[version];
  });
  return result as any;
};

const buildField = (
  value: DiffValue<any>,
  options: FieldOptionsObj,
  name: string,
  languageTag?: string | undefined
): Field => {
  const label = options.label;

  return {
    name,
    value,
    label,
    options,
    languageTag,
  };
};

const getFieldValue = (
  fieldName: string,
  modifications: ModifiedEntityModel['modifications'],
  options: FieldOptionsObj
): DiffValue<any> | undefined => {
  if (!options.compute) {
    return modifications?.[fieldName] as DiffValue<any>;
  }

  const newValues = getDiffVersion('new', modifications);
  const oldValues = getDiffVersion('old', modifications);

  const result = {
    new: options.compute?.(newValues),
    old: options.compute?.(oldValues),
  };

  if (result.new === undefined && result.old === undefined) {
    return undefined;
  }
  return result;
};

const reduceReferences = (allReferences: Reference[]): Reference[] => {
  const unifiedMap = new Map<string, Reference>();
  const otherReferences: Reference[] = [];

  const mergeReference = <T extends Reference>(
    id: any,
    reference: T,
    merge: (existingRef: T, newRef: T) => T
  ) => {
    const identificator = `${reference.type}:${id}`;
    if (unifiedMap.has(identificator)) {
      unifiedMap.set(
        identificator,
        merge(unifiedMap.get(identificator) as T, reference)
      );
    } else {
      unifiedMap.set(identificator, reference);
    }
  };

  allReferences.forEach((reference) => {
    switch (reference.type) {
      case 'key':
        mergeReference(reference.id, reference, (oldRef, newRef) => {
          return {
            ...oldRef,
            languages: [
              ...(oldRef.languages || []),
              ...(newRef.languages || []),
            ],
          };
        });
        break;

      default:
        otherReferences.push(reference);
    }
  });

  return [...unifiedMap.values(), ...otherReferences];
};

function getFieldLanguageTag(
  fieldName: string,
  entityType: EntityEnum,
  entityData: ModifiedEntityModel
): string | undefined {
  if (entityType == 'Translation') {
    return entityData.relations?.['language']?.data?.['tag'] as any as string;
  }
}

export const buildEntity = (
  entityType: EntityEnum,
  entityData: ModifiedEntityModel,
  options: EntityOptions,
  selectedFields: string[]
): Entity => {
  const result: Entity = {
    type: entityType,
    options,
    fields: [],
    references: options.references?.(entityData) || [],
  };

  Object.entries(options.fields)
    .filter(([fieldName]) => selectedFields.includes(fieldName))
    .forEach(([fieldName, o]) => {
      if (!o) {
        return null;
      }
      const optionsObj = (typeof o === 'object' ? o : {}) as FieldOptionsObj;

      const fieldData = getFieldValue(
        fieldName,
        entityData.modifications,
        optionsObj
      );

      const languageTag = getFieldLanguageTag(
        fieldName,
        entityType,
        entityData
      );

      if (fieldData) {
        result.fields.push(
          buildField(fieldData, optionsObj, fieldName, languageTag)
        );
      }
    });

  return result;
};

export const buildActivity = (
  data: ActivityModel,
  filter = false
): Activity => {
  const options = actionsConfiguration[data.type];

  const result: Activity = {
    translation: options?.label,
    type: data.type,
    entities: [],
    references: [],
    counts: data.counts || {},
    options: options!,
  };

  let allReferences: Reference[] = [];

  // add params as "entity" with only `new` modifications
  if (data.params && (!filter || options?.entities?.Params)) {
    const entityOptions = activityEntities.Params;
    const selectedFields =
      filter && Array.isArray(options?.entities?.Params)
        ? (options?.entities?.Params as string[])
        : Object.keys(entityOptions?.fields || {});

    const modifications: ModifiedEntityModel['modifications'] = {};
    Object.entries(data.params).map(([key, value]) => {
      // @ts-ignore
      modifications[key] = { new: value };
    });

    result.entities.push(
      buildEntity(
        'Params',
        { entityId: 0, modifications, entityClass: 'Params' },
        entityOptions,
        selectedFields
      )
    );
  }

  if (data.modifiedEntities) {
    const entities = filter
      ? Object.keys(options?.entities || {})
      : Object.keys(data.modifiedEntities);

    entities.forEach((entityName) => {
      if (filter && !options?.entities?.[entityName]) {
        return;
      }
      const entityOptions = activityEntities[entityName] as
        | EntityOptions
        | undefined;

      const selectedFields =
        filter && Array.isArray(options?.entities?.[entityName])
          ? (options?.entities?.[entityName] as string[])
          : Object.keys(entityOptions?.fields || {});

      const values = data.modifiedEntities?.[entityName];

      values?.forEach((entityData) => {
        if (entityOptions) {
          const entity = buildEntity(
            entityName as EntityEnum,
            entityData,
            entityOptions,
            selectedFields
          );

          result.entities.push(entity);
          allReferences = allReferences.concat(entity.references);
        }
      });
    });

    result.references = reduceReferences(allReferences);
  }

  return result;
};

import { EntityOptions, ActionType, ActionOptions } from './types';

export const entitiesConfiguration: Record<string, EntityOptions> = {
  Translation: {
    fields: {
      text: true,
    },
  },
  Language: {
    fields: {
      tag: true,
      name: true,
      flagEmoji: true,
    },
  },
};

export const actionsConfiguration: Partial<Record<ActionType, ActionOptions>> =
  {
    SET_TRANSLATIONS: {
      label: 'Set translation',
      labelDescription: ({ modifiedEntities }) => {
        const relations = modifiedEntities?.Translation[0].relations;
        return `${relations?.key.data.name} ${relations?.language.data.flagEmoji}`;
      },
      entities: ['Translation'],
    },
    DELETE_LANGUAGE: {
      label: 'Deleted language',
      labelDescription: (data) => {
        const modifications = data.modifiedEntities?.Language[0].modifications;
        return `${modifications?.name.old} ${modifications?.flagEmoji.old}`;
      },
    },
    KEY_DELETE: {
      label: 'Deleted keys',
      labelDescription: ({ modifiedEntities }) =>
        `${modifiedEntities?.Key.map(
          ({ modifications }) => modifications?.name.old
        ).join(', ')}`,
    },
    CREATE_LANGUAGE: {
      label: 'Created language',
      labelDescription: (data) => {
        const modifications = data.modifiedEntities?.Language[0].modifications;
        return `${modifications?.name.new} ${modifications?.flagEmoji.new}`;
      },
    },
  };

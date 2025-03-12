import { operations, components } from 'tg.service/apiSchema.generated';

export type FiltersType = operations['getTranslations']['parameters']['query'];

export type LanguageModel = components['schemas']['LanguageModel'];

export const isFilterEmpty = (filter: FiltersType) => {
  return Object.values(filter).filter(Boolean).length === 0;
};

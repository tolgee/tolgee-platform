import { components } from 'tg.service/apiSchema.generated';

export type DiffInput<T = string> = {
  old?: T;
  new?: T;
};

export type TranslationHistoryModel =
  components['schemas']['TranslationHistoryModel'];

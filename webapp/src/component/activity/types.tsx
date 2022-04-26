import { components } from 'tg.service/apiSchema.generated';

type ModifiedEntityModel = components['schemas']['ModifiedEntityModel'];
export type ActionType = components['schemas']['ProjectActivityModel']['type'];
export type ProjectActivityModelType =
  components['schemas']['ProjectActivityModel'];

export type DiffValue<T> = {
  old?: T;
  new?: T;
};

export type FieldOptionsObj = {
  label?: boolean | string;
};

export type FieldOptions = boolean | FieldOptionsObj;

export type EntityOptions = {
  description?: (data: ModifiedEntityModel) => string;
  fields: Record<string, FieldOptions>;
};

export type ActionOptions = {
  label: string;
  labelDescription?: (data: ProjectActivityModelType) => string;
  description?: (data: ProjectActivityModelType) => string;
  entities?: string[];
};

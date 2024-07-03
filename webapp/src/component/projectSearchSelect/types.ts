import { components } from 'tg.service/apiSchema.generated';

export type Avatar = components['schemas']['Avatar'];

export type Project = {
  id: number;
  name?: string;
  avatar?: Avatar;
};

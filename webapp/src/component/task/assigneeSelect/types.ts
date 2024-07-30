import { components } from 'tg.service/apiSchema.generated';

export type Avatar = components['schemas']['Avatar'];

export type User = {
  id: number;
  username: string;
  name?: string;
  avatar?: Avatar;
};

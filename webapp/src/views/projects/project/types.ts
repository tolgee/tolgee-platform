import { CreateProjectRequest } from 'tg.service/apiSchemaTypes.generated';

export type CreateProjectFormValues = Omit<
  CreateProjectRequest,
  'organizationId'
>;

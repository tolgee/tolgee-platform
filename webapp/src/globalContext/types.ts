import { PrivateOrganizationModel } from 'tg.service/apiSchemaTypes.generated';

export type ContextOrganizationModel = Omit<
  PrivateOrganizationModel,
  'quickStart'
>;

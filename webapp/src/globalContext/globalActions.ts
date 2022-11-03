import { components } from 'tg.service/apiSchema.generated';

type OrganizationModel = components['schemas']['OrganizationModel'];
type UsageModel = components['schemas']['UsageModel'];

export type GlobalActionType =
  | {
      type: 'UPDATE_ORGANIZATION';
      payload: number | OrganizationModel;
    }
  | { type: 'REFETCH_INITIAL_DATA' }
  | { type: 'REFETCH_USAGE' }
  | {
      type: 'UPDATE_USAGE';
      payload: Partial<UsageModel>;
    }
  | { type: 'INCREMENT_PLAN_LIMIT_ERRORS' }
  | { type: 'INCREMENT_NO_CREDIT_ERRORS' };

export const globalDispatchRef = {
  current: undefined as ((action: GlobalActionType) => void) | undefined,
};

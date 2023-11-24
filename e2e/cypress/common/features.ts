import { components } from '../../../webapp/src/service/apiSchema.generated';
import { internalFetch } from './apiCalls/common';

type Feature =
  components['schemas']['SelfHostedEePlanModel']['enabledFeatures'][number];

export async function setFeature(feature: Feature, enabled: boolean) {
  internalFetch(`features/toggle?feature=${feature}&enabled=${enabled}`, {
    method: 'put',
  });
}

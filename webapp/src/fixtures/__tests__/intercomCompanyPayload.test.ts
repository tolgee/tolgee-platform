import { PrivateOrganizationModel } from 'tg.service/apiSchemaTypes.generated';
import { intercomCompanyPayload } from 'tg.fixtures/intercomCompanyPayload';
import { organization } from 'tg.fixtures/__tests__/organizationTestData';

type CloudSubscription = NonNullable<
  PrivateOrganizationModel['activeCloudSubscription']
>;

const subscription = (): CloudSubscription =>
  ({
    plan: { name: 'Premium' },
    status: 'ACTIVE',
  } as CloudSubscription);

describe('intercomCompanyPayload', () => {
  it('reports nothing for an organization the viewer only reaches through public projects', () => {
    expect(
      intercomCompanyPayload(
        organization({
          limitedView: true,
          currentUserRole: undefined,
          activeCloudSubscription: subscription(),
        })
      )
    ).toBeUndefined();
  });

  it('maps an organization that has an active cloud subscription', () => {
    expect(
      intercomCompanyPayload(
        organization({
          id: 42,
          name: 'Acme',
          enabledFeatures: ['STANDARD_SUPPORT', 'TASKS'],
          activeCloudSubscription: subscription(),
        })
      )
    ).toEqual({
      company_id: 42,
      name: 'Acme',
      plan: 'Premium',
      subscriptionStatus: 'ACTIVE',
      enabledFeatures: 'STANDARD_SUPPORT, TASKS',
    });
  });

  it('reports no company for an organization without a cloud subscription', () => {
    expect(intercomCompanyPayload(organization({}))).toBeUndefined();
  });

  it('reports no company for a viewer without any organization', () => {
    expect(intercomCompanyPayload(undefined)).toBeUndefined();
  });
});

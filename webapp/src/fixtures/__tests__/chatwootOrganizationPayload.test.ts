import { chatwootOrganizationPayload } from 'tg.fixtures/chatwootOrganizationPayload';
import { organization } from 'tg.fixtures/__tests__/organizationTestData';

const subscription = () =>
  ({
    plan: { name: 'Premium' },
    status: 'ACTIVE',
  } as any);

describe('chatwootOrganizationPayload', () => {
  it('reports the plan a member is on', () => {
    const payload = chatwootOrganizationPayload(
      organization({
        currentUserRole: 'MEMBER',
        activeCloudSubscription: subscription(),
      })
    );

    expect(payload.plan).toBe('Premium');
    expect(payload.subscriptionStatus).toBe('ACTIVE');
    expect(payload.organizationId).toBe(1);
  });

  it('withholds a subscription the viewer is not a member of', () => {
    const payload = chatwootOrganizationPayload(
      organization({
        currentUserRole: undefined,
        activeCloudSubscription: subscription(),
      })
    );

    expect(payload.plan).toBe('free');
    expect(payload.subscriptionStatus).toBe('inactive');
    expect(payload.organizationName).toBe('Org');
  });

  it('passes the viewer role through', () => {
    expect(
      chatwootOrganizationPayload(organization({ currentUserRole: 'OWNER' }))
        .currentUserRole
    ).toBe('OWNER');
  });
});

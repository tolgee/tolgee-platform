import { components } from 'tg.service/apiSchema.generated';
import {
  organizationCompanyInfo,
  organizationHasSupportChat,
} from '../organizationEntitlement';

type PrivateOrganization = components['schemas']['PrivateOrganizationModel'];
type CloudSubscription = NonNullable<
  PrivateOrganization['activeCloudSubscription']
>;

const organization = (
  data: Partial<PrivateOrganization>
): PrivateOrganization =>
  ({
    id: 1,
    name: 'Org',
    slug: 'org',
    basePermissions: {},
    enabledFeatures: [],
    limitedView: false,
    ...data,
  } as PrivateOrganization);

const subscription = (): CloudSubscription =>
  ({
    plan: { name: 'Premium' },
    status: 'ACTIVE',
  } as CloudSubscription);

describe('organizationHasSupportChat', () => {
  it('accepts either support tier', () => {
    expect(
      organizationHasSupportChat(
        organization({ enabledFeatures: ['STANDARD_SUPPORT'] })
      )
    ).toBe(true);
    expect(
      organizationHasSupportChat(
        organization({ enabledFeatures: ['PREMIUM_SUPPORT'] })
      )
    ).toBe(true);
  });

  it('rejects an organization with neither support tier', () => {
    expect(organizationHasSupportChat(organization({}))).toBe(false);
  });

  it('rejects both support tiers on a limited-view organization', () => {
    expect(
      organizationHasSupportChat(
        organization({
          limitedView: true,
          enabledFeatures: ['STANDARD_SUPPORT'],
        })
      )
    ).toBe(false);
    expect(
      organizationHasSupportChat(
        organization({
          limitedView: true,
          enabledFeatures: ['PREMIUM_SUPPORT'],
        })
      )
    ).toBe(false);
  });

  it('rejects a viewer without any organization', () => {
    expect(organizationHasSupportChat(undefined)).toBe(false);
  });
});

describe('organizationCompanyInfo', () => {
  it('maps an organization that has an active cloud subscription', () => {
    expect(
      organizationCompanyInfo(
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

  it('returns null for an organization without a cloud subscription', () => {
    expect(organizationCompanyInfo(organization({}))).toBeNull();
  });

  it('returns null for a viewer without any organization', () => {
    expect(organizationCompanyInfo(undefined)).toBeNull();
  });
});

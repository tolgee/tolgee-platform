import { billingRefusal, canSeeBilling } from 'tg.fixtures/billingAccess';
import { organization } from 'tg.fixtures/__tests__/organizationTestData';

const params = (over: Partial<Parameters<typeof canSeeBilling>[0]> = {}) => ({
  billingEnabled: true,
  isAdminOrSupporter: false,
  organization: organization({ currentUserRole: 'OWNER' }),
  ...over,
});

describe('canSeeBilling', () => {
  it('refuses everyone when billing is disabled', () => {
    expect(canSeeBilling(params({ billingEnabled: false }))).toBe(false);
    expect(
      canSeeBilling(params({ billingEnabled: false, isAdminOrSupporter: true }))
    ).toBe(false);
  });

  it('accepts the owner of the preferred organization', () => {
    expect(canSeeBilling(params())).toBe(true);
  });

  it('refuses members and maintainers', () => {
    expect(
      canSeeBilling(
        params({ organization: organization({ currentUserRole: 'MEMBER' }) })
      )
    ).toBe(false);
    expect(
      canSeeBilling(
        params({
          organization: organization({ currentUserRole: 'MAINTAINER' }),
        })
      )
    ).toBe(false);
  });

  it('refuses a viewer of an adopted organization they hold no role in', () => {
    expect(
      canSeeBilling(
        params({
          organization: organization({
            limitedView: true,
            currentUserRole: undefined,
          }),
        })
      )
    ).toBe(false);
  });

  it('refuses an admin who has no organization, since there is no billing to open', () => {
    expect(
      canSeeBilling(
        params({ isAdminOrSupporter: true, organization: undefined })
      )
    ).toBe(false);
  });

  it('accepts admins and supporters regardless of role', () => {
    expect(
      canSeeBilling(
        params({
          isAdminOrSupporter: true,
          organization: organization({ currentUserRole: undefined }),
        })
      )
    ).toBe(true);
  });

  it('refuses when there is no organization at all', () => {
    expect(canSeeBilling(params({ organization: undefined }))).toBe(false);
  });
});

describe('billingRefusal', () => {
  const params = (over = {}) => ({
    billingEnabled: true,
    isAdminOrSupporter: false,
    organization: organization({ currentUserRole: 'OWNER' }),
    ...over,
  });

  it('names the server setting before anything about the viewer', () => {
    expect(
      billingRefusal(params({ billingEnabled: false, organization: undefined }))
    ).toBe('billing-disabled');
  });

  it('names the missing organization, so every denial can explain itself', () => {
    expect(billingRefusal(params({ organization: undefined }))).toBe(
      'no-organization'
    );
    expect(canSeeBilling(params({ organization: undefined }))).toBe(false);
  });

  it('names the role when the viewer has an organization they do not own', () => {
    expect(
      billingRefusal(
        params({ organization: organization({ currentUserRole: 'MEMBER' }) })
      )
    ).toBe('billing-not-an-owner');
  });

  it('refuses nothing to an owner or an admin', () => {
    expect(billingRefusal(params())).toBeUndefined();
    expect(
      billingRefusal(
        params({
          isAdminOrSupporter: true,
          organization: organization({ currentUserRole: undefined }),
        })
      )
    ).toBeUndefined();
  });
});

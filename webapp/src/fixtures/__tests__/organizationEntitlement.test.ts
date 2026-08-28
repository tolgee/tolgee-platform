import {
  organizationOwnedFeatures,
  memberIsEntitledTo,
} from 'tg.fixtures/organizationEntitlement';
import { organization } from 'tg.fixtures/__tests__/organizationTestData';

describe('organizationOwnedFeatures vs memberIsEntitledTo', () => {
  it('reports whatever features the payload carries, whatever the viewer role', () => {
    const roleless = organization({
      currentUserRole: undefined,
      enabledFeatures: ['GLOSSARY'],
    });

    expect(organizationOwnedFeatures(roleless)).toContain('GLOSSARY');
    expect(memberIsEntitledTo(roleless, 'GLOSSARY')).toBe(false);
  });

  it("carries a limited-view organization's own features, but entitles the viewer to none", () => {
    const limitedView = organization({
      limitedView: true,
      currentUserRole: undefined,
      enabledFeatures: ['GLOSSARY'],
    });

    // A project inherits its organization's features whoever is browsing it, so the array is not
    // redacted; membership is what `memberIsEntitledTo` answers, and there is none.
    expect(organizationOwnedFeatures(limitedView)).toEqual(['GLOSSARY']);
    expect(memberIsEntitledTo(limitedView, 'GLOSSARY')).toBe(false);
  });

  it('reports both for an organization the viewer belongs to', () => {
    const own = organization({
      enabledFeatures: ['GLOSSARY'],
      currentUserRole: 'MEMBER',
    });

    expect(organizationOwnedFeatures(own)).toContain('GLOSSARY');
    expect(memberIsEntitledTo(own, 'GLOSSARY')).toBe(true);
  });

  it('refuses the viewer entitlement to an admin reading a customer organization unlimited', () => {
    const customer = organization({
      enabledFeatures: ['GLOSSARY'],
      limitedView: false,
      currentUserRole: undefined,
    });

    expect(organizationOwnedFeatures(customer)).toContain('GLOSSARY');
    expect(memberIsEntitledTo(customer, 'GLOSSARY')).toBe(false);
  });

  it('reports nothing without an organization', () => {
    expect(organizationOwnedFeatures(undefined)).toEqual([]);
    expect(memberIsEntitledTo(undefined, 'GLOSSARY')).toBe(false);
  });
});

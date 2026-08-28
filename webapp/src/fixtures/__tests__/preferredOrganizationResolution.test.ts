import { preferredOrganizationResolution } from 'tg.fixtures/preferredOrganizationResolution';
import { organization } from 'tg.fixtures/__tests__/organizationTestData';

describe('preferredOrganizationResolution', () => {
  it('resolves to the organization the viewer already has', () => {
    const preferred = organization({ id: 7 });

    expect(
      preferredOrganizationResolution({
        organization: preferred,
        isSwitching: false,
      })
    ).toEqual({ status: 'resolved', organization: preferred });
  });

  it('holds while a switch is still in flight, rather than reporting none', () => {
    expect(
      preferredOrganizationResolution({
        organization: undefined,
        isSwitching: true,
      })
    ).toEqual({ status: 'resolving' });
  });

  it('reports none once nothing is in flight to produce one', () => {
    expect(
      preferredOrganizationResolution({
        organization: undefined,
        isSwitching: false,
      })
    ).toEqual({ status: 'missing' });
  });
});

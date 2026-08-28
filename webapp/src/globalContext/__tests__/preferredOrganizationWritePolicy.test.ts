import { decidePreferredOrganizationWrite } from 'tg.globalContext/preferredOrganizationWritePolicy';

const decide = (
  over: Partial<Parameters<typeof decidePreferredOrganizationWrite>[0]>
) =>
  decidePreferredOrganizationWrite({
    organizationId: 1,
    inFlightOrganizationId: undefined,
    preferredOrganizationId: undefined,
    hasUnsettledWrite: false,
    ...over,
  });

describe('decidePreferredOrganizationWrite', () => {
  it('reuses the in-flight write when the same organization is asked for again', () => {
    expect(decide({ inFlightOrganizationId: 1 })).toBe('await-in-flight');
  });

  it('issues a write when a different organization is asked for mid-flight', () => {
    expect(decide({ organizationId: 2, inFlightOrganizationId: 1 })).toBe(
      'issue-write'
    );
  });

  it('does nothing when the organization is already the preferred one', () => {
    expect(decide({ preferredOrganizationId: 1 })).toBe('already-preferred');
  });

  it('still issues a write for the preferred organization while a switch away is in flight', () => {
    expect(
      decide({ preferredOrganizationId: 1, inFlightOrganizationId: 2 })
    ).toBe('issue-write');
  });

  it('issues a write for a viewer with no preferred organization', () => {
    expect(decide({})).toBe('issue-write');
  });

  it('still writes for the already-preferred organization while an abandoned write is live', () => {
    expect(
      decide({
        organizationId: 1,
        preferredOrganizationId: 1,
        hasUnsettledWrite: true,
      })
    ).toBe('issue-write');
  });
});

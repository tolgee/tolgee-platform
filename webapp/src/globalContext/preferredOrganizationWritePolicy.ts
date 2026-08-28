type Params = {
  organizationId: number;
  inFlightOrganizationId: number | undefined;
  preferredOrganizationId: number | undefined;
  hasUnsettledWrite: boolean;
};

type WriteDecision = 'await-in-flight' | 'already-preferred' | 'issue-write';

/** `hasUnsettledWrite` is load-bearing: see 'does not report the previous organization as already-preferred while an abandoned write can still land'. */
export const decidePreferredOrganizationWrite = ({
  organizationId,
  inFlightOrganizationId,
  preferredOrganizationId,
  hasUnsettledWrite,
}: Params): WriteDecision => {
  if (inFlightOrganizationId === organizationId) {
    return 'await-in-flight';
  }

  if (
    inFlightOrganizationId === undefined &&
    !hasUnsettledWrite &&
    organizationId === preferredOrganizationId
  ) {
    return 'already-preferred';
  }

  return 'issue-write';
};

export const ownsWriteRequest = (request: number, newestRequest: number) =>
  request === newestRequest;

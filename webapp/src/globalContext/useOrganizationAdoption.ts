import { useEffect, useRef, useState } from 'react';

import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { ownsWriteRequest } from 'tg.globalContext/preferredOrganizationWritePolicy';

export const useOrganizationAdoption = (organizationId: number | undefined) => {
  const { preferredOrganization, updatePreferredOrganization } =
    usePreferredOrganization();
  const [settledFor, setSettledFor] = useState<number | undefined>(undefined);
  const [trackedOrganizationId, setTrackedOrganizationId] = useState<
    number | undefined
  >(organizationId);
  const requestedAdoptionRef = useRef(0);

  const [seenActiveId, setSeenActiveId] = useState(preferredOrganization?.id);

  if (organizationId !== trackedOrganizationId) {
    setTrackedOrganizationId(organizationId);
    setSettledFor(undefined);
  }

  // Re-arm when the active organization changes underneath us — the switcher on this very page can
  // do that. Keyed on a CHANGE, not on a mismatch: a refused adoption leaves the two different
  // forever, and re-arming on that would write in a loop.
  if (preferredOrganization?.id !== seenActiveId) {
    setSeenActiveId(preferredOrganization?.id);
    if (
      organizationId !== undefined &&
      preferredOrganization?.id !== organizationId
    ) {
      setSettledFor(undefined);
    }
  }

  useEffect(() => {
    if (organizationId === undefined) {
      return;
    }
    const adoption = ++requestedAdoptionRef.current;

    // finally, not then: both consumers block on this marker with no timeout of their own, so a
    // rejection here would leave the page loading forever rather than degraded.
    updatePreferredOrganization(organizationId).finally(() => {
      if (ownsWriteRequest(adoption, requestedAdoptionRef.current)) {
        setSettledFor(organizationId);
      }
    });
  }, [organizationId]);

  const awaitingAdoption =
    organizationId !== undefined && settledFor !== organizationId;

  return {
    // The project page renders during an in-flight adoption on purpose, so it waits only when there
    // is no organization at all; the organization pages wait for any cross-organization arrival,
    // where the chrome would otherwise show the previously active organization.
    awaitingFirstOrganization: awaitingAdoption && !preferredOrganization,
    awaitingOrganization:
      awaitingAdoption && preferredOrganization?.id !== organizationId,
  };
};

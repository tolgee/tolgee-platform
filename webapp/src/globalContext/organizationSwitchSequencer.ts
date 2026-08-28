import {
  decidePreferredOrganizationWrite,
  ownsWriteRequest,
} from 'tg.globalContext/preferredOrganizationWritePolicy';

export type SwitchHandlers<T> = {
  write: (organizationId: number) => Promise<T>;
  apply: (written: T) => void;
  onRequested: (request: number) => void;
  onSettled: (request: number) => void;
};

type InFlight = {
  organizationId: number;
  request: number;
  promise: Promise<boolean>;
};

export type OrganizationSwitchTo = (
  organizationId: number,
  preferredOrganizationId: number | undefined
) => Promise<boolean>;

export const ORGANIZATION_SWITCH_DEADLINE_MS = 8000;

export const createOrganizationSwitchSequencer = <T>({
  write,
  apply,
  onRequested,
  onSettled,
}: SwitchHandlers<T>) => {
  let requested = 0;
  let chain: Promise<unknown> = Promise.resolve();
  let inFlight: InFlight | undefined = undefined;
  let unsettledWrites = 0;

  const switchTo = (
    organizationId: number,
    preferredOrganizationId: number | undefined
  ): Promise<boolean> => {
    const decision = decidePreferredOrganizationWrite({
      organizationId,
      inFlightOrganizationId: inFlight?.organizationId,
      preferredOrganizationId,
      hasUnsettledWrite: unsettledWrites > 0,
    });

    if (decision === 'await-in-flight' && inFlight) {
      return inFlight.promise;
    }

    if (decision === 'already-preferred') {
      return Promise.resolve(true);
    }

    const request = ++requested;
    onRequested(request);

    // Chained on the bounded promise, and the deadline is armed inside it: see 'still issues a later
    // switch after an earlier write blew its deadline' and 'gives a queued switch its whole budget'.
    const promise = chain.then(() =>
      withDeadline(request, () => run(organizationId, request))
    );
    chain = promise;
    inFlight = { organizationId, request, promise };
    return promise;
  };

  const run = async (
    organizationId: number,
    request: number
  ): Promise<boolean> => {
    if (!owns(request)) {
      onSettled(request);
      return false;
    }
    unsettledWrites += 1;
    try {
      const written = await write(organizationId);
      if (!owns(request)) {
        return false;
      }
      apply(written);
      return true;
    } catch {
      return false;
    } finally {
      unsettledWrites -= 1;
      onSettled(request);
    }
  };

  const release = (request: number) => {
    if (inFlight?.request === request) {
      inFlight = undefined;
    }
  };

  const withDeadline = (
    request: number,
    issue: () => Promise<boolean>
  ): Promise<boolean> =>
    new Promise((resolve) => {
      const timer = setTimeout(() => {
        release(request);
        onSettled(request);
        resolve(false);
      }, ORGANIZATION_SWITCH_DEADLINE_MS);

      issue().then((switched) => {
        clearTimeout(timer);
        release(request);
        resolve(switched);
      });
    });

  const owns = (request: number) => ownsWriteRequest(request, requested);

  return switchTo;
};

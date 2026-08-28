type SessionState = {
  canOpenSupportChat: boolean;
  installed: boolean;
  booted: boolean;
  identityChanged: boolean;
};

type IntercomSessionAction =
  | 'install'
  | 'boot'
  | 'reboot'
  | 'update'
  | 'shutdown'
  | 'none';

export const intercomSessionAction = ({
  canOpenSupportChat,
  installed,
  booted,
  identityChanged,
}: SessionState): IntercomSessionAction => {
  if (!canOpenSupportChat) {
    return booted ? 'shutdown' : 'none';
  }
  if (!installed) {
    return 'install';
  }
  // Intercom() re-runs the loader on every call; it does not resume a shut-down session.
  if (!booted) {
    return 'boot';
  }
  if (identityChanged) {
    return 'reboot';
  }
  return 'update';
};

export type IntercomTrackers = {
  installed: boolean;
  booted: boolean;
  bootedFor: number | undefined;
};

type IntercomSdk<S> = {
  install: (settings: S) => void;
  boot: (settings: S) => void;
  update: (settings: S) => void;
  shutdown: () => void;
};

type SessionInput<S> = {
  settings: () => S;
  userId: number | undefined;
  companyId: number | undefined;
};

export const applyIntercomSession = <S>(
  action: IntercomSessionAction,
  { settings, userId, companyId }: SessionInput<S>,
  trackers: IntercomTrackers,
  sdk: IntercomSdk<S>
): IntercomTrackers => {
  if (action === 'shutdown') {
    sdk.shutdown();
    return {
      ...trackers,
      booted: false,
      bootedFor: undefined,
    };
  }

  if (action === 'none') {
    return trackers;
  }

  if (action === 'update') {
    sdk.update(settings());
    return { ...trackers, bootedFor: userId };
  }

  if (action === 'reboot') {
    sdk.shutdown();
  }

  const installs = action === 'install';
  if (installs) {
    sdk.install(settings());
  } else {
    sdk.boot(settings());
  }

  return {
    installed: trackers.installed || installs,
    booted: true,
    bootedFor: userId,
  };
};

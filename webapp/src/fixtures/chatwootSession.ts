type SessionState = {
  canOpenSupportChat: boolean;
  identifiedUserId: number | undefined;
  currentUserId: number | undefined;
};

type ChatwootSessionAction = 'set-attributes' | 'reset' | 'none';

export const chatwootSessionAction = ({
  canOpenSupportChat,
  identifiedUserId,
  currentUserId,
}: SessionState): ChatwootSessionAction => {
  if (identifiedUserId === undefined) {
    return 'none';
  }
  if (identifiedUserId !== currentUserId) {
    return 'reset';
  }
  // Not reset: that is Chatwoot's logout and would discard a conversation in progress.
  return canOpenSupportChat ? 'set-attributes' : 'none';
};

type ChatwootSdk<O> = {
  setAttributes: (organization: O) => void;
  reset: () => void;
};

type ChatwootOpenSdk<U, O> = ChatwootSdk<O> & {
  setUser: (user: U) => void;
  toggle: () => void;
};

export const applyChatwootSession = <O>(
  action: ChatwootSessionAction,
  organization: O | undefined,
  identifiedUserId: number | undefined,
  sdk: ChatwootSdk<O>
): number | undefined => {
  if (action === 'reset') {
    sdk.reset();
    return undefined;
  }

  if (action === 'set-attributes' && organization !== undefined) {
    sdk.setAttributes(organization);
  }

  return identifiedUserId;
};

export const openChatwootSession = <U, O>(
  action: ChatwootSessionAction,
  {
    user,
    userId,
    organization,
  }: { user: U; userId: number; organization: O | undefined },
  sdk: ChatwootOpenSdk<U, O>
): number => {
  if (action === 'reset') {
    sdk.reset();
  }

  sdk.setUser(user);

  if (organization !== undefined) {
    sdk.setAttributes(organization);
  }

  sdk.toggle();

  return userId;
};

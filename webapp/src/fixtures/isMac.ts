export const IS_MAC = Boolean(navigator.userAgent.includes('Mac'));

export const getMeta = () => {
  return IS_MAC ? 'Cmd' : 'Ctrl';
};

export const getMetaName = () => {
  return IS_MAC ? '⌘' : 'Ctrl';
};

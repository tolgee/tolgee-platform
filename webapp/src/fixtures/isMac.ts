export const isMac = () => navigator.userAgent.indexOf('Mac OS X');

export const getMeta = () => {
  return isMac() ? 'Cmd' : 'Ctrl';
};

export const getMetaName = () => {
  return isMac() ? '⌘' : 'Ctrl';
};

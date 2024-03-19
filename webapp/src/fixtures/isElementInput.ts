export const isElementInput = (activeElement: Element) => {
  if (
    activeElement.tagName === 'TEXTAREA' ||
    activeElement.classList.contains('MuiInputBase-root') ||
    (activeElement.tagName === 'INPUT' &&
      // @ts-ignore
      !['checkbox', 'radio', 'submit', 'reset'].includes(activeElement.type))
  ) {
    return true;
  }
  return false;
};

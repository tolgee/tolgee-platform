export const isElementInput = (activeElement: Element) =>
  activeElement.tagName === 'TEXTAREA' ||
  activeElement.classList.contains('MuiInputBase-root') ||
  (activeElement as HTMLElement).isContentEditable ||
  (activeElement.tagName === 'INPUT' &&
    !['checkbox', 'radio', 'submit', 'reset'].includes(
      (activeElement as HTMLInputElement).type
    ));

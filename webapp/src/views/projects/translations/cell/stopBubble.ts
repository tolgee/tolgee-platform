export const stopBubble = (func?) => (e) => {
  e.stopPropagation();
  func?.(e);
};

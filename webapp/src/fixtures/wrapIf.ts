import React from 'react';

/**
 * If condition is met, wraps content with element
 */
export function wrapIf(
  condition: any,
  content: React.ReactNode,
  element: React.ReactElement
) {
  if (!condition) {
    return content;
  } else {
    return React.cloneElement(element, undefined, content);
  }
}

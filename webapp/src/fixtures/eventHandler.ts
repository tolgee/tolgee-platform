import { SyntheticEvent } from 'react';

export const stopBubble =
  <T = any>(func?: (e: SyntheticEvent<T>) => any): any =>
  (e: SyntheticEvent<T>) => {
    e.stopPropagation();
    func?.(e);
  };

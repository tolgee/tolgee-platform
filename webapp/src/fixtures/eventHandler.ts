import { MouseEvent } from 'react';

export const stopBubble =
  <T = any>(func?: (e: MouseEvent<T>) => any): any =>
  (e: MouseEvent<T>) => {
    e.stopPropagation();
    func?.(e);
  };

export const stopAndPrevent =
  <T = any>(func?: (e: MouseEvent<T>) => any): any =>
  (e: MouseEvent<T>) => {
    e.stopPropagation();
    e.preventDefault();
    func?.(e);
  };

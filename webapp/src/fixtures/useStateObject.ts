import { useState } from 'react';
export const useStateObject = <T extends { [key: string]: any }>(
  states: T
): T => {
  const reactStates = Object.entries(states).reduce(
    (acc, [name, initial]) => ({ ...acc, [name]: useState(initial) }),
    {}
  );

  return new Proxy(reactStates, {
    get(target, name, receiver) {
      const rv = Reflect.get(target, name, receiver);
      return rv[0];
    },
    set(target, name, value, receiver) {
      const rv = Reflect.get(target, name, receiver);
      rv[1](value);
      return true;
    },
  }) as T;
};

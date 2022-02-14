import { useRef, useState } from 'react';
import ReactList from 'react-list';
import { serializeElPosition } from '../shortcuts/tools';
import { CellPosition, KeyElement, ScrollToElement } from '../types';

export const useRefsService = () => {
  const elementsRef = useRef<Map<string, HTMLElement>>(new Map());
  const [reactList, setReactList] = useState<ReactList>();

  const focusCell = (cell: CellPosition) => {
    const element = elementsRef.current?.get(serializeElPosition(cell));
    element?.focus();
  };

  const registerElement = (data: KeyElement) =>
    elementsRef.current.set(serializeElPosition(data), data.ref);

  const unregisterElement = (data: KeyElement) => {
    elementsRef.current.delete(serializeElPosition(data));
  };

  const scrollToElement = (data: ScrollToElement) => {
    const el = elementsRef.current.get(serializeElPosition(data));
    el?.scrollIntoView(data.options);
  };

  const registerList = (list: ReactList) => {
    setReactList(list);
  };

  const unregisterList = (list: ReactList) => {
    if (reactList === list) {
      setReactList(undefined);
    }
  };

  return {
    elementsRef,
    reactList,
    focusCell,
    registerElement,
    unregisterElement,
    scrollToElement,
    registerList,
    unregisterList,
  };
};

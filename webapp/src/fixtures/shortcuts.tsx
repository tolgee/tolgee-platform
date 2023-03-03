import React from 'react';
import { KeyTemplate } from 'tg.component/key/KeyTemplate';
import {
  KeyDown,
  KeyUp,
  KeyLeft,
  KeyRight,
  KeyEnter,
  KeyShift,
} from 'tg.component/key/SvgKeys';

export type KeyType = 'Ctrl' | 'Alt' | 'Shift' | 'Meta' | 'Cmd' | string;

export type KeyMap = Record<string, ReadonlyArray<KeyType> | Array<KeyType>>;

export const splitFormula = (formula: string) => {
  return formula.split('+').map((key) => key.trim());
};

const matchesKey = (e: KeyboardEvent, key: KeyType) => {
  switch (key) {
    case 'Ctrl':
      return e.ctrlKey;
    case 'Alt':
      return e.altKey;
    case 'Shift':
      return e.shiftKey;
    case 'Meta':
    case 'Cmd':
      return e.metaKey;

    default:
      return key.length === 1
        ? e.key === key.toLocaleLowerCase()
        : e.key === key;
  }
};

export const getKeyVisual = (key: KeyType) => {
  switch (key) {
    case 'ArrowDown':
      return <KeyDown />;
    case 'ArrowUp':
      return <KeyUp />;
    case 'ArrowLeft':
      return <KeyLeft />;
    case 'ArrowRight':
      return <KeyRight />;
    case 'Enter':
      return <KeyEnter />;
    case 'Insert':
      return 'Ins';
    case 'Shift':
      return <KeyShift />;
    case 'Cmd':
      return '⌘';
    default:
      return key.length === 1 ? key.toUpperCase() : key;
  }
};

export const formatShortcut = (formula: string, spaces = true) => {
  return splitFormula(formula)
    .map(getKeyVisual)
    .map((element, i) => (
      <React.Fragment key={i}>
        {Boolean(i) && '+'}
        <KeyTemplate>{element}</KeyTemplate>
      </React.Fragment>
    ));
};

export const getEventAction = <T extends KeyMap>(
  event: KeyboardEvent,
  map: T
): keyof T | undefined => {
  return Object.entries(map).find(([_, formulas]) =>
    formulas.some((formula) =>
      splitFormula(formula).every((key) => matchesKey(event, key))
    )
  )?.[0];
};

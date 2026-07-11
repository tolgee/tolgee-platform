import { CellObject, Sheet } from 'xlsx';

function getCellColumn(location: string) {
  return location.match(/[A-Z]+/g)?.[0];
}

function getCellRow(location: string) {
  return Number(location.match(/[0-9]+/g)?.[0]);
}

/**
 *  finds cell in A column (name)
 *  and check corresponding value in the same row (offset 1 = B, 2 = C, ...)
 */
export function checkSheetProperty(
  sheet: Sheet,
  name: string,
  value: string,
  offset = 1
) {
  const keyCell = (Object.entries(sheet) as [string, CellObject][]).find(
    ([location, c]) =>
      c.v?.toString().includes(name) && getCellColumn(location) === 'A'
  );

  const valueCell = (Object.entries(sheet) as [string, CellObject][]).find(
    ([location, c]) =>
      getCellColumn(location) ===
        String.fromCodePoint('A'.codePointAt(0) + offset) &&
      keyCell?.[0] &&
      getCellRow(location) === getCellRow(keyCell[0])
  );

  assert(
    keyCell !== undefined,
    `Cell containing "${name}" is on ${keyCell?.[0]}`
  );
  assert(valueCell !== undefined, `Value cell found`);
  if (valueCell) {
    const [location, content] = valueCell;
    assert(
      content?.v?.toString().includes(value),
      `${location} contains ${value}`
    );
  }
}

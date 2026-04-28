import { styled } from '@mui/material';

export type EntryRowLayout = 'stacked' | 'flat';

/**
 * Grid template used by both the sticky header and every data row in flat mode
 * so the columns line up. Mirrors the Glossary list (see `GlossaryViewListHeader`).
 * Leading 44px is the selection checkbox column.
 */
export const flatGridColumns = (languageCount: number) =>
  `44px minmax(300px, 1fr)` +
  ` minmax(200px, 1fr)`.repeat(Math.max(languageCount, 0));

// --- Row container ---
export const StyledRow = styled('div')<{ $layout: EntryRowLayout }>`
  ${({ $layout }) =>
    $layout === 'flat'
      ? `
        display: grid;
      `
      : `
        display: flex;
      `}
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
`;

// --- Key cell (left column) ---
// Flat layout pads on all sides (matches every other grid cell). Stacked drops the
// left padding so virtual rows (no checkbox column) hug the row's left edge instead
// of carrying ~12px of dead space.
export const StyledKeyCell = styled('div')<{ $layout: EntryRowLayout }>`
  display: grid;
  grid-template-rows: auto 1fr;
  grid-template-areas:
    'key'
    'source';
  ${({ theme, $layout }) =>
    $layout === 'flat'
      ? `padding: ${theme.spacing(1.5)};`
      : `padding: ${theme.spacing(1.5, 1.5, 1.5, 0)};`}
  ${({ $layout }) =>
    $layout === 'flat' ? `min-width: 0;` : `flex: 0 0 33%; max-width: 33%;`}
`;

export const StyledKeyName = styled('div')`
  grid-area: key;
  overflow: hidden;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-family: monospace;
  margin-bottom: 4px;
`;

export const StyledSourceText = styled('div')`
  grid-area: source;
  overflow: hidden;
`;

// --- Translation cells (right column) ---
export const StyledTranslations = styled('div')<{ $layout: EntryRowLayout }>`
  display: grid;
  position: relative;
  min-width: 0;
  ${({ $layout }) =>
    $layout === 'flat'
      ? `
        display: contents;
      `
      : `
        flex: 1;
      `}
`;

export const StyledTranslationCell = styled('div')<{ $layout: EntryRowLayout }>`
  display: grid;
  grid-template-areas: ${({ $layout }) =>
    $layout === 'flat'
      ? `'translation'`
      : `'language language' 'translation translation'`};
  padding: ${({ theme, $layout }) =>
    $layout === 'flat' ? theme.spacing(1.5) : theme.spacing(0)};
  ${({ theme, $layout }) =>
    $layout === 'flat'
      ? `border-left: 1px solid ${theme.palette.divider1};`
      : `border-top: 1px solid ${theme.palette.divider1};`}
  cursor: pointer;
  min-width: 0;
  &:first-of-type {
    ${({ $layout }) => ($layout === 'flat' ? '' : 'border-top: none;')}
  }
  &:hover {
    background: ${({ theme }) => theme.palette.cell.hover};
    transition: background 0.1s ease-in;
  }
  &.editing {
    cursor: default;
    z-index: 1;
    background: transparent !important;
    box-shadow: ${({ theme }) =>
      theme.palette.mode === 'dark'
        ? '0px 0px 7px rgba(0, 0, 0, 1)'
        : '0px 0px 10px rgba(0, 0, 0, 0.2)'} !important;
  }
`;

export const StyledLanguage = styled('div')`
  display: flex;
  grid-area: language;
  gap: 8px;
  align-items: center;
  font-size: 14px;
  padding: 12px 2px 4px 16px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const StyledTranslation = styled('div')<{ $layout: EntryRowLayout }>`
  grid-area: translation;
  min-height: 23px;
  position: relative;
  ${({ theme, $layout }) =>
    $layout === 'flat'
      ? ''
      : `margin: 0px ${theme.spacing(1.5)} ${theme.spacing(2)} ${theme.spacing(
          2
        )};`}
`;

export const StyledControls = styled('div')`
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
  margin-top: 8px;
`;

export const StyledEmpty = styled('span')`
  font-style: italic;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

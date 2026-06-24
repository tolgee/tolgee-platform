import { IconButton, styled } from '@mui/material';

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
// Flat mode pushes the row separator onto each cell (StyledSelectionCell / StyledKeyCell /
// StyledTranslationCell) rather than the row itself. The row is `display: grid` and its box
// stays at the scroll container's width even when tracks overflow horizontally — a single
// `border-top` on the row therefore gets clipped at the viewport edge once the user scrolls
// right. The cells, sized by their grid tracks, paint borders across the full visible width
// of the scrolled grid.
export const StyledRow = styled('div')<{ $layout: EntryRowLayout }>`
  ${({ theme, $layout }) =>
    $layout === 'flat'
      ? `
        display: grid;
      `
      : `
        display: flex;
        border-top: 1px solid ${theme.palette.divider1};
      `}
`;

// --- Key cell (left column) ---
// Flat layout pads on all sides; stacked drops the left padding so the source text sits
// flush against the selection column instead of carrying ~12px of dead space.
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
  ${({ theme, $layout }) =>
    $layout === 'flat'
      ? `min-width: 0; border-top: 1px solid ${theme.palette.divider1};`
      : `flex: 0 0 33%; max-width: 33%;`}
`;

export const StyledKeyName = styled('div')`
  grid-area: key;
  overflow: hidden;
  font-size: 12px;
  color: ${({ theme }) => theme.palette.text.secondary};
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
  position: relative;
  display: grid;
  grid-template-areas: ${({ $layout }) =>
    $layout === 'flat'
      ? `'translation'`
      : `'language language' 'translation translation'`};
  padding: ${({ theme, $layout }) =>
    $layout === 'flat' ? theme.spacing(1.5) : theme.spacing(0)};
  ${({ theme, $layout }) =>
    $layout === 'flat'
      ? `
        border-left: 1px solid ${theme.palette.divider1};
        border-top: 1px solid ${theme.palette.divider1};
      `
      : ``}
  cursor: pointer;
  min-width: 0;
  &:hover {
    background: ${({ theme }) => theme.palette.cell.hover};
    transition: background 0.1s ease-in;
  }
  /* Pencil affordance is rendered always-on for editable cells; a sibling-state hover rule
     here brings it from 0 → 1 opacity so the cell only advertises editability when the
     pointer is over it. */
  &:hover .tm-edit-affordance {
    opacity: 1;
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

// Mirrors the translations view's ControlsButton: a 32px IconButton with a 18px icon
// centered inside, so MUI's hover-circle background has enough room around the icon to be
// clearly visible. Clicks bubble up to the parent cell, whose onClick wires the edit
// handler.
export const StyledEditAffordance = styled(IconButton)`
  position: absolute;
  top: 4px;
  right: 4px;
  width: 32px;
  height: 32px;
  color: ${({ theme }) => theme.palette.text.secondary};
  opacity: 0;
  transition: opacity 0.1s ease-in;

  & svg {
    width: 18px;
    height: 18px;
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

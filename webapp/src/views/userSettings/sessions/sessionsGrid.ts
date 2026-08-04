// Shared by the header row and every session row so their columns line up. The header lives in the
// list's subheader slot, so both sit in the same box and only need matching columns and padding.
// The action column is fixed rather than `auto`: `auto` resolves to the button's width in a row but
// to zero in the header's empty cell, which leaves every `fr` column a different width in the two.
export const SESSIONS_GRID_COLUMNS = '2.2fr 1.6fr 1.3fr 1.3fr 96px';

export const SESSIONS_ROW_PADDING = '8px 12px';

/**
 * Width of the list itself, not the viewport - the settings content column is much narrower than
 * the window, so a viewport media query would keep the table wide long after it stopped fitting.
 */
export const SESSIONS_WIDE_LAYOUT = '760px';

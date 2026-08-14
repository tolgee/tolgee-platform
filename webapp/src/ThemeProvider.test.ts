import { describe, expect, it } from 'vitest';
import { getTheme } from '@tginternal/library/theme/getTheme';
import { getWebappTheme } from './ThemeProvider';

describe.each(['light', 'dark'] as const)('getWebappTheme(%s)', (mode) => {
  const theme = getWebappTheme(mode);

  it('keeps the requested palette mode', () => {
    expect(theme.palette.mode).toBe(mode);
  });

  it('merges the webapp layer over the library CssBaseline instead of replacing it', () => {
    const overrides = theme.components?.MuiCssBaseline
      ?.styleOverrides as Record<string, unknown>;
    expect(Object.keys(overrides).sort()).toEqual([
      '#root',
      '@font-face',
      'body',
      'html',
    ]);
  });

  it('deep-merges body, the one key both layers declare', () => {
    const overrides = theme.components?.MuiCssBaseline
      ?.styleOverrides as Record<string, unknown>;
    expect(overrides.body).toMatchObject({
      // from the library theme
      minHeight: '100%',
      position: 'relative',
      fontSize: 15,
      // from the webapp layer
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'stretch',
    });
  });

  it('keeps the library component defaults', () => {
    expect(theme.components?.MuiButton?.defaultProps?.color).toBe('default');
  });

  // The library theme is the single description of component styling. The webapp layer may extend
  // an entry it already has (it extends MuiCssBaseline), never introduce one of its own.
  it('themes no component the library does not', () => {
    expect(Object.keys(theme.components ?? {}).sort()).toEqual(
      Object.keys(getTheme(mode).components ?? {}).sort()
    );
  });

  it('wires the MUI palette to the Tolgee tokens of the same mode', () => {
    expect(theme.palette.primary.main).toBe(theme.palette.tokens.primary.main);
    expect(theme.palette.text.primary).toBe(theme.palette.tokens.text.primary);
  });
});

it('gives the two modes different surfaces', () => {
  expect(getWebappTheme('light').palette.background.default).not.toBe(
    getWebappTheme('dark').palette.background.default
  );
});

it('gives the two modes different tokens', () => {
  expect(getWebappTheme('light').palette.tokens).not.toEqual(
    getWebappTheme('dark').palette.tokens
  );
});

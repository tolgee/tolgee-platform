import { useEffect, useState } from 'react';
import { addons } from 'storybook/preview-api';
// The event constants have no public entry in storybook 10.
import { GLOBALS_UPDATED, SET_GLOBALS } from 'storybook/internal/core-events';
import { THEME_KEYS } from '../themeKeys';

// `useGlobals()` throws here: preview hooks are only valid in decorators and story functions.
export const useThemeMode = (): 'light' | 'dark' => {
  const [dark, setDark] = useState(() => isDark(globalsFromUrl()));

  useEffect(() => {
    const channel = addons.getChannel();
    const onChange = ({ globals }: { globals?: Record<string, unknown> }) =>
      setDark(isDark(globals));
    channel.on(GLOBALS_UPDATED, onChange);
    channel.on(SET_GLOBALS, onChange);
    return () => {
      channel.off(GLOBALS_UPDATED, onChange);
      channel.off(SET_GLOBALS, onChange);
    };
  }, []);

  return dark ? 'dark' : 'light';
};

const isDark = (globals?: Record<string, unknown>) =>
  globals?.theme === THEME_KEYS.dark;

const globalsFromUrl = () =>
  Object.fromEntries(
    new URLSearchParams(window.location.search)
      .get('globals')
      ?.split(';')
      .map((pair) => pair.split(':')) ?? [],
  );

import { useEffect, useState } from 'react';
import { addons } from 'storybook/internal/preview-api';
import { GLOBALS_UPDATED, SET_GLOBALS } from 'storybook/internal/core-events';

// Docs pages render outside the preview decorators, so `useGlobals()` throws — preview hooks are
// only valid in decorators and story functions. Listen on the channel instead. `theme` is
// addon-themes' GLOBAL_KEY and its values are the keys registered in preview.tsx.
const isDark = (globals?: Record<string, unknown>) => globals?.theme === 'Dark';

export const useThemeMode = (): 'light' | 'dark' => {
  const [dark, setDark] = useState(
    () =>
      new URLSearchParams(window.location.search)
        .get('globals')
        ?.includes('theme:Dark') ?? false,
  );

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

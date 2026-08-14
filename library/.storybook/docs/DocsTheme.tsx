import { ThemeProvider } from '@mui/material';
import type { ReactNode } from 'react';
import { useThemes } from './themes';

export const DocsTheme = ({ children }: { children: ReactNode }) => {
  const { active } = useThemes();
  return <ThemeProvider theme={active}>{children}</ThemeProvider>;
};

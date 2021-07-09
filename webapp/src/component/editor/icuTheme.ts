import { editor } from 'monaco-editor';

const icuTheme: editor.IStandaloneThemeData = {
  base: 'vs',
  inherit: true,
  rules: [
    { token: 'string', foreground: '#000000' },
    { token: 'variable', foreground: '#155d94' },
  ],
  colors: {
    'editor.lineHighlightBorder': '#ffffff',
    'editor.lineHighlightBackground': '#efefef',
    'editor.background': '#efefef',
  },
};

export default icuTheme;

import { editor } from 'monaco-editor';

const icuTheme: (background?: string) => editor.IStandaloneThemeData = (
  background = '#efefef'
) => {
  return {
    base: 'vs',
    inherit: true,
    rules: [
      { token: 'string', foreground: '#000000' },
      { token: 'variable', foreground: '#155d94' },
    ],
    colors: {
      'editor.lineHighlightBorder': '#ffffff',
      'editor.lineHighlightBackground': background,
      'editor.background': background,
    },
  };
};

export default icuTheme;

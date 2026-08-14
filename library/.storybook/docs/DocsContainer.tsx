import { useEffect } from 'react';
import type { ComponentProps } from 'react';
import { DocsContainer as BaseDocsContainer } from '@storybook/addon-docs/blocks';
import { DARK_THEME, LIGHT_THEME } from './themes';
import { useThemeMode } from './useThemeMode';

type Props = ComponentProps<typeof BaseDocsContainer>;

const STYLE_ID = 'tolgee-docs-theme';

const css = (mode: 'light' | 'dark') => {
  const { palette } = mode === 'dark' ? DARK_THEME : LIGHT_THEME;
  const bg = palette.background.default;
  const paper = palette.background.paper;
  const text = palette.text.primary;
  const line = palette.divider;
  const code = palette.colors.grey['950'];
  const onCode = palette.colors.grey['50'];

  return `
    .sbdocs-wrapper, .sbdocs.sbdocs-content { background: ${bg}; }
    .sbdocs.sbdocs-content, .sbdocs h1, .sbdocs h2, .sbdocs h3, .sbdocs h4,
    .sbdocs p, .sbdocs li, .sbdocs strong { color: ${text}; }
    .sbdocs hr, .sbdocs h2 { border-bottom-color: ${line}; }
    .sbdocs p > code, .sbdocs li > code, .sbdocs h1 > code, .sbdocs h2 > code,
    .sbdocs h3 > code { background: ${paper}; color: ${text}; border-color: ${line}; }
    /* The surface stays dark in both modes because Storybook colors its syntax tokens for a dark
       ground. A fenced block written in MDX has no tokens, so it needs the foreground set too —
       without it the text keeps the light theme's near-black. */
    .docblock-source, pre.prismjs { background: ${code} !important;
      color: ${onCode} !important; border-color: ${line} !important; }
    .sbdocs a[href^='#'] { color: ${palette.text.secondary}; }

    /* Frame only; the canvas surface is painted by the decorator in .storybook/preview.tsx. */
    .sbdocs.sbdocs-preview { background: transparent; border-color: ${line}; }
    .sbdocs-preview .docblock-code-toggle,
    .sbdocs-preview [class*='sb-bar'] { background: ${paper}; color: ${text};
      border-color: ${line}; }
    .sb-preparing-docs, .sbdocs .sb-wrapper { background: ${bg} !important; }
    .docblock-argstable, .docblock-argstable-body, .docblock-argstable tr,
    .docblock-argstable th, .docblock-argstable td,
    .sb-argstableBlock, .sb-argstableBlock tr, .sb-argstableBlock th,
    .sb-argstableBlock td { background: ${bg} !important; color: ${text} !important;
      border-color: ${line} !important; box-shadow: none !important; }
    /* Only the table's own text, never the interactive controls: they bring their own light
       track and knob, and recoloring just the text leaves the label invisible on top of it. */
    .docblock-argstable td > span, .docblock-argstable td > div,
    .docblock-argstable td div > span,
    .docblock-argstable td code, .docblock-argstable th > span {
      color: ${text} !important; background: transparent !important; }
    .docblock-code-toggle { background: ${paper} !important; color: ${text} !important;
      border-color: ${line} !important; box-shadow: none !important; }
  `;
};

export const DocsContainer = (props: Props) => {
  const mode = useThemeMode();

  useEffect(() => {
    const el =
      document.getElementById(STYLE_ID) ??
      document.head.appendChild(
        Object.assign(document.createElement('style'), { id: STYLE_ID }),
      );
    el.textContent = css(mode);
  }, [mode]);

  return <BaseDocsContainer {...props} />;
};

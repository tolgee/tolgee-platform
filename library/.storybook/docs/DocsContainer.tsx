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
  const codeBg = palette.colors.grey['950'];
  const onCode = palette.colors.grey['50'];
  // primary.main is the brand pink; on either page background it lands at 3.7:1, under WCAG AA.
  const linkColor =
    mode === 'dark'
      ? palette.tokens.primary.light
      : palette.tokens.primary.dark;

  return `
    .sbdocs-wrapper, .sbdocs.sbdocs-content { background: ${bg}; }
    .sbdocs.sbdocs-content, .sbdocs h1, .sbdocs h2, .sbdocs h3, .sbdocs h4,
    .sbdocs p, .sbdocs li, .sbdocs strong { color: ${text}; }
    .sbdocs hr, .sbdocs h2 { border-bottom-color: ${line}; }
    .sbdocs :not(pre) > code { background: ${paper}; color: ${text};
      border-color: ${line}; }
    /* The surface is dark in both modes because a story's source is syntax-highlighted for a dark
       ground. Text without a token of its own needs the foreground set too — that is every block
       written in these MDX pages, which is why they are fenced without a language: the docs theme
       would highlight them with its light palette, unreadable here and unfixable from outside. */
    .docblock-source, pre.prismjs { background: ${codeBg} !important;
      color: ${onCode} !important; border-color: ${line} !important; }
    .sbdocs a { color: ${linkColor}; }
    /* The table of contents sits outside .sbdocs, so it keeps Storybook's own blue otherwise. */
    .toc-wrapper .toc-list-item .toc-link { color: ${palette.text.secondary}; }
    .toc-wrapper .toc-list-item .toc-link.is-active-link { color: ${text};
      font-weight: 500; }
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

// The container's `theme` prop is deliberately not used: changing it re-renders the page and the
// headings shift horizontally on every switch.
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

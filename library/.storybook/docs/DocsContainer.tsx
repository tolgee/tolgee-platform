import { useEffect } from 'react';
import type { ComponentProps } from 'react';
import { DocsContainer as BaseDocsContainer } from '@storybook/addon-docs/blocks';
import { getTheme } from '../../src/theme/getTheme';
import { useThemeMode } from './useThemeMode';

type Props = ComponentProps<typeof BaseDocsContainer>;

const STYLE_ID = 'tolgee-docs-theme';

const css = (mode: 'light' | 'dark') => {
  const { palette } = getTheme(mode);
  const bg = palette.background.default;
  const paper = palette.background.paper;
  const text = palette.text.primary;
  const line = palette.divider;
  const code = palette.colors.grey['950'];

  return `
    .sbdocs-wrapper, .sbdocs.sbdocs-content { background: ${bg}; }
    .sbdocs.sbdocs-content, .sbdocs h1, .sbdocs h2, .sbdocs h3, .sbdocs h4,
    .sbdocs p, .sbdocs li, .sbdocs strong { color: ${text}; }
    .sbdocs hr, .sbdocs h2 { border-bottom-color: ${line}; }
    /* Inline code follows the mode; the source block stays dark in both, because its syntax
       highlighting is colored for a dark background. */
    .sbdocs p > code, .sbdocs li > code, .sbdocs h1 > code, .sbdocs h2 > code,
    .sbdocs h3 > code { background: ${paper}; color: ${text}; border-color: ${line}; }
    .docblock-source, pre.prismjs { background: ${code} !important;
      border-color: ${line} !important; }
    .sbdocs a[href^='#'] { color: ${palette.text.secondary}; }

    /* Only the frame around the canvas. The canvas surface itself is painted by a decorator that
       runs inside the story's own theme provider, so it can never disagree with the component. */
    .sbdocs.sbdocs-preview { background: transparent; border-color: ${line}; }
    .sbdocs-preview .docblock-code-toggle,
    .sbdocs-preview [class*='sb-bar'] { background: ${paper}; color: ${text};
      border-color: ${line}; }
    /* Storybook styles these with emotion classes of equal specificity, so the overrides have to
       win explicitly — otherwise the props table keeps a white row under dark text. Both class
       names are needed: docblock-argstable is the loaded table, sb-argstableBlock the skeleton
       shown while it loads. */
    .sb-preparing-docs, .sbdocs .sb-wrapper { background: ${bg} !important; }
    .docblock-argstable, .docblock-argstable-body, .docblock-argstable tr,
    .docblock-argstable th, .docblock-argstable td,
    .sb-argstableBlock, .sb-argstableBlock tr, .sb-argstableBlock th,
    .sb-argstableBlock td { background: ${bg} !important; color: ${text} !important;
      border-color: ${line} !important; box-shadow: none !important; }
    /* Only the table's own text, never the interactive controls: they bring their own light
       track and knob, and recoloring just the text leaves the label invisible on top of it. */
    .docblock-argstable td > span, .docblock-argstable td > div,
    .docblock-argstable td code, .docblock-argstable th > span {
      color: ${text} !important; background: transparent !important; }
    .docblock-code-toggle { background: ${paper} !important; color: ${text} !important;
      border-color: ${line} !important; box-shadow: none !important; }
  `;
};

/**
 * Keeps every docs page on the theme picked in the toolbar, painted with the app's own surface
 * colors. The colors are swapped through a stylesheet rather than by handing the container a
 * different theme object: changing that prop remounts the whole page, which makes Storybook
 * re-insert its heading anchors and leaves the two modes with different markup.
 */
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

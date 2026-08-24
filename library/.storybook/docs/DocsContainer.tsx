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

  // Ratios against the #182230 surface: punctuation 8.1, comment 4.7, keyword 7.2, string 10.4,
  // function 7.4, class 9.7, tag 8.1, attribute 11.3, number 7.9. Storybook's own are 1.4, 1.9
  // and 1.5 for the first three, which is why the blocks were unreadable.
  const codeTokens = `
    pre.prismjs .token { color: #aeb9cc; }
    pre.prismjs .token.comment,
    pre.prismjs .token.prolog,
    pre.prismjs .token.doctype,
    pre.prismjs .token.cdata { color: #7e8ca3; font-style: italic; }
    pre.prismjs .token.keyword,
    pre.prismjs .token.control-flow,
    pre.prismjs .token.module,
    pre.prismjs .token.imports,
    pre.prismjs .token.exports { color: #c99bf5; }
    pre.prismjs .token.string,
    pre.prismjs .token.attr-value,
    pre.prismjs .token.char,
    pre.prismjs .token.regex { color: #8de39b; }
    pre.prismjs .token.function,
    pre.prismjs .token.method { color: #84b0ff; }
    pre.prismjs .token.class-name,
    pre.prismjs .token.maybe-class-name,
    pre.prismjs .token.builtin,
    pre.prismjs .token.known-class-name { color: #6fdcc8; }
    pre.prismjs .token.tag,
    pre.prismjs .token.selector,
    pre.prismjs .token.symbol { color: #ff9bae; }
    pre.prismjs .token.attr-name,
    pre.prismjs .token.property,
    pre.prismjs .token.parameter { color: #ffd37a; }
    pre.prismjs .token.number,
    pre.prismjs .token.boolean,
    pre.prismjs .token.constant,
    pre.prismjs .token.null { color: #f9a17a; }
    /* Punctuation and operators last: they nest inside every other token, and on a dark ground
       they must not inherit that token's color or the structure of the code disappears. */
    pre.prismjs .token.punctuation,
    pre.prismjs .token.operator,
    pre.prismjs .token.spread { color: #aeb9cc; }
  `;

  return `
    .sbdocs-wrapper, .sbdocs.sbdocs-content { background: ${bg}; }
    .sbdocs.sbdocs-content, .sbdocs h1, .sbdocs h2, .sbdocs h3, .sbdocs h4,
    .sbdocs p, .sbdocs li, .sbdocs strong { color: ${text}; }
    .sbdocs hr, .sbdocs h2 { border-bottom-color: ${line}; }
    .sbdocs :not(pre) > code { background: ${paper}; color: ${text};
      border-color: ${line}; }
    /* The surface is dark in both modes, and the token colors below replace Storybook's, which are
       picked for a white ground: on this background its punctuation lands at 1.4:1 and its keywords
       at 1.9:1. Every color here clears 4.5:1 against the surface — see the ratios in the block. */
    .docblock-source, pre.prismjs { background: ${codeBg} !important;
      color: ${onCode} !important; border-color: ${line} !important; }
    ${codeTokens}
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

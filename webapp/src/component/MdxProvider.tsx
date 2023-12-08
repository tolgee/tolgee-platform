import { default as React, FC } from 'react';
import { Link, Typography, useTheme } from '@mui/material';
import { MDXProvider } from '@mdx-js/react';
import Highlight, { defaultProps, Prism } from 'prism-react-renderer';
import lightTheme from 'prism-react-renderer/themes/github';
import darkTheme from 'prism-react-renderer/themes/nightOwl';
import { API_KEY_PLACEHOLDER } from 'tg.views/projects/integrate/IntegrateView';
import { styled } from '@mui/material';

(typeof global !== 'undefined' ? global : window).Prism = Prism;
// require('prismjs/components/prism-php');
// require('prismjs/components/prism-shell-session');
// require('prism-svelte');

const StyledCode = styled('pre')`
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding: 20px;
  font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas,
    Liberation Mono, monospace;
  overflow: auto;
`;

const StyledInlineCode = styled('span')`
  border-radius: ${({ theme }) => theme.shape.borderRadius};
  background-color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[100]
      : theme.palette.emphasis[200]};
  color: ${({ theme }) => theme.palette.emphasis[900]};
  padding: 4px 4px;
  font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas,
    Liberation Mono, monospace;
`;

const StyledParagraph = styled(Typography)`
  padding: ${({ theme }) => theme.spacing(1)};
`;

const StyledH1 = styled(Typography)`
  font-size: 35px;
`;

const StyledH2 = styled(Typography)`
  font-weight: 400;
  font-size: 26px;
  margin-top: ${({ theme }) => theme.spacing(6)};
`;

const StyledH3 = styled(Typography)`
  font-size: 22px;
`;

export const MdxProvider: FC<{
  modifyValue?: (code: string) => string;
}> = (props) => {
  const modifyValue = (code: string) =>
    props.modifyValue ? props.modifyValue(code) : code;

  const theme = useTheme();

  return (
    <MDXProvider
      components={{
        a: function A(props) {
          return <Link {...props} target="_blank" />;
        },
        p: function P(props) {
          return (
            <StyledParagraph variant="body1">{props.children}</StyledParagraph>
          );
        },
        h1: function H1(props) {
          return <StyledH1 variant="h1">{props.children}</StyledH1>;
        },
        h2: function H2(props) {
          return <StyledH2 variant="h2">{props.children}</StyledH2>;
        },
        h3: function H3(props) {
          return <StyledH3 variant="h3">{props.children}</StyledH3>;
        },
        inlineCode: function InlineCode(props) {
          return <StyledInlineCode {...props} className={props.className} />;
        },
        code: function Code({ children, className }) {
          const language = className?.replace(/language-/, '');
          children = children?.trim();
          return (
            <Highlight
              {...defaultProps}
              theme={theme.palette.mode === 'dark' ? darkTheme : lightTheme}
              code={children}
              language={language}
            >
              {({ className, style, tokens, getLineProps, getTokenProps }) => (
                <StyledCode
                  className={className}
                  style={{
                    ...style,
                    background: theme.palette.emphasis[50],
                  }}
                >
                  {tokens.map((line, i) => (
                    <div key={i} {...getLineProps({ line, key: i })}>
                      {line.map((token, key) => {
                        const tokenProps = getTokenProps({ token, key });
                        const splitByApiKey = tokenProps.children
                          .split(API_KEY_PLACEHOLDER)
                          .map(modifyValue);
                        tokenProps.children = insertBetweenAll(
                          splitByApiKey,
                          <span data-sentry-mask="">
                            {modifyValue(API_KEY_PLACEHOLDER)}
                          </span>
                        ).map((it, idx) => (
                          <React.Fragment key={idx}>{it}</React.Fragment>
                        ));
                        return <span key={key} {...tokenProps} />;
                      })}
                    </div>
                  ))}
                </StyledCode>
              )}
            </Highlight>
          );
        },
      }}
    >
      {props.children}
    </MDXProvider>
  );
};

const insertBetweenAll = (arr, thing) =>
  arr.flatMap((value, index, array) =>
    array.length - 1 !== index // check for the last item
      ? [value, thing]
      : value
  );

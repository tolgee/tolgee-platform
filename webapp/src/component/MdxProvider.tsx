import React, { FC } from 'react';
import { Link, Typography, useTheme } from '@mui/material';
import { styled } from '@mui/material';

const StyledCode = styled('pre')`
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
  padding: 20px;
  font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas,
    Liberation Mono, monospace;
  overflow: auto;
  background-color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[50]
      : theme.palette.emphasis[50]} !important;
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

type Props = {
  modifyValue?: (code: string) => string;
  content: React.FC<any>;
};

function replaceRecursively(
  children: React.ReactNode,
  replaceFn: (text: string) => string
) {
  if (typeof children === 'string') {
    return replaceFn(children);
  }
  if (Array.isArray(children)) {
    return children.map((child) => replaceRecursively(child, replaceFn));
  }
  if (React.isValidElement(children)) {
    return {
      ...children,
      props: {
        ...children.props,
        children: replaceRecursively(children.props.children, replaceFn),
      },
    };
  }
  return children;
}

export const MdxProvider: FC<Props> = (props) => {
  const Content = props.content;
  const modifyValue = props.modifyValue ?? ((text: string) => text);

  const theme = useTheme();

  return (
    <>
      <link
        rel="stylesheet"
        href={
          theme.palette.mode === 'dark'
            ? 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/github-dark.min.css'
            : 'https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.8.0/styles/github.min.css'
        }
      />
      <Content
        components={{
          a: function A(props) {
            return <Link {...props} target="_blank" />;
          },
          p: function P(props) {
            return (
              <StyledParagraph variant="body1">
                {props.children}
              </StyledParagraph>
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
          code: function Code(props) {
            if (
              typeof props.children === 'string' &&
              !props.children.includes('\n')
            ) {
              return (
                <StyledInlineCode {...props}>
                  {replaceRecursively(props.children, modifyValue)}
                </StyledInlineCode>
              );
            } else {
              return (
                <StyledCode {...props}>
                  {replaceRecursively(props.children, modifyValue)}
                </StyledCode>
              );
            }
          },
        }}
      />
    </>
  );
};

import { styled } from '@mui/material';

export const Container = styled('div')`
  position: relative;
  display: grid;
  margin: 0px;
  border-left: 0px;
  border-right: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;

  &::before {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    background-image: linear-gradient(90deg, #0000002c, transparent);
    top: 0px;
    left: 0px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
  }

  &::after {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    background-image: linear-gradient(-90deg, #0000002c, transparent);
    top: 0px;
    right: 0px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
  }
`;

export const VerticalScroll = styled('div')`
  overflow: auto;
  scrollbar-width: thin;
  scrollbar-color: ${({ theme }) => theme.palette.text.secondary} transparent;
  scroll-behavior: smooth;
  margin-top: ${({ theme }) => theme.spacing(0.5)};
  min-height: 350px;
`;

export const Content = styled('div')`
  position: relative;
`;

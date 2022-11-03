import { useEffect } from 'react';
import { styled } from '@mui/material';

import { useTopBarHidden } from 'tg.component/layout/TopBar/TopBarContext';
import {
  useHeaderNsDispatch,
  useHeaderNsContext,
} from '../context/HeaderNsContext';
import { useTranslate } from '@tolgee/react';

const NS_HEIGHT = 18;

const StyledContainer = styled('div')`
  position: sticky;
  box-sizing: border-box;
  top: 50px;
  margin: -12px -5px -10px -5px;
  margin-left: ${({ theme }) => theme.spacing(-2)};
  margin-right: ${({ theme }) => theme.spacing(-2)};
  background: ${({ theme }) => theme.palette.background.default};
  z-index: ${({ theme }) => theme.zIndex.appBar + 1};
  transition: transform 0.2s ease-in-out;
  overflow: visible;
  display: grid;
`;

const StyledControls = styled('div')`
  padding: ${({ theme }) => theme.spacing(0, 1.5)};
  background: ${({ theme }) => theme.palette.background.default};
`;

const StyledNs = styled('div')`
  position: absolute;
  top: 100%;
  padding: ${({ theme }) => theme.spacing(0, 1, 1, 0)};
  height: ${NS_HEIGHT + 10}px;
  overflow: hidden;
`;

const StyledNsWithShadow = styled('div')`
  padding: ${({ theme }) => theme.spacing(0, 2, 0, 2)};
  box-sizing: border-box;
  background: ${({ theme }) => theme.palette.background.default};
  border-radius: 0px 0px 10px 0px;
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 1px 6px -1px #000000, 0px 1px 6px -1px #000000'
      : '0px -1px 7px -2px #000000'};
`;

const StyledShadow = styled('div')`
  background: ${({ theme }) => theme.palette.background.default};
  height: 1px;
  position: sticky;
  z-index: ${({ theme }) => theme.zIndex.appBar};
  margin-left: ${({ theme }) => theme.spacing(-1)};
  margin-right: ${({ theme }) => theme.spacing(-1)};
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 1px 6px 0px #000000, 0px 1px 6px 0px #000000'
      : '0px -1px 7px 0px #000000'};
  transition: transform 0.25s;
`;

type Props = {
  height: number;
};

export const StickyHeader: React.FC<Props> = ({ height, children }) => {
  const topBarDispatch = useHeaderNsDispatch();
  const topBarHidden = useTopBarHidden();
  const topNamespace = useHeaderNsContext((c) => c.topNamespace);
  const t = useTranslate();

  useEffect(() => {
    topBarDispatch({
      type: 'TOP_BAR_HEIGHT',
      payload: height + (topBarHidden ? 0 : 50),
    });
  }, [topBarHidden]);

  return (
    <>
      <StyledContainer
        style={{
          height: height + 5,
          transform: topBarHidden
            ? 'translate(0px, -55px)'
            : 'translate(0px, 0px)',
        }}
      >
        <StyledControls style={{ height }}>{children}</StyledControls>
        {topNamespace !== undefined && (
          <StyledNs>
            <StyledNsWithShadow>
              {topNamespace || t('namespace_default')}
            </StyledNsWithShadow>
          </StyledNs>
        )}
      </StyledContainer>
      <StyledShadow
        style={{
          top: 54 + height,
          transform: topBarHidden
            ? `translate(0px, -55px)`
            : `translate(0px, 0px)`,
        }}
      />
    </>
  );
};

import { useEffect } from 'react';
import { styled } from '@mui/material';

import {
  useHeaderNsActions,
  useHeaderNsContext,
} from '../context/HeaderNsContext';
import { NamespaceContent } from '../Namespace/NamespaceContent';
import { useColumnsContext } from '../context/ColumnsContext';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

const StyledContainer = styled('div')`
  position: sticky;
  box-sizing: border-box;
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
  top: calc(100% + 1px);
  padding: ${({ theme }) => theme.spacing(0, 2, 2, 2)};
  overflow: hidden;
  width: 100%;
  display: flex;
  justify-content: flex-start;
`;

const StyledShadow = styled('div')`
  background: ${({ theme }) => theme.palette.divider1};
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
  const { setTopBarHeight } = useHeaderNsActions();
  const topNamespace = useHeaderNsContext((c) => c.topNamespace);
  const columnSizes = useColumnsContext((c) => c.columnSizes);
  const topBannerHeight = useGlobalContext((c) => c.topBannerHeight);
  const topBarHidden = useGlobalContext((c) => !c.topBarHeight);

  useEffect(() => {
    setTopBarHeight(height + (topBarHidden ? 0 : 50));
  }, [topBarHidden]);

  return (
    <>
      <StyledContainer
        style={{
          top: 50 + topBannerHeight,
          height: height + 5,
          transform: topBarHidden
            ? `translate(0px, -55px)`
            : `translate(0px, 0px)`,
        }}
      >
        <StyledControls style={{ height }}>{children}</StyledControls>
        {topNamespace !== undefined && (
          <StyledNs data-cy="translations-namespace-banner">
            <NamespaceContent
              maxWidth={columnSizes[0]}
              namespace={topNamespace}
              sticky={true}
            />
          </StyledNs>
        )}
      </StyledContainer>
      <StyledShadow
        style={{
          top: 55 + height + topBannerHeight,
          transform: topBarHidden
            ? `translate(0px, -55px)`
            : `translate(0px, 0px)`,
        }}
      />
    </>
  );
};

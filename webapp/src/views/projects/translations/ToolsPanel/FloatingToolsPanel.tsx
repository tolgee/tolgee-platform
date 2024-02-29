import { useEffect, useMemo, useRef, useState } from 'react';
import { styled } from '@mui/material';

import { useGlobalContext } from 'tg.globalContext/GlobalContext';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { ToolsPanel } from './ToolsPanel';
import { useHeaderNsContext } from '../context/HeaderNsContext';

const StyledContainer = styled('div')`
  position: relative;
  position: sticky;
  box-sizing: border-box;
  width: 24vw;
  min-width: 300px;
  height: 800px;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-bottom: 0px;
  border-right: 0px;
  margin-top: 10px;
  transition: top 0.2s ease-in-out;
  margin-bottom: -20px;
  overflow-y: auto;
  overflow-x: hidden;
`;

export const FloatingToolsPanel = () => {
  const containerRef = useRef<HTMLDivElement>(null);
  const topBannerHeight = useGlobalContext((c) => c.topBannerHeight);
  const topBarHeight = useGlobalContext((c) => c.topBarHeight);
  const keyId = useTranslationsSelector((c) => c.cursor?.keyId);
  const languageTag = useTranslationsSelector((c) => c.cursor?.language);
  const languages = useTranslationsSelector((c) => c.languages);
  const [fixedTopDistance, setFixedTopDistance] = useState(0);

  useEffect(() => {
    function recalculate() {
      const position = containerRef.current?.getBoundingClientRect();
      setFixedTopDistance(position?.top ?? 0);
    }

    recalculate();
    addEventListener('scroll', recalculate);
    addEventListener('resize', recalculate);
    const interval = setInterval(recalculate, 500);

    return () => {
      removeEventListener('scroll', recalculate);
      removeEventListener('resize', recalculate);
      clearInterval(interval);
    };
  }, [topBarHeight]);

  const language = useMemo(() => {
    return languages?.find((l) => l.tag === languageTag);
  }, [languageTag, languages]);

  const floatingBannerHeight = useHeaderNsContext(
    (c) => c?.floatingBannerHeight ?? 0
  );

  return (
    <StyledContainer
      key={`${keyId}.${language?.id}`}
      style={{
        top: topBannerHeight + topBarHeight + floatingBannerHeight,
        height: `calc(${-fixedTopDistance}px + 100vh)`,
        maxHeight: `calc(${-(
          topBannerHeight +
          topBarHeight +
          floatingBannerHeight
        )}px + 100vh)`,
      }}
      ref={containerRef}
    >
      <ToolsPanel />
    </StyledContainer>
  );
};

import { useEffect, useMemo, useRef, useState } from 'react';
import { Box, Button, ButtonGroup, styled } from '@mui/material';

import { useGlobalContext } from 'tg.globalContext/GlobalContext';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { ToolsPanel } from './ToolsPanel';
import { useHeaderNsContext } from '../context/HeaderNsContext';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { AiPlayground } from './AiPlayground';

const StyledContainer = styled('div')`
  position: relative;
  position: sticky;
  box-sizing: border-box;
  height: 800px;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-bottom: 0px;
  border-right: 0px;
  transition: top 0.2s ease-in-out;
  margin-bottom: -20px;
  overflow-y: auto;
  overflow-x: hidden;
`;

const StyledButton = styled(Button)`
  font-size: 12px;
  padding: 4px 8px;
`;

type Props = {
  width: number;
};

export const FloatingToolsPanel = ({ width }: Props) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const topBannerHeight = useGlobalContext((c) => c.layout.topBannerHeight);
  const topBarHeight = useGlobalContext((c) => c.layout.topBarHeight);
  const keyId = useTranslationsSelector((c) => c.cursor?.keyId);
  const languageTag = useTranslationsSelector((c) => c.cursor?.language);
  const languages = useTranslationsSelector((c) => c.languages);
  const [fixedTopDistance, setFixedTopDistance] = useState(0);
  const needsNamespaceMargin = useTranslationsSelector(
    (c) => Boolean(c.translations?.[0]?.keyNamespace) && c.view === 'LIST'
  );

  const [aiPlayground, setAiPlayground] = useUrlSearchState('aiPlayground', {
    defaultVal: undefined,
    history: false,
  });

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
      key={aiPlayground ? undefined : `${keyId}.${language?.id}`}
      style={{
        top: topBannerHeight + topBarHeight + floatingBannerHeight,
        height: `calc(${-fixedTopDistance}px + 100vh)`,
        maxHeight: `calc(${-(
          topBannerHeight +
          topBarHeight +
          floatingBannerHeight
        )}px + 100vh)`,
        width,
        marginTop: needsNamespaceMargin ? 7 : 0,
      }}
      ref={containerRef}
    >
      <Box display="flex" justifyContent="center" paddingTop="8px">
        <ButtonGroup size="small" variant="outlined">
          <StyledButton
            size="small"
            color={!aiPlayground ? 'primary' : 'default'}
            onClick={() => setAiPlayground(undefined)}
          >
            Translation tools
          </StyledButton>
          <StyledButton
            size="small"
            color={aiPlayground ? 'primary' : 'default'}
            onClick={() => setAiPlayground('1')}
          >
            Ai playground
          </StyledButton>
        </ButtonGroup>
      </Box>
      {aiPlayground ? <AiPlayground /> : <ToolsPanel />}
    </StyledContainer>
  );
};

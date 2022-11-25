import { useEffect, useRef } from 'react';
import { useState } from 'react';
import { useTranslationsSelector } from './TranslationsContext';
import { useDebouncedCallback } from 'use-debounce';
import { NsBannerRecord, useNsBanners } from './useNsBanners';
import { createProviderNew } from 'tg.fixtures/createProviderNew';

/**
 * Context responsible for top namespace banner in translations header
 * keeps track of banner elements and decides which one should be displayed
 */
export const [HeaderNsContext, useHeaderNsActions, useHeaderNsContext] =
  createProviderNew(() => {
    const translations = useTranslationsSelector((c) => c.translations);
    const reactList = useTranslationsSelector((c) => c.reactList);
    const [topNamespace, setTopNamespace] = useState<
      NsBannerRecord | undefined
    >(undefined);
    const [topBarHeight, setTopBarHeight] = useState(0);

    const nsElements = useRef<Record<number, HTMLElement | undefined>>({});

    const bannersRef = useRef([] as NsBannerRecord[]);

    bannersRef.current = useNsBanners();

    const calculateTopNamespace = useDebouncedCallback(() => {
      const nsBanners = bannersRef.current;
      const [start, end] = reactList?.getVisibleRange() || [0, 0];

      function isAfterTreshold(
        row: number,
        el: HTMLElement | undefined,
        isFirst: boolean
      ) {
        if (row < start) {
          // is before current sliding window
          return false;
        }
        if (row > end) {
          // is after current sliding window
          return true;
        }
        if (!el || !el.isConnected) {
          // element doesn't exist
          return false;
        }
        const advance = !isFirst ? 5 : 0;
        const top = el.getBoundingClientRect()!.top;
        // check exact location
        return top > topBarHeight + advance;
      }

      // take first banner that is after `start`
      let index = nsBanners.findIndex(({ row }, i) =>
        isAfterTreshold(row, nsElements.current[row], i === 0)
      );

      if (index === -1) {
        // if not found put the last in
        index = nsBanners.length;
      }

      index -= 1;

      const topBanner = nsBanners[index];

      setTopNamespace(topBanner);
    });

    useEffect(() => {
      calculateTopNamespace();
    }, [reactList, topBarHeight, nsElements, translations]);

    useEffect(() => {
      window.addEventListener('scroll', calculateTopNamespace, {
        passive: true,
      });
      window.addEventListener('resize', calculateTopNamespace, {
        passive: true,
      });
      return () => {
        window.removeEventListener('scroll', calculateTopNamespace);
        window.removeEventListener('resize', calculateTopNamespace);
      };
    }, []);

    const actions = {
      nsRefRegister(index: number, el: HTMLElement | undefined) {
        nsElements.current[index] = el;
        calculateTopNamespace();
      },
      setTopBarHeight(height: number) {
        setTopBarHeight(height);
      },
    };

    const contextData = {
      topBarHeight,
      topNamespace,
    };

    return [contextData, actions];
  });

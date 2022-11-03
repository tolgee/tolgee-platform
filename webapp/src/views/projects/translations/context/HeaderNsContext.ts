import { useEffect, useMemo, useRef } from 'react';
import { useState } from 'react';
import { createProvider } from 'tg.fixtures/createProvider';
import { useTranslationsSelector } from './TranslationsContext';
import { useDebouncedCallback } from 'use-debounce';

type ActionType =
  | {
      type: 'NS_REF_REGISTER';
      payload: { index: number; el: HTMLElement | undefined };
    }
  | { type: 'TOP_BAR_HEIGHT'; payload: number };

/**
 * Context responsible for top namespace banner in translations header
 * keeps track of banner elements and decides which one should be displayed
 */
export const [HeaderNsContext, useHeaderNsDispatch, useHeaderNsContext] =
  createProvider(() => {
    const translations = useTranslationsSelector((c) => c.translations);
    const reactList = useTranslationsSelector((c) => c.reactList);
    const [topNamespace, setTopNamespace] = useState<string | undefined>(
      undefined
    );
    const [topBarHeight, setTopBarHeight] = useState(0);

    const nsElements = useRef<Record<number, HTMLElement | undefined>>({});

    const bannersRef = useRef([] as { name: string; row: number }[]);

    bannersRef.current = useMemo(() => {
      const nsBanners = [] as { name: string; row: number }[];
      let lastNamespace: undefined | string = undefined;
      translations?.forEach((translation, i) => {
        const keyNamespace = translation.keyNamespace;
        if (lastNamespace !== keyNamespace && keyNamespace) {
          nsBanners.push({
            name: keyNamespace,
            row: i,
          });
        }
        lastNamespace = keyNamespace;
      });
      return nsBanners;
    }, [translations]);

    const calculateTopNamespace = useDebouncedCallback(() => {
      const nsBanners = bannersRef.current;
      function isVisible(el: HTMLElement, isFirst: boolean) {
        const advance = !isFirst ? 10 : 0;
        if (!el.isConnected) {
          return false;
        }
        const top = el.getBoundingClientRect()!.top;
        return top > topBarHeight + advance;
      }

      const [start] = reactList?.getVisibleRange() || [0, 0];

      // take first banner that is after `start`
      let index = nsBanners.findIndex(({ row }) => row >= start);

      if (index === -1) {
        // if not found put the last in
        index = nsBanners.length - 1;
      }

      const banner = nsBanners[index];
      if (banner) {
        if (nsElements.current[banner.row]) {
          // banner is rendered
          if (isVisible(nsElements.current[banner.row]!, index === 0)) {
            // banner is still visible, use previous
            index -= 1;
          }
        } else {
          // banner not rendered
          if (banner.row > start) {
            // banner is below or in current view
            index -= 1;
          } else {
            // wait until rendered
            index = -1;
          }
        }
      } else {
        index = -1;
      }

      const topBanner = nsBanners[index];

      setTopNamespace(topBanner?.name);
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

    const dispatch = async (action: ActionType) => {
      switch (action.type) {
        case 'NS_REF_REGISTER':
          nsElements.current[action.payload.index] = action.payload.el;
          calculateTopNamespace();
          break;

        case 'TOP_BAR_HEIGHT':
          return setTopBarHeight(action.payload);
      }
    };

    const contextData = {
      topBarHeight,
      topNamespace,
    };

    return [contextData, dispatch];
  });

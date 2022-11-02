import { useEffect, useRef } from 'react';
import { useState } from 'react';
import { createProvider } from 'tg.fixtures/createProvider';
import { useTranslationsSelector } from './TranslationsContext';

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

    // ns banners keeps structure of where the banners are
    const nsBanners = useRef<
      Map<number, { name: string; index: number; ref: HTMLElement | undefined }>
    >(new Map());

    // recalculate the structure in effect
    useEffect(() => {
      nsBanners.current = new Map();
      let lastNamespace: undefined | string = undefined;
      translations?.forEach((translation, i) => {
        const keyNamespace = translation.keyNamespace;
        if (lastNamespace !== keyNamespace && keyNamespace) {
          nsBanners.current.set(i, {
            name: keyNamespace,
            ref: undefined,
            index: i,
          });
        }
        lastNamespace = keyNamespace;
      });
    }, [translations]);

    useEffect(() => {
      function isVisible(el: HTMLElement, isFirst: boolean) {
        const advance = !isFirst ? 10 : 0;
        const top = el.getBoundingClientRect()!.top;
        return top > topBarHeight + advance;
      }

      function calculateTopNamespace() {
        const [start, end] = reactList?.getVisibleRange() || [0, 0];
        const sortedByIndex = [...nsBanners.current]
          .sort((a, b) => a[0] - b[0])
          .map((i) => i[1]);

        // take first banner that is after `start`
        let bannerIndex = sortedByIndex.findIndex(
          ({ index }) => index >= start
        );

        if (bannerIndex === -1) {
          // if not found put the last in
          bannerIndex = sortedByIndex.length - 1;
        }

        // check if banner is visible
        const candidateVisible =
          sortedByIndex[bannerIndex]?.ref &&
          isVisible(sortedByIndex[bannerIndex].ref!, bannerIndex === 0);

        // if visible put previous in
        const index = candidateVisible ? bannerIndex - 1 : bannerIndex;
        const banner = sortedByIndex[index];

        // banner needs to be before `end` of visible range
        const topNamespace =
          banner && banner.index < end ? banner.name : undefined;

        setTopNamespace(topNamespace);
      }
      calculateTopNamespace();

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
    }, [reactList, topBarHeight]);

    const dispatch = async (action: ActionType) => {
      switch (action.type) {
        case 'NS_REF_REGISTER': {
          const value = nsBanners.current.get(action.payload.index);
          if (value) {
            nsBanners.current.set(action.payload.index, {
              ...value,
              ref: action.payload.el,
            });
          }
          break;
        }
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

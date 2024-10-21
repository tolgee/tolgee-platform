'use client';

import { TolgeeBase } from './shared';
import { TolgeeProvider, useTolgeeSSR } from '@tolgee/react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

type Props = {
  locales: any;
  locale: string;
  children: React.ReactNode;
};

const tolgee = TolgeeBase().init();

export const TolgeeNextProvider = ({ locale, locales, children }: Props) => {
  const tolgeeSSR = useTolgeeSSR(tolgee, locale, locales);
  const router = useRouter();

  useEffect(() => {
    const { unsubscribe } = tolgeeSSR.on('permanentChange', () => {
      router.refresh();
    });

    return () => unsubscribe();
  }, [tolgeeSSR, router]);

  return (
    <TolgeeProvider
      tolgee={tolgeeSSR}
      options={{ useSuspense: false }}
      fallback="Loading"
    >
      {children}
    </TolgeeProvider>
  );
};

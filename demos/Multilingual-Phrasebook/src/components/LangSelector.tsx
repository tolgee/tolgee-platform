'use client';

import React, { ChangeEvent, useTransition } from 'react';
import { useTolgee } from '@tolgee/react';
import { usePathname, useRouter } from '@/navigation';

export const LangSelector: React.FC = () => {
  const tolgee = useTolgee(['language']);
  const locale = tolgee.getLanguage();
  const router = useRouter();
  const pathname = usePathname();
  const [_, startTransition] = useTransition();

  function onSelectChange(event: ChangeEvent<HTMLSelectElement>) {
    const nextLocale = event.target.value;
    startTransition(() => {
      router.replace(pathname, { locale: nextLocale });
    });
  }

  return (
    <select
      className="px-4 py-2 border border-blue-950 rounded-md bg-[#23283f] text-white focus:outline-none focus:ring-2 focus:ring-blue-950"
      onChange={onSelectChange}
      value={locale}
    >
      <option value="en">English</option>
      <option value="ar">Arabic</option>
      <option value="hi">Hindi</option>
      <option value="es">Spanish</option>
      <option value="ja">Japanese</option>
    </select>
  );
};

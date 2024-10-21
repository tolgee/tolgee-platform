import React from 'react';
import { LangSelector } from './LangSelector';
import { getTranslate } from '@/tolgee/server';

export const Navbar = async ({ children }: React.PropsWithChildren) => {
  const t = await getTranslate();
  return (
    <nav className="bg-[#0f1525]] text-white fixed top-0 left-0 w-full shadow-xl z-50">
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div className="flex justify-between items-center h-20">
        <div className="flex items-center">
          <div className="text-2xl font-bold flex items-center gap-5">
          <img src="/img/appLogo.svg" className='h-8'/>
          <h1 className="text-2xl font-normal">{t('app-title')}</h1>

            {children}
          </div>
        </div>
        <div className="flex items-center space-x-4">
          <span className='max-sm:hidden'>{t('select-lang')}</span>
          <LangSelector />
        </div>
      </div>
    </div>
  </nav>
  );
};

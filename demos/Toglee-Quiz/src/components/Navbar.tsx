import React from 'react';
import { LangSelector } from './LangSelector';

export const Navbar = ({ t, children }: { t: any; children?: React.ReactNode }) => {
  return (
    <nav className="bg-[#0f1525] text-white fixed top-0  w-full justify-center items-center shadow-xl z-50">
      <div className="sm:px-6 lg:px-8">
        <div className="flex justify-between items-center  h-20">
          <div className="flex items-center">
            <div className="text-2xl font-bold flex color-white items-center gap-5">
              <img src="/img/capital.svg" className="h-8" />
              <h1 className="text-2xl font-normal">{t('app-title')}</h1>
              {children}
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <span className="max-sm:hidden">{t('select-lang')}</span>
            <LangSelector />
          </div>
        </div>
      </div>
    </nav>
  );
};

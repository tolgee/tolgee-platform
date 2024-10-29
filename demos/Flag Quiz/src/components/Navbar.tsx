import React from 'react';
import { LangSelector } from './LangSelector';

export const Navbar = ({ t, children }: { t: any; children?: React.ReactNode }) => {
  return (
<nav className="bg-[#0f1525] w-full top-0 sticky z-50 px-4 shadow-lg">
  <div className="sm:px-6 lg:px-8 w-full">
    <div className="flex justify-between items-center h-20 w-full">
      <div className="flex items-center space-x-4">
        <img src="/flag/quiz.png" alt="Quiz Logo" className="h-20 max-md:h-10 w-auto max-sm:h-16" />
        <h1 className="text-2xl font-semibold text-white max-md:text-lg">
          {children}
        </h1>
      </div>

      <div className="flex items-center space-x-4">
        <span className="hidden sm:inline-block text-white text-opacity-75">{t('select-lang')}</span>
        <LangSelector />
      </div>
    </div>
  </div>
</nav>

  );
};

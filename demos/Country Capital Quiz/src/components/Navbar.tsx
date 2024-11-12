import React from 'react';
import { LangSelector } from './LangSelector';

export const Navbar = ({ t, children }: { t: any; children?: React.ReactNode }) => {
  return (
    <nav className='bg-[#0f1525] top-0 sticky px-4 max-md:text-sm z-50'>
{/* <nav className="bg-[#0f1525] text-white fixed top-0 w-full justify-center items-center shadow-xl z-50"> */}
<div className="sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-20">
          <div className="flex items-center">
            <div className="text-2xl font-bold flex color-white items-center gap-5 max-md:gap-3">
              <img src="/img/capital.svg" className="h-8 max-md:h-6" />
              <h1 className="text-2xl font-normal max-md:text-sm ">{t('app-title')}</h1>
              {children}
            </div>
          </div>
          <div className="flex items-center space-x-4 max-md:space-x-2">
            <span className="max-sm:hidden">{t('select-lang')}</span>
            <LangSelector />
          </div>
        </div>
      </div>
    </nav>
  );
};

import React from 'react';
import { LangSelector } from './LangSelector';
import { getTranslate } from '@/tolgee/server';

export const Navbar = async ({ children }: React.PropsWithChildren) => {
  const t = await getTranslate();

  return (
    <nav className="fixed top-0 left-0 w-full bg-white/80 backdrop-blur-md shadow-lg z-50">
      <div className="max-w-7xl mx-auto px-6">
        <div className="flex justify-between items-center h-20">
          <div className="flex items-center">
            <div className="flex items-center gap-5">
              {/* Logo and App Name with Moving Gradient */}
              <h1 className="text-3xl font-semibold flex items-center gap-2 group hover:scale-105 transition-all duration-300 ease-in-out">
                <span className="moving-gradient font-bold">
                  {t('app-name')}
                </span>
                <svg xmlns="http://www.w3.org/2000/svg" width="30" height="30" fill="currentColor" className="bi bi-wallet" viewBox="0 0 16 16">
                  <path d="M0 3a2 2 0 0 1 2-2h13.5a.5.5 0 0 1 0 1H15v2a1 1 0 0 1 1 1v8.5a1.5 1.5 0 0 1-1.5 1.5h-12A2.5 2.5 0 0 1 0 12.5zm1 1.732V12.5A1.5 1.5 0 0 0 2.5 14h12a.5.5 0 0 0 .5-.5V5H2a2 2 0 0 1-1-.268M1 3a1 1 0 0 0 1 1h12V2H2a1 1 0 0 0-1 1" />
                </svg>
              </h1>
              {children}
            </div>
          </div>

          {/* Language Selector Section */}
          <div className="flex items-center gap-4">
            <span className="max-sm:hidden text-gray-600 font-medium">
              {t('select-lang')}
            </span>
            {/* Language Selector with Glow Effect */}
            <div className="group relative">
              <div className="absolute -inset-1 bg-gradient-to-r from-blue-600 to-purple-600 rounded-lg blur opacity-0 group-hover:opacity-25 transition duration-300"></div>
              <div className="relative">
                <LangSelector />
              </div>
            </div>
          </div>
        </div>
      </div>
    </nav>
  );
};

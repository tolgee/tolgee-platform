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
              {/* Logo and App Name */}
              <h1 className="text-2xl font-semibold flex items-center gap-2 group hover:scale-105 transition-all duration-300 ease-in-out">
                <span className="text-3xl animate-bounce">📖</span>
                <span className="bg-gradient-to-r from-blue-600 via-purple-600 to-indigo-600 bg-clip-text text-transparent font-bold">
                  {t('app-name')}
                </span>
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
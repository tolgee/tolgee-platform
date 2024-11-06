import React from 'react';
import { LangSelector } from './LangSelector';
import { getTranslate } from '@/tolgee/server';
import { Globe2 } from 'lucide-react';

interface NavbarProps extends React.PropsWithChildren {}

export const Navbar: React.FC<NavbarProps> = async ({ children }) => {
  const t = await getTranslate();

  return (
    <nav className="fixed top-0 left-0 w-full z-50 shadow-lg">
      {/* Gradient line at the top */}
      <div className="h-1 w-full bg-gradient-to-r from-purple-600 via-pink-500 to-purple-600" />

      {/* Main navbar content */}
      <div className="bg-white/10 backdrop-blur-md border-b border-white/20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Left side - Logo and App Name */}
            <div className="flex items-center space-x-4">
              <div className="relative group">
                <div className="absolute -inset-1 bg-gradient-to-r from-purple-600 to-pink-600 rounded-full blur opacity-30 group-hover:opacity-50 transition duration-200" />
                <img
                  src="/img/appLogo.svg"
                  className="relative h-10 w-10 rounded-full shadow-lg ring-2 ring-white/20 transition-transform duration-200 transform hover:scale-110"
                  alt="App Logo"
                />
              </div>
              <h1 className="text-2xl font-bold bg-gradient-to-r from-purple-400 via-pink-400 to-purple-400 bg-clip-text text-transparent transition duration-200 hover:text-opacity-80">
                {t('app-name')}
              </h1>
            </div>
            {/* Navigation items */}
            <div className="hidden md:flex items-center space-x-4 text-gray-200">
              {children}
            </div>

            {/* Right side - Language Selector */}
            <div className="flex items-center space-x-4">
              <div className="hidden sm:flex items-center space-x-2 text-gray-300 hover:text-white transition duration-200">
                <Globe2 className="h-5 w-5" />
                <span className="text-sm font-medium">{t('select-lang')}</span>
              </div>
              <div className="relative">
                <div className="absolute -inset-1 bg-gradient-to-r from-purple-600 to-pink-600 rounded-lg blur opacity-20 group-hover:opacity-30 transition duration-200" />
                <div className="relative">
                  <LangSelector />
                </div>
              </div>
            </div>
          </div>

          {/* Mobile navigation */}
          <div className="md:hidden px-2 pb-3">
            {children}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

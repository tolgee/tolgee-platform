'use client';
import { useState } from 'react';
import { FaArrowLeft, FaArrowRight, FaBookmark, FaList, FaMoon, FaSun } from 'react-icons/fa';
import { usePathname } from 'next/navigation';
import arPhrases from '@/data/ar-phrases.json';
import enPhrases from '@/data/en-phrases.json';
import esPhrases from '@/data/es-phrases.json';
import hiPhrases from '@/data/hi-phrases.json';
import jaPhrases from '@/data/ja-phrases.json';
import { useTranslate } from '@tolgee/react';

export default function Book() {
  const path = usePathname();
  const { t } = useTranslate();

  let Phrases = enPhrases;
  switch (path) {
    case '/ar':
      Phrases = arPhrases;
      break;
    case '/es':
      Phrases = esPhrases;
      break;
    case '/hi':
      Phrases = hiPhrases;
      break;
    case '/ja':
      Phrases = jaPhrases;
      break;
    default:
      Phrases = enPhrases;
      break;
  }

  const [currentPage, setCurrentPage] = useState(0);
  const [animate, setAnimate] = useState(false);
  const [bookmark, setBookmark] = useState<number | null>(null);
  const [showTOC, setShowTOC] = useState(false);
  const [darkMode, setDarkMode] = useState(false); // State for dark mode

  const handlePageChange = (newPage: number) => {
    setAnimate(true);
    setTimeout(() => {
      setCurrentPage(newPage);
      setAnimate(false);
    }, 500);
  };

  const handleBookmark = () => {
    setBookmark(currentPage);
  };

  const goToBookmark = () => {
    if (bookmark !== null) {
      handlePageChange(bookmark);
    }
  };

  const toggleTOC = () => {
    setShowTOC(!showTOC);
  };

  const toggleDarkMode = () => {
    setDarkMode(!darkMode); // Toggle dark mode state
  };

  return (
    <div className={`flex items-center justify-center h-screen ${darkMode ? 'bg-gray-800' : 'bg-gradient-to-tr from-indigo-700 via-purple-600 to-blue-500'}`}>
      <div className={`max-w-3xl w-full ${darkMode ? 'bg-gray-900' : 'bg-white/90'} rounded-xl shadow-2xl p-8 relative overflow-hidden transition-transform duration-700 ease-in-out transform hover:scale-105`}>
        <div className={`absolute inset-0 ${darkMode ? 'bg-gradient-to-br from-gray-700 to-gray-900' : 'bg-gradient-to-br from-blue-400/40 via-purple-300/30 to-indigo-400/10'} opacity-20 rounded-xl pointer-events-none animate-pulse`}></div>
        
        <div className={`absolute inset-0 border-2 border-transparent rounded-xl ${darkMode ? 'bg-gradient-to-r from-gray-600 to-gray-800' : 'bg-gradient-to-r from-indigo-500 to-purple-500'} opacity-25 hover:opacity-75 transition-opacity duration-500 pointer-events-none`}></div>

        <h1 className={`text-5xl font-extrabold text-center ${darkMode ? 'text-white' : 'text-indigo-900'} mb-8 tracking-wide shadow-lg transition duration-700 ease-in-out transform hover:rotate-1 hover:scale-105`}>
          {t('book-title')}
        </h1>

        <div
          className={`text-xl leading-relaxed ${darkMode ? 'text-gray-300' : 'text-gray-800'} text-center mb-10 px-4 transition-all duration-500 ease-in-out transform ${animate ? 'opacity-0 -translate-y-5 rotate-y-180' : 'opacity-100 translate-y-0 rotate-y-0'}`}
          style={{ perspective: '1000px', transformStyle: 'preserve-3d' }}
        >
          {Phrases[currentPage]}
        </div>

        <div className={`text-base font-semibold ${darkMode ? 'text-gray-400' : 'text-gray-600'} text-center mb-8`}>
          {t('page')} {currentPage + 1} / {Phrases.length}
        </div>

        <div className="flex justify-between items-center mb-4">
          <button
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 0}
            className={`px-6 py-3 rounded-full flex items-center justify-center ${darkMode ? 'bg-gradient-to-r from-gray-700 to-gray-600' : 'bg-gradient-to-r from-purple-400 to-indigo-500'} text-white hover:from-indigo-500 hover:to-purple-400 focus:outline-none focus:ring-4 focus:ring-indigo-400/50 disabled:opacity-50 transition-all duration-300 ease-in-out shadow-lg transform hover:-translate-y-1`}
          >
            <FaArrowLeft className="mr-2" />
            {t('previous')}
          </button>
          
          <button
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === Phrases.length - 1}
            className={`px-6 py-3 rounded-full flex items-center justify-center ${darkMode ? 'bg-gradient-to-r from-gray-700 to-gray-600' : 'bg-gradient-to-r from-purple-400 to-indigo-500'} text-white hover:from-indigo-500 hover:to-purple-400 focus:outline-none focus:ring-4 focus:ring-indigo-400/50 disabled:opacity-50 transition-all duration-300 ease-in-out shadow-lg transform hover:-translate-y-1`}
          >
            {t('next')}
            <FaArrowRight className="ml-2" />
          </button>
        </div>

        <div className="flex justify-center space-x-6 mt-4">
          <button
            onClick={handleBookmark}
            className={`flex items-center ${darkMode ? 'text-gray-200' : 'text-indigo-700'} font-semibold hover:${darkMode ? 'text-gray-300' : 'text-indigo-500'} transition-colors`}
          >
            <FaBookmark className="mr-2" />
            {bookmark !== null ? `${t('go-to-bookmark')}` : `${t('bookmark')}`}
          </button>

          {bookmark !== null && (
            <button onClick={goToBookmark} className={`${darkMode ? 'text-gray-200' : 'text-indigo-700'} font-semibold hover:${darkMode ? 'text-gray-300' : 'text-indigo-500'} transition-colors`}>
              {t('go-to-bookmark')}
            </button>
          )}

          <button
            onClick={toggleTOC}
            className={`flex items-center ${darkMode ? 'text-gray-200' : 'text-indigo-700'} font-semibold hover:${darkMode ? 'text-gray-300' : 'text-indigo-500'} transition-colors`}
          >
            <FaList className="mr-2" />
            {t('table-of-contents')}
          </button>

          <button
            onClick={toggleDarkMode}
            className={`flex items-center ${darkMode ? 'text-gray-200' : 'text-indigo-700'} font-semibold hover:${darkMode ? 'text-gray-300' : 'text-indigo-500'} transition-colors`}
          >
            {darkMode ? <FaSun className="mr-2" /> : <FaMoon className="mr-2" />}
            {darkMode ? t('light-mode') : t('dark-mode')}
          </button>
        </div>

        {/* Table of Contents Modal */}
        {showTOC && (
          <div className={`absolute top-0 left-0 right-0 bottom-0 ${darkMode ? 'bg-gray-900' : 'bg-white'} bg-opacity-95 z-20 p-6 flex flex-col items-center rounded-lg shadow-lg overflow-y-auto`}>
            <h2 className={`text-3xl font-bold ${darkMode ? 'text-white' : 'text-indigo-800'} mb-4`}>{t('table-of-contents')}</h2>
            <ul className="w-full max-w-md text-left space-y-2">
              {Phrases.map((phrase, index) => (
                <li key={index}>
                  <button
                    onClick={() => {
                      handlePageChange(index);
                      toggleTOC();
                    }}
                    className={`text-lg ${darkMode ? 'text-gray-300' : 'text-indigo-700'} hover:${darkMode ? 'text-gray-200' : 'text-indigo-500'} focus:outline-none transition`}
                  >
                    {t('page')} {index + 1}: {phrase.substring(0, 30)}...
                  </button>
                </li>
              ))}
            </ul>
            <button
              onClick={toggleTOC}
              className={`mt-6 px-4 py-2 rounded ${darkMode ? 'bg-gray-700 text-white' : 'bg-indigo-600 text-white'} hover:bg-opacity-90 transition`}
            >
              {t('close')}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

'use client';
import { useState } from 'react';
import { FaArrowLeft, FaArrowRight } from 'react-icons/fa';
import { usePathname } from 'next/navigation';
import arPhrases from '@/data/ar-phrases.json';
import enPhrases from '@/data/en-phrases.json';
import esPhrases from '@/data/es-phrases.json';
import hiPhrases from '@/data/hi-phrases.json';
import jaPhrases from '@/data/ja-phrases.json';
import { useTranslate } from '@tolgee/react';


export default function Book() {
  const path = usePathname();
    // const t = await getTranslate();
    const { t } = useTranslate();


  let Phrases: string[] = enPhrases;
  
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

  const resultPhrases = Phrases;
  const [currentPage, setCurrentPage] = useState(0);
  const [isTransitioning, setIsTransitioning] = useState(false);

  const handlePrevPage = () => {
    if (currentPage > 0) {
      setIsTransitioning(true);
      setTimeout(() => {
        setCurrentPage(currentPage - 2);
        setIsTransitioning(false);
      }, 300);
    }
  };

  const handleNextPage = () => {
    if (currentPage < resultPhrases.length - 1) {
      setIsTransitioning(true);
      setTimeout(() => {
        setCurrentPage(currentPage + 2);
        setIsTransitioning(false);
      }, 300);
    }
  };

  return (
    <div className=" bg-gradient-to-br from-purple-100 to-blue-100 flex items-center justify-center p-4 pt-10 max-lg:p-2 mx-lg:pt-2 max-xl:pt-2">
      <div className="w-full max-w-6xl perspective-[2000px]">
        <div className="relative transform-gpu">
          {/* Book Title */}
          <h1 className="text-4xl max-md:text-2xl font-bold text-center mb-6 p-4 max-lg:mb-4 max-lg:p-3 text-transparent bg-clip-text bg-gradient-to-r from-purple-600 to-blue-600">
            {t('book-title')}  
          </h1>

          {/* Book Container */}
          <div className={`
            relative
            w-full
            h-96
            aspect-[2/1.4]
            transform-gpu
            transition-all
            duration-500
            ${isTransitioning ? 'rotate-y-180 scale-95' : 'rotate-y-0 scale-100'}
            preserve-3d
            group
          `}>
            {/* Book Spine */}
            <div className="absolute left-1/2 top-0 w-8 h-96 bg-gradient-to-r from-purple-800 to-blue-800 transform -translate-x-1/2 rotate-y-90 origin-left shadow-xl"></div>

            {/* Left Page */}
            <div className="absolute inset-0 w-1/2 bg-white h-96 rounded-l-2xl shadow-2xl p-8 backface-hidden">
              <div className="h-full flex flex-col justify-between">
                <div className="flex-1 flex items-center justify-center">
                  <p className="text-xl md:text-2xl text-gray-800 text-center leading-relaxed">
                    {currentPage > 0 ? resultPhrases[currentPage - 1] : ''}
                  </p>
                </div>
                <div className="text-gray-500 text-right">
                 {t('page')} {currentPage > 0 ? currentPage : ''}
                </div>
              </div>
              {/* Page fold effect */}
              <div className="absolute right-0 top-0 w-16 h-full bg-gradient-to-l from-gray-200/50 to-transparent"></div>
            </div>

            {/* Right Page */}
            <div className="absolute inset-0 w-1/2 h-96 left-1/2 bg-white rounded-r-2xl shadow-2xl p-8 backface-hidden">
              <div className="h-full flex flex-col justify-between">
                <div className="flex-1 flex items-center justify-center">
                  <p className="text-xl md:text-2xl text-gray-800 text-center leading-relaxed">
                    {resultPhrases[currentPage]}
                  </p>
                </div>
                <div className="text-gray-500">
                {t('page')} {currentPage + 1}
                </div>
              </div>
              {/* Page fold effect */}
              <div className="absolute left-0 top-0 w-16 h-full bg-gradient-to-r from-gray-200/50 to-transparent"></div>
            </div>

            {/* Book cover shadow */}
            <div className="absolute inset-0 rounded-2xl shadow-[0_0_50px_rgba(0,0,0,0.3)] pointer-events-none"></div>
          </div>

          {/* Navigation Buttons */}
          <div className="m-12 max-lg:m-10 max-xl:mt-8 max-xl:mb-4 max-2xl:m-24 flex justify-center items-center gap-8">
            <button
              onClick={handlePrevPage}
              disabled={currentPage === 0}
              className={`
                flex items-center gap-2
                px-8 py-4
                bg-gradient-to-r from-purple-600 to-blue-600
                text-white font-semibold
                rounded-xl
                transform-gpu
                transition-all duration-300
                hover:translate-y-[-4px]
                hover:shadow-lg
                hover:scale-105
                active:translate-y-0
                disabled:opacity-50
                disabled:cursor-not-allowed
                disabled:hover:translate-y-0
                disabled:hover:scale-100
              `}
            >
              <FaArrowLeft className="text-lg" />
              {t('previous')}
            </button>

            <button
              onClick={handleNextPage}
              disabled={currentPage === resultPhrases.length - 1}
              className={`
                flex items-center gap-2
                px-8 py-4
                bg-gradient-to-r from-purple-600 to-blue-600
                text-white font-semibold
                rounded-xl
                transform-gpu
                transition-all duration-300
                hover:translate-y-[-4px]
                hover:shadow-lg
                hover:scale-105
                active:translate-y-0
                disabled:opacity-50
                disabled:cursor-not-allowed
                disabled:hover:translate-y-0
                disabled:hover:scale-100
              `}
            >
              {t('next')}
              <FaArrowRight className="text-lg" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
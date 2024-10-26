'use client';
import React, { useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Sparkles, Share2, RefreshCw } from 'lucide-react';

const JokeCard = () => {
  const { t } = useTranslate();
  const [joke, setJoke] = useState('Loading...');
  const [isLoading, setIsLoading] = useState(true);
  const [isAnimating, setIsAnimating] = useState(false);

  const fetchJoke = async () => {
    setIsLoading(true);
    setIsAnimating(true);
    try {
      const response = await fetch('https://official-joke-api.appspot.com/jokes/random');
      const data = await response.json();
      setJoke(`${data.setup} - ${data.punchline}`);
    } catch (error) {
      setJoke('Failed to fetch joke. Try again!');
    }
    setIsLoading(false);
    setTimeout(() => setIsAnimating(false), 500); // Adjust timeout for animation duration
  };

  useEffect(() => {
    fetchJoke();
  }, []);

  const handleShare = () => {
    const jokeText = joke;
    const twitterUrl = `https://twitter.com/intent/tweet?text=${encodeURIComponent(jokeText)}`;
    window.open(twitterUrl, '_blank');
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-purple-100 to-pink-100 flex items-center justify-center p-4">
      <div className="w-full max-w-lg">
        {/* Main Card */}
        <div className={`bg-white rounded-3xl shadow-2xl transform transition-all duration-500 
          ${isAnimating ? 'scale-95 opacity-0' : 'scale-100 opacity-100'} hover:shadow-xl`}>

          {/* Header Section */}
          <div className="px-8 pt-8 pb-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="w-6 h-6 text-purple-600 animate-pulse" />
                <h2 className="text-3xl font-bold bg-gradient-to-r from-purple-600 to-pink-600 bg-clip-text text-transparent">
                  {t('joke-title')}
                </h2>
              </div>
              <div className="h-8 w-8 rounded-full bg-purple-200 flex items-center justify-center shadow-md">
                <span className="text-purple-600 text-sm font-medium">😄</span>
              </div>
            </div>
          </div>

          {/* Joke Content */}
          <div className="px-8 py-6">
            <div className={`bg-gradient-to-r from-purple-50 to-pink-50 rounded-2xl p-6 
              transition-all duration-500 ${isLoading ? 'opacity-50' : 'opacity-100'} 
              transform transition-transform duration-500 
              ${isAnimating ? 'scale-100' : 'scale-110'}`}>
              <p className="text-gray-800 text-lg leading-relaxed font-semibold">
                {joke}
              </p>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="p-8 pt-4 grid grid-cols-2 gap-4">
            <button
              onClick={fetchJoke}
              className="flex items-center justify-center gap-2 px-6 py-3 bg-purple-500 
                hover:bg-purple-600 text-white rounded-xl transition-colors duration-200
                font-semibold transform hover:scale-105"
            >
              <RefreshCw className="w-4 h-4 animate-spin" />
              {t('new-button')}
            </button>
            <button
              onClick={handleShare}
              className="flex items-center justify-center gap-2 px-6 py-3 bg-pink-500 
                hover:bg-pink-600 text-white rounded-xl transition-colors duration-200
                font-semibold transform hover:scale-105"
            >
              <Share2 className="w-4 h-4" />
              {t('share-button')}
            </button>
          </div>
        </div>

        {/* Footer Text */}
        <div className="mt-6 text-center">
          <p className="text-gray-500 text-sm font-medium">
            Click "New Joke" for another laugh!
          </p>
        </div>
      </div>
    </div>
  );
};

export default JokeCard;

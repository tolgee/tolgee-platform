'use client'
import React, { useEffect, useState, useRef } from 'react';
import { useTranslate } from '@tolgee/react';

const JokeCard: React.FC = () => {
  const { t } = useTranslate();
  const [joke, setJoke] = useState<string>('Loading...');
  const [isLoading, setIsLoading] = useState(true);
  const [isAnimating, setIsAnimating] = useState(false);
  const jokeCardRef = useRef<HTMLDivElement>(null);

  const fetchJoke = async () => {
    setIsLoading(true);
    setIsAnimating(true);
    const response = await fetch('https://official-joke-api.appspot.com/jokes/random');
    const data = await response.json();
    setJoke(`${data.setup} - ${data.punchline}`);
    setIsLoading(false);
    setTimeout(() => setIsAnimating(false), 500);
  };

  useEffect(() => {
    fetchJoke();
  }, []);

  const handleShare = () => {
    const jokeText = `${joke}`;
    const twitterUrl = `https://twitter.com/intent/tweet?text=${encodeURIComponent(jokeText)}`;
    window.open(twitterUrl, '_blank');
  };


  return (
    <div className="fixed inset-0 bg-black flex  flex-col items-center justify-center p-4 overflow-hidden ">
      {/* Animated Cyber Background */}
      <div className="absolute inset-0">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_center,_var(--tw-gradient-stops))] from-blue-900 via-black to-black" />
        <div className="absolute inset-0 opacity-20" />
        <div className="absolute inset-0">
          <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-gradient-to-r from-blue-500/20 to-cyan-500/20 rounded-full blur-3xl animate-pulse" />
          <div className="absolute top-1/3 left-1/3 w-[600px] h-[600px] bg-gradient-to-r from-violet-500/20 to-purple-500/20 rounded-full blur-3xl animate-pulse delay-100" />
          <div className="absolute bottom-1/3 right-1/3 w-[400px] h-[400px] bg-gradient-to-r from-fuchsia-500/20 to-pink-500/20 rounded-full blur-3xl animate-pulse delay-200" />
        </div>
      </div>

      {/* Main Content Container */}
      <div className="relative w-full max-w-4xl mx-auto perspective-1000">
        {/* Joke Card */}
        <div
          ref={jokeCardRef}
          className={`relative bg-gradient-to-br from-gray-900/90 to-gray-900/50 backdrop-blur-2xl border border-white/10 rounded-3xl transform transition-all duration-700 hover:rotate-x-12 group ${
            isAnimating ? 'scale-95 opacity-0' : 'scale-100 opacity-100'
          }`}
        >
          {/* Neon Border Effect */}
          <div className="absolute -inset-[1px] bg-gradient-to-r from-cyan-500 via-blue-500 to-purple-500 rounded-3xl opacity-0 group-hover:opacity-100 blur-sm transition-opacity duration-500" />
          
          {/* Content Container */}
          <div className="relative p-8 rounded-3xl bg-gray-900/90">
            {/* Header Section */}
            <div className="flex flex-col items-center space-y-6">
              <div className="relative">
                <h2 className="text-5xl font-bold bg-gradient-to-r from-cyan-400 via-blue-400 to-purple-400 bg-clip-text text-transparent pb-2">
                {t('joke-title')}
                </h2>
                <div className="absolute -inset-1 bg-gradient-to-r from-cyan-500 via-blue-500 to-purple-500 opacity-30 blur-lg -z-10" />
              </div>

              {/* Animated Loading Indicator */}
              <div className="flex justify-center space-x-3">
                <div className="w-4 h-4 rounded-full bg-cyan-500 animate-[bounce_1s_infinite_0ms]" />
                <div className="w-4 h-4 rounded-full bg-blue-500 animate-[bounce_1s_infinite_200ms]" />
                <div className="w-4 h-4 rounded-full bg-purple-500 animate-[bounce_1s_infinite_400ms]" />
              </div>

              {/* Decorative Line */}
              <div className="w-full h-px bg-gradient-to-r from-transparent via-white/20 to-transparent" />
            </div>

            {/* Joke Text */}
            <div className="mt-8 p-6 bg-white/5 rounded-2xl backdrop-blur-sm border border-white/10">
              <p className={`text-2xl text-gray-100 text-center leading-relaxed transition-all duration-500 ${
                isLoading ? 'opacity-50 blur-sm' : 'opacity-100 blur-0'
              }`}>
                {joke}
              </p>
            </div>

            {/* Interactive Elements */}
            <div className="absolute -bottom-3 left-1/2 -translate-x-1/2 w-1/2 h-6 bg-gradient-to-r from-transparent via-blue-500/20 to-transparent blur-xl" />
          </div>
        </div>

        {/* Action Buttons */}
        <div className="flex flex-col sm:flex-row gap-4 mt-12 w-full">
          {/* New Joke Button */}
          <button
            onClick={fetchJoke}
            className="group relative flex-1 overflow-hidden rounded-2xl transition-all duration-300 hover:scale-105 hover:rotate-x-12"
          >
            <div className="absolute inset-0 bg-gradient-to-r from-cyan-500 to-blue-500 animate-gradient-x" />
            <div className="absolute inset-[1px] bg-gray-900 rounded-2xl" />
            <div className="relative px-6 py-4 flex items-center justify-center">
              <div className="absolute inset-0 bg-gradient-to-r from-cyan-500/20 to-blue-500/20 opacity-0 group-hover:opacity-100 transition-opacity" />
              <span className="font-bold text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-blue-400">
              {t('new-button')}
              </span>
            </div>
          </button>

          {/* Share Button */}
          <button
            onClick={handleShare}
            className="group relative flex-1 overflow-hidden rounded-2xl transition-all duration-300 hover:scale-105 hover:rotate-x-12"
          >
            <div className="absolute inset-0 bg-gradient-to-r from-blue-500 to-violet-500 animate-gradient-x" />
            <div className="absolute inset-[1px] bg-gray-900 rounded-2xl" />
            <div className="relative px-6 py-4 flex items-center justify-center">
              <div className="absolute inset-0 bg-gradient-to-r from-blue-500/20 to-violet-500/20 opacity-0 group-hover:opacity-100 transition-opacity" />
              <span className="font-bold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-violet-400">
              {t('share-button')}
              </span>
            </div>
          </button>

        </div>
      </div>
    </div>
  );
};

export default JokeCard;
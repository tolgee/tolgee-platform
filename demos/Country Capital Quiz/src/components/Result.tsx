import React from 'react';
import { useTranslate } from '@tolgee/react';

interface ResultInterface {
  score: number;
  restart: () => void;
}

const Result: React.FC<ResultInterface> = ({ score, restart }) => {
  const { t } = useTranslate();

  return (
    <div className="flex flex-col items-center justify-center w-full max-w-lg bg-gray-800 p-10 rounded-xl shadow-lg">
      <p className="text-3xl font-semibold mb-6 text-white bg-gradient-to-r from-purple-600 to-blue-600 p-4 rounded-lg">
        {t('game-over')}
      </p>
      
      <p className="text-2xl text-white mb-4">
        {t('your-score')} {' '}
        <span className={`font-bold ${score >= 7 ? 'text-green-400' : 'text-red-400'}`}>
          {score}
        </span>
      </p>

      <p
        data-testid="outcome"
        className={`text-2xl font-bold mb-6 ${score >= 7 ? 'text-green-500' : 'text-red-500'}`}
      >
        {score >= 7 ? t('you-win') : t('you-lose')}
      </p>

      <button
        className="bg-indigo-800 text-white py-3 px-6 rounded-lg hover:bg-indigo-600 transition duration-200 transform hover:scale-105 shadow-md"
        onClick={restart}
      >
        {t('restart-quiz')}
      </button>
    </div>
  );
};

export default Result;

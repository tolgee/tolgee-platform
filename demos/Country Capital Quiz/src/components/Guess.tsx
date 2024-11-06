import React from 'react';
import { useTranslate } from '@tolgee/react';

interface GuessInterface {
  capital: string;
  handleAnswer: (capital: string) => void;
  disableGuess: boolean;
}

const Guess: React.FC<GuessInterface> = ({
  capital,
  handleAnswer,
  disableGuess,
}) => {
  const { t } = useTranslate();

  return (
    <button className='option' disabled={disableGuess} onClick={() => handleAnswer(capital)}>
      {capital}
    </button>
  );
};

export default Guess;

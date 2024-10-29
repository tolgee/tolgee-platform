import React from 'react';
// import { useTranslate } from '@tolgee/react';

interface GuessInterface {
  country: string;
  handleAnswer: (country: string) => void;
  disableGuess: boolean;
}

const Guess: React.FC<GuessInterface> = ({
  country,
  handleAnswer,
  disableGuess,
}) => {
  // const { t } = useTranslate();

  return (
    <button className='option' disabled={disableGuess} onClick={() => handleAnswer(country)}>
      {country}
    </button>
  );
};

export default Guess;

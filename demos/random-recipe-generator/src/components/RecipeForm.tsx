'use client';
import React, { useState, FormEvent, useEffect } from 'react';
import { useTranslate } from '@tolgee/react';

// Define the props interface
interface RecipeFormProps {
  addRecipe: (input: string) => Promise<void>;
  isrunnig: boolean;  // renamed to isRunning for clarity
}

const RecipeForm: React.FC<RecipeFormProps> = ({ addRecipe, isrunnig }) => {
  const { t } = useTranslate();
  const [input, setInput] = useState<string>('');
  const [isFocused, setIsFocused] = useState(false);

  useEffect(() => {
    console.log('isRunning changed:', isrunnig);
  }, [isrunnig]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;
    console.log("here"+input)
    try {
      setIsFocused(false);  // Remove focus when form is submitted
      await addRecipe(input); // Ensure the addRecipe call awaits
      setInput(''); // Clear input after adding the recipe
    } catch (error) {
      console.error('Error generating recipe:', error);
    }
  };
  const handleRandombutton = async (e: FormEvent) => {
    e.preventDefault();

    try {
      setIsFocused(false);  // Remove focus when form is submitted
      
    console.log("here random")
      await addRecipe("Random"); // Ensure the addRecipe call awaits
      setInput(''); // Clear input after adding the recipe
    } catch (error) {
      console.error('Error generating recipe:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mt-12 flex justify-center">
      <div
        className={`relative transition-all duration-300 ease-in-out ${isFocused
            ? 'w-[95%] sm:w-[60%] md:w-[60%] lg:w-[50%] xl:w-[40%]'
            : 'w-[95%] sm:w-[50%] md:w-[40%] lg:w-[30%] xl:w-[24%]'
          }`}
      >

        <div className="flex gap-3 items-center bg-[#171d2f] py-2 border-[1px] border-[#2828b5] rounded-full shadow-md overflow-hidden transition-all duration-300 ease-in-out px-4 mx-auto">
          <button type='button'
            className={`flex items-center justify-center hover:bg-[#0f131f] rounded-xl px-2 py-2`} onClick={handleRandombutton}
          >
            <img src="/img/shuffle.svg" alt="Submit" className="min-w-[24px] min-h-[24px]" />
          </button>
          <input
            type="text"
            placeholder={t('input-text')}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            className={`flex-grow py-3 px-4 border border-transparent bg-[#0f131f] text-white text-left text-md outline-none rounded-full transition-all duration-300 ease-in-out`}
            disabled={isrunnig}  // Disable input while loading
          />

          {isrunnig ? (
            <div className="flex items-center justify-center rounded-xl px-2 py-2">
              <div className="animate-spin rounded-full h-8 w-8 border-4 border-t-4 border-b-4 border-red-500 border-t-transparent border-b-transparent"></div>
            </div>

          ) : (
            <button
              type="submit"
              className={`flex items-center justify-center hover:bg-[#0f131f] rounded-xl px-2 py-2 max-sm:hidden`}
            >
              <img src="/img/submit.svg" alt="Submit" className="min-w-[24px] min-h-[24px]" />
            </button>
          )}
        </div>
      </div>
    </form>
  );
};

export default RecipeForm;

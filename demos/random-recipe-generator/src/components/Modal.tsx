import React from "react";

import { useTranslate } from '@tolgee/react';

// Define types for props
interface ModalProps {
  recipe: {
    name: string;
    ingredients: string[];
    instructions: string[];
    tips?: string | string[];
  };
  onClose: () => void;
}

const Modal: React.FC<ModalProps> = ({ recipe, onClose }) => {
  const { t } = useTranslate();
  return (
<div className="fixed inset-0 bg-gray-800 bg-opacity-75 flex items-center justify-center z-50 px-4 ">
  <div className="bg-[#252c42] border border-gray-800 shadow-xl rounded-xl p-6 w-full max-w-3xl md:w-[80%] lg:w-[50%] max-h-[90vh] overflow-y-auto transition-transform duration-300">
    <h2 className="text-2xl font-bold mb-4 text-white text-center">{recipe.name}</h2>
    <p className="mb-2 text-gray-200">
      <strong>{t('ingredients-title')} :</strong><br /> {recipe.ingredients.join(", ")}
    </p>
    <br />
    <p className="mb-2 text-gray-200">
      <strong>{t('instructions-title')} :</strong><br /> {recipe.instructions.join(", ")}
    </p>
    <br />
    {recipe.tips && (
      <p className="mb-2 text-gray-200">
        <strong>{t('tips-title')} :</strong><br /> {recipe.tips}
      </p>
    )}
    <div>
      
    </div>
    <button
      onClick={onClose}
      className="mt-4 mx-auto block bg-blue-500  text-white font-medium px-4 py-2 rounded-lg hover:bg-blue-600 hover:shadow-lg transition-all duration-300"
    >
      {t('close-button')}
    </button>
  </div>
</div>


  );
};

export default Modal;

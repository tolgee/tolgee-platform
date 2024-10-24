import { useTolgee } from '@tolgee/react';

export const LangSelector = () => {
  const tolgee = useTolgee(['language']);

  return (
    <select
      onChange={(e) => tolgee.changeLanguage(e.target.value)}
          value={tolgee.getLanguage()}
          className="block w-48 px-4 py-2 mt-1 bg-gray-800 text-white border border-gray-600 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 mb-2"
    >
      <option value="en">English</option>
      <option value="ar">Arabic</option>
      <option value="hi">Hindi</option>
      <option value="es">Spanish</option>
          
    </select>
  );
};

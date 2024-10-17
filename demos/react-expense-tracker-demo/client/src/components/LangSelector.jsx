import React, { useEffect, useState } from 'react';
import { useTolgee } from "@tolgee/react";

export const LangSelector = () => {
    const tolgee = useTolgee(['language']);
    const [language, setLanguage] = useState(localStorage.getItem('language') || tolgee.getLanguage());

    useEffect(() => {
        tolgee.changeLanguage(language);
    }, [language, tolgee]);

    const handleChange = (e) => {
        const newLanguage = e.target.value;
        setLanguage(newLanguage);
        localStorage.setItem('language', newLanguage);
    };

    return (
        <div className="flex justify-end p-4"> {/* Right-align the selector */}
            <div className="inline-flex flex-col">
                <label htmlFor="language-select" className="mb-2 text-sm font-medium text-gray-700">
                    🌍 {tolgee.t('choose_language', 'Choose Language')}
                </label>
                <select
                    id="language-select"
                    className="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full p-2.5"
                    onChange={handleChange}
                    value={language}
                >
                    <option value="en">🇬🇧 English</option>
                    <option value="hi-IN">🇮🇳 हिन्दी (भारत)</option>
                    <option value="mr-IN">🇮🇳 मराठी (भारत)</option>
                    <option value="it-IT">🇮🇹 italiano (Italia)</option>
                    <option value="ru-RU">🇷🇺 русский (Россия)</option>
                </select>
            </div>
        </div>
    );
};
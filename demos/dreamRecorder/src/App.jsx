import { useState, useEffect } from "react";
import { Tolgee, DevTools, FormatSimple, T, useTranslate } from "@tolgee/react";
import DatePicker from "react-datepicker";
import "react-datepicker/dist/react-datepicker.css";
import "./App.css"; // Ensure this imports your CSS file
import { LanguageSelect } from "./LanguageSelect";
import enTranslations from "../public/locales/en.json";
import esTranslations from "../public/locales/es.json";
import frTranslations from "../public/locales/fr.json";
import hiTranslations from "../public/locales/hi.json";

// Initialize Tolgee
export const tolgee = Tolgee()
  .use(DevTools())
  .use(FormatSimple())
  .init({
    staticData: {
      en: enTranslations, // Static translation data for English
      es: esTranslations, // Static translation data for Spanish
      hi: hiTranslations,
      fr: frTranslations,
    },
    defaultLanguage: "en",
    fallbackLanguage: "en",
    preloadFallback: true,
  });

console.log("Tolgee Initialized:", tolgee);

function App() {
  const [dreams, setDreams] = useState(() => {
    const savedDreams = localStorage.getItem("dreams");
    return savedDreams ? JSON.parse(savedDreams) : [];
  });
  const { t } = useTranslate();

  useEffect(() => {
    localStorage.setItem("dreams", JSON.stringify(dreams));
  }, [dreams]);

  const addDream = (newDream, newDate) => {
    const newDreamObj = {
      id: Date.now(),
      dream: newDream,
      date: newDate,
    };
    setDreams([...dreams, newDreamObj]);
  };

  return (
    <div className="App min-h-screen flex flex-col items-center justify-center p-8 relative">
      <LanguageSelect />
      <h1 className="text-5xl font-extrabold text-gray-800 mb-8 z-10">
        <T>My Dreams</T>
      </h1>
      <ul className="space-y-6 w-full max-w-md z-10">
        {dreams.map((dream) => (
          <li
            key={dream.id}
            className="bg-white p-6 rounded-lg shadow-lg transition-shadow duration-200 hover:shadow-2xl"
          >
            {/* Attempt to translate the dream text */}
            <p className="text-gray-800 text-lg font-medium">
              {t(dream.dream) || dream.dream}{" "}
              {/* Use the dream text or fall back to the original */}
            </p>
            <span className="text-gray-600 text-sm italic">{dream.date}</span>
          </li>
        ))}
      </ul>
      <AddDream onAddDream={addDream} />
    </div>
  );
}

function AddDream({ onAddDream }) {
  const [dream, setDream] = useState("");
  const [date, setDate] = useState(new Date());
  const { t } = useTranslate(); // Tolgee hook for translations

  const handleAddDream = () => {
    if (dream && date) {
      // Translate the dream before adding
      const translatedDream = t(dream) || dream; // Translate if a key exists
      onAddDream(translatedDream, date.toISOString().split("T")[0]);
      setDream("");
      setDate(new Date());
    } else {
      alert(t("enter_dream_alert", "Please enter a dream and select a date."));
    }
  };

  return (
    <div className="mt-10 w-full max-w-md z-10 bg-white p-6 rounded-lg shadow-md">
      <input
        type="text"
        placeholder={t("✨ Enter Your Dream ✨")}
        value={dream}
        onChange={(e) => setDream(e.target.value)}
        className="border border-gray-300 p-4 rounded-lg w-full mb-4 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition duration-200 placeholder-gray-400"
      />
      <DatePicker
        selected={date}
        onChange={(date) => setDate(date)}
        className="border border-gray-300 p-4 rounded-lg w-full mb-4 shadow-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 transition duration-200"
      />
      <button
        onClick={handleAddDream}
        className="bg-gradient-to-r from-indigo-500 to-pink-500 text-white px-6 py-3 rounded-lg w-full transition-colors duration-200 hover:from-indigo-600 hover:to-pink-600 shadow-lg focus:outline-none focus:ring-2 focus:ring-blue-400"
      >
        {t("Add Dream")}
      </button>
    </div>
  );
}
export default App;

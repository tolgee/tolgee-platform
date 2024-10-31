import { useContext, createContext } from "react";
import useLocalStorage from "../hooks/useLocalStorage";
import { Tolgee, DevTools, TolgeeProvider, FormatSimple } from "@tolgee/react";
import Loader from "../components/loader/Loader";

const LanguageContext = createContext();

export const useLanguage = () => useContext(LanguageContext);

function LanguageProvider({ children }) {
  // setting current language in the local storage
  const [currentLanguage, setCurrentLanguage] = useLocalStorage(
    "language",
    "en"
  );

  const tolgee = Tolgee()
    .use(DevTools())
    .use(FormatSimple())
    .init({
      defaultLanguage: currentLanguage,
      availableLanguages: ["en", "fr", "de", "hi", "es"],
      // for development
      // apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
      // apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,

      // for production
      staticData: {
        en: () => import ("../i18n/en.json"),
        fr: () => import ("../i18n/fr.json"),
        de: () => import ("../i18n/de.json"),
        es: () => import ("../i18n/es.json"),
        hi: () => import ("../i18n/hi.json"),
      },
    });

  return (
    <LanguageContext.Provider value={[currentLanguage, setCurrentLanguage]}>
      <TolgeeProvider tolgee={tolgee} fallback={<Loader />}>
        {children}
      </TolgeeProvider>
    </LanguageContext.Provider>
  );
}

export default LanguageProvider;

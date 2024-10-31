import { useTolgee } from "@tolgee/react";
import Select from "react-select";
import { languageOptions } from "../constants/constants";
import useLocalStorage from "../hooks/useLocalStorage";

const LanguageSelector = () => {
  const tolgee = useTolgee(["language"]);
  const [currentLanguage, setCurrentLanguage] = useLocalStorage(
    "language",
    "en"
  );
  function handleLanguageSelection(e) {
    setCurrentLanguage(e.value);
    tolgee.changeLanguage(e.value);
  }
  return (
    <Select
      classNames={{
        control: () => "text-sm w-36",
      }}
      onChange={handleLanguageSelection}
      value={languageOptions.find(
        (language) => language.value === currentLanguage
      )}
      options={languageOptions}
    />
    // <select
    //   onChange={(e) => tolgee.changeLanguage(e.target.value)}
    //   value={tolgee.getLanguage()}
    // >
    //   <option value="en">🇬🇧 English</option>
    //   <option value="fr">🇨🇿 français</option>
    //   <option value="de">🇩🇪 Deutsch</option>
    //   <option value="hi">🇮🇳 हिन्दी</option>
    //   <option value="es">🇪🇸 español</option>
    // </select>
  );
};

export default LanguageSelector;

import React from "react";
import { tolgee } from "../App";

const LanguageSwitcher: React.FC = () => {
  const handleChangeLanguage = (lang: string) => {
    tolgee.changeLanguage(lang).then(() => tolgee.run());
  };

  return (
    <div className="flex gap-4 border-2 border-blue-500">
      <button onClick={() => handleChangeLanguage("en")}>English</button>
      <button onClick={() => handleChangeLanguage("cs")}>Czech</button>
      <button onClick={() => handleChangeLanguage("fr")}>French</button>
      <button onClick={() => handleChangeLanguage("de")}>German</button>
    </div>
  );
};

export default LanguageSwitcher;

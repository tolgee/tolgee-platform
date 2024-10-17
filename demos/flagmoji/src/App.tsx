import { useState } from "react";
import { T, useTranslate } from "@tolgee/react";
import LanguageSelect from "./LanguageSelect";


export default function App() {
  const [country, setCountry] = useState("");
  const [flag, setFlag] = useState("");
  const { t } = useTranslate();

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCountry(e.target.value);
  };

  const fetchFlag = async () => {
    if (country.trim() === "") return;
    try {
      const response = await fetch(
        `https://restcountries.com/v3.1/name/${country}`
      );
      const data = await response.json();
      if (data && data[0] && data[0].flags) {
        setFlag(data[0].flags.png);
      } else {
        setFlag("Country not found");
      }
    } catch (error) {
      setFlag("Error fetching data");
    }
  };

  return (
    <>
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100">
        <LanguageSelect />
        <div className="p-6 bg-white shadow-lg rounded-md w-full max-w-4xl text-center mt-4">
          <h1 className="font-bold text-3xl mb-4">
            <T keyName="app_title">
              Flagmoji | Country to Flag Emoji Converter
            </T>
          </h1>
          <T keyName="flag_emoji_short_text">
            Type a country name to see its flag emoji.
          </T>
          <div className="flex flex-col lg:flex-row lg:items-center lg:space-x-4">
            <div className="flex flex-col w-full lg:w-1/2 my-5 lg:mb-0">
              <input
                type="text"
                value={country}
                onChange={handleInputChange}
                placeholder={t("placeholder_country_name", "Enter country name")}
                className="border p-2 rounded-md mb-2 w-full"
              />
              <div className="flex items-start mt-3">
                <button
                  onClick={fetchFlag}
                  className="bg-slate-800 text-white py-1 px-4 rounded hover:bg-slate-900"
                >
                  <T keyName="button_fetch_flag">View</T>
                </button>
              </div>
            </div>
            {flag && (
              <div className="flex justify-center lg:w-1/2 mt-4 lg:mt-5">
                {flag.startsWith("http") ? (
                  <img src={flag} alt="Country flag" className="w-32 h-32" />
                ) : (
                  <p>{flag}</p>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </>
  );
}

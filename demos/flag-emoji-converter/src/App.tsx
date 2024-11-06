/* eslint-disable @typescript-eslint/no-var-requires */
import { Tolgee, TolgeeProvider, BackendFetch, DevTools } from "@tolgee/react";
import { FormatIcu } from "@tolgee/format-icu";
import FindCountry from "./components/findCountry.tsx";
import Navbar from "./components/Navbar.tsx";
import Description from "./components/Description.tsx";

const tolgee = Tolgee()
  .use(DevTools())
  .use(FormatIcu())
  .use(BackendFetch())
  .init({
    availableLanguages: ["en", "es", "hi", "zh", "de", "fr"],
    apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
    apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,
    projectId: import.meta.env.VITE_APP_TOLGEE_PROJECT_ID,
    fallbackLanguage: "en",
    defaultLanguage: "en",
  });

export const App = () => {
  return (
    <TolgeeProvider tolgee={tolgee} fallback="Loading...">
      <Navbar />
      <Description />
      <FindCountry />
    </TolgeeProvider>
  );
};

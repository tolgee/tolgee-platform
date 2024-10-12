/* eslint-disable @typescript-eslint/no-var-requires */
import { Tolgee, TolgeeProvider, BackendFetch, DevTools } from "@tolgee/react";
import { FormatIcu } from "@tolgee/format-icu";
import "../src/style.css";
import DishCard from "./components/DishCard";

const tolgee = Tolgee()
  .use(DevTools())
  .use(FormatIcu())
  .use(BackendFetch())
  .init({
    availableLanguages: ["en", "cs", "fr", "de"],
    apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
    apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,
    projectId: import.meta.env.VITE_APP_TOLGEE_PROJECT_ID,
    fallbackLanguage: "en",
    defaultLanguage: "en",
  });

export const App = () => {
  return (
    <TolgeeProvider tolgee={tolgee}>
      <DishCard />
    </TolgeeProvider>
  );
};

import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import "./index.css";
import { Tolgee, DevTools, TolgeeProvider, FormatSimple, BackendFetch } from "@tolgee/react";
import Loading from "./components/Loading.jsx";
const tolgee = Tolgee()
  .use(DevTools())
  .use(BackendFetch())
  .use(FormatSimple())
  .init({
    language: "en",

    // for development
    // apiUrl: process.env.VITE_APP_TOLGEE_API_URL,
    apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
    // apiKey: process.env.VITE_APP_TOLGEE_API_KEY,
    apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,

    // for production
    // staticData: {
    //   ...
    // }
  });

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <TolgeeProvider
      tolgee={tolgee}
      fallback={<Loading />} // loading fallback
    >
      <App />
    </TolgeeProvider>
  </StrictMode>
);

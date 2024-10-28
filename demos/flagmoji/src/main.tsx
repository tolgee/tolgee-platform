import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import {
  Tolgee,
  DevTools,
  TolgeeProvider,
  FormatSimple,
  BackendFetch,
} from "@tolgee/react";
import App from "./App.tsx";
import "./index.css";

const tolgee = Tolgee()
  .use(DevTools())
  .use(BackendFetch())
  .use(FormatSimple())
  .init({
    language: "en",

    // for development
    apiUrl: process.env.VITE_APP_TOLGEE_API_URL,
    apiKey: process.env.VITE_APP_TOLGEE_API_KEY,
  });

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <TolgeeProvider tolgee={tolgee} fallback="Loading...">
      <App />
    </TolgeeProvider>
  </StrictMode>
);

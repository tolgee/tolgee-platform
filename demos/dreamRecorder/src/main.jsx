import React from "react";
import App from "./App";
import { createRoot } from "react-dom/client";
import { TolgeeProvider } from "@tolgee/react";
import { StrictMode } from "react";
import { tolgee } from "./App"; // Ensure tolgee is imported correctly

createRoot(document.getElementById("root")).render(
  <TolgeeProvider tolgee={tolgee} fallback="Loading...">
    <StrictMode>
      <App />
    </StrictMode>
  </TolgeeProvider>
);

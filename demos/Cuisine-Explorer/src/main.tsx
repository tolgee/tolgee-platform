import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App.tsx";
import "./style.css";
import Navbar from "./components/Navbar.tsx";
import Footer from "./components/Footer.tsx";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <Navbar />
    <App />
    <Footer />
  </React.StrictMode>
);

import React from "react";
import "./loader.css";

function Loader() {
  return (
    <div className="w-screen h-screen bg-primaryBackground fixed top-0 left-0 z-100 grid place-items-center">
      <div className="loader"></div>
    </div>
  );
}

export default Loader;

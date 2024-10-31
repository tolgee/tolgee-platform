import React from "react";
import meditatingBoy from "../assets/meditating-boy.svg";
import { Link } from "react-router-dom";
import { useTranslate } from "@tolgee/react";

function PageNotFound() {
  const { t } = useTranslate();
  return (
    <div className="w-screen h-screen flex flex-col justify-center items-center gap-6 bg-primaryBackground text-primaryText">
      <div className="h-3/5">
        <img
          src={meditatingBoy}
          alt="illustration of meditating boy"
          className="h-full"
        />
      </div>
      <div className="flex flex-col gap-4 items-center">
        <h2 className="text-3xl font-bold font-heading">
          {t("404PageTitle", "Keep Calm!")}
        </h2>
        <span>
          {t("404PageText", "The page you are looking for is not available!")}
        </span>
        <Link to="/" className="btn primary-btn">
          {t("404PageButtonText", "Ease towards the Home Page")}
        </Link>
      </div>
    </div>
  );
}

export default PageNotFound;

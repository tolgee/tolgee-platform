import React from "react";
import { Link } from "react-router-dom";
import lotus from "../assets/lotus.png";
import { useTranslate } from "@tolgee/react";

function ExitMeditationModal({ setShowModal }) {
  const { t } = useTranslate();
  return (
    <div className="w-screen h-screen fixed z-20 top-0 left-0 bg-slate-50/15 backdrop-blur grid place-items-center p-4">
      <div className="bg-primaryBackground px-8 sm:px-12 py-8 rounded-md shadow-md flex flex-col gap-10 items-center">
        <h4 className="font-semibold text-xl font-heading text-center text-primaryText">
          {t(
            "meditationQuitMessage",
            "Are you sure you want to quit your meditation session?"
          )}
        </h4>
        <img src={lotus} alt="Lotus" className="w-20" />
        <div className="flex gap-8">
          <button
            className="primary-btn btn w-20"
            onClick={() => setShowModal(false)}
          >
            {t("no", "No")}
          </button>
          <Link to="/" className="secondary-btn btn w-20 text-center">
            {t("yes", "Yes")}
          </Link>
        </div>
      </div>
    </div>
  );
}

export default ExitMeditationModal;

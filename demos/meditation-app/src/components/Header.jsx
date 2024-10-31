import { useLocation, Link } from "react-router-dom";
import { useTranslate } from "@tolgee/react";
import useScroll from "../hooks/useScroll";
import LanguageSelector from "./LanguageSelector";

function Header() {
  const { t } = useTranslate();
  const { scrollY } = useScroll("main");
  const location = useLocation();
  const pathname = location.pathname;

  return (
    <header
      className={`flex gap-6 justify-between items-center px-4 md:px-8 py-4 bg-secondaryBackground fixed z-90 w-full h-header ${
        scrollY > 50 ? "shadow-md" : ""
      }`}
    >
      <Link to="/">
        <h1 className="text-2xl md:text-4xl font-heading text-logoColor font-bold flex gap-2">
          <span>{t("appName", "Zenith")}</span>
        </h1>
      </Link>
      <nav>
        <ul className="flex gap-6 md:gap-12 text-sm md:text-lg items-center">
          <li>
            <LanguageSelector />
          </li>
          <li>
            <Link
              to="/meditation/sound-select"
              className={`pb-1 ${
                pathname.startsWith("/meditation")
                  ? "border-b border-activeLink text-activeLink"
                  : "text-primaryText"
              }`}
            >
              {t("meditation", "MEDITATION")}
            </Link>
          </li>
        </ul>
      </nav>
    </header>
  );
}

export default Header;

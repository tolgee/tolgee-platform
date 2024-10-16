import { useTolgee, useTranslate } from "@tolgee/react";

export default function Navbar() {
	const tolgee = useTolgee(["language"]);
	const { t } = useTranslate();

	return (
		<nav className="navbar">
			<h2 className="navbar__title">🌍 {t("app-title")}</h2>
			<select
				name="selected-language"
				className="navbar__dropdown"
				aria-label="Select language of your choice..."
				onChange={(e) => tolgee.changeLanguage(e.target.value)}
				value={tolgee.getLanguage()}
				required
			>
				<option value="en" selected>
					{t("option-english")}
				</option>
				<option value="es">{t("option-spanish")}</option>
				<option value="hi">{t("option-hindi")}</option>
				<option value="zh">{t("option-chinese")}</option>
				<option value="de">{t("option-german")}</option>
				<option value="fr">{t("option-french")}</option>
			</select>
		</nav>
	);
}

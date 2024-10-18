import { useTranslate } from "@tolgee/react";

export default function Description() {
	const { t } = useTranslate();

	return <p>{t("app-description")}</p>;
}

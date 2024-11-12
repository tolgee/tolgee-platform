import { useTranslate } from "@tolgee/react";
import { useState } from "react";

import countries from "../assets/countries.json";
import "../styles/findCountry.style.css";
import { CountryTrie } from "../util/countryTrie";

const trie = new CountryTrie();
for (let [country, code] of Object.entries(countries)) {
	trie.insert(country, code);
}

export default function FindCountry() {
	const { t } = useTranslate();
	const [countryName, setCountryName] = useState("");

	return (
		<>
			<input
				type="search"
				name="search"
				placeholder={t("input-placeholder")}
				autoComplete="off"
				aria-label="Search"
				value={countryName}
				onChange={(e) => setCountryName(e.target.value)}
			/>
			<div className="flag-container">
				{trie.bestFind(countryName).map((code) => (
					<div key={code} className={`flag fib fi-${code}`} />
				))}
			</div>
		</>
	);
}

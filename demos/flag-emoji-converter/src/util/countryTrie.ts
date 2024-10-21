export class CountryTrie {
	children: Record<string, CountryTrie>;
	countryCode: string;
	isCountry: boolean;

	constructor() {
		this.children = {};
		this.countryCode = "";
		this.isCountry = false;
	}

	insert(name: string, code: string): void {
		let cur: CountryTrie = this;
		for (let alphabet of name.split("")) {
			if (!(alphabet in cur.children)) {
				cur.children[alphabet] = new CountryTrie();
			}
			cur = cur.children[alphabet];
		}

		cur.isCountry = true;
		cur.countryCode = code;
	}

	search(name: string): string {
		let cur: CountryTrie = this;
		name = name.toLowerCase();

		for (let alphabet of name.split("")) {
			if (alphabet in cur.children) {
				cur = cur.children[alphabet];
			} else {
				break;
			}
		}

		if (cur.isCountry) {
			return cur.countryCode;
		}
		return "";
	}

	bestFind(name: string) {
		let cur: CountryTrie = this;
		name = name.toLowerCase();

		if (name.length < 1) return [];

		for (let alphabet of name.split("")) {
			if (alphabet in cur.children) {
				cur = cur.children[alphabet];
			} else {
				return [];
			}
		}

		let result: string[] = [];

		function findRest(self: CountryTrie) {
			if (self.isCountry) {
				return result.push(self.countryCode);
			}
			for (let key in self.children) {
				findRest(self.children[key]);
			}
		}

		findRest(cur);
		return result;
	}
}

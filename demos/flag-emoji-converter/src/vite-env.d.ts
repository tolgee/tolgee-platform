/// <reference types="vite/client" />

declare module "*.json" {
	const value: { [country: string]: string };
	export default value;
}

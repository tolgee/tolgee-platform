declare module '*.svg' {
    const content: React.FunctionComponent<React.SVGAttributes<SVGElement>>;
    export default content;
}

declare module '*.woff2' {
    const content: any;
    export default content;
}

declare const environment: {
    polygloatApiKey: string;
    polygloatApiUrl: string;
    polygloatWithUI: string;
    sentryDsn: string,
    mode: "production" | "development",
    apiUrl: string,
};

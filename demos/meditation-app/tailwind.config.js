/** @type {import('tailwindcss').Config} */
const defaultTheme = require("tailwindcss/defaultTheme");

export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        primaryBackground: "#f6f3e7",
        secondaryBackground: "#b6643f",
        primaryText: "#001f3f",
        secondaryText: "#f6f3e7",
        themeLightColor: "#9dc5c1",
        themeColor: "#48b4a0",
        activeLink: "#f6f3e7",
        logoColor: "#48b4a0",
      },
      fontFamily: {
        primary: ['"Nunito"', ...defaultTheme.fontFamily.sans],
        heading: ['"Exo 2"', ...defaultTheme.fontFamily.sans],
        number: ['"Graduate"', ...defaultTheme.fontFamily.sans],
      },
      height: {
        header: "72px",
      },
      keyframes: {
        bounceSmooth: {
          "0%, 100%": {
            transform: "translateY(0)",
            animationTimingFunction: "ease-out",
          },
          "50%": {
            transform: "translateY(-10px)",
            animationTimingFunction: "ease-in",
          },
        },
      },
      animation: {
        bounceSmooth: "bounceSmooth 1.5s infinite",
      },
    },
  },
  plugins: [],
};

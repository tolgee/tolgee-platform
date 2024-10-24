/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Kumbh Sans', 'sans-serif'],
        serif: ['Roboto', 'serif'],
      },
    },
  },
  plugins: [],
}
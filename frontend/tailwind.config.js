/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        wow: {
          gold: "#f4c430",
          parchment: "#1a1411",
          frame: "#2a1f1a"
        }
      }
    }
  },
  plugins: []
};

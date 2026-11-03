/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        telecom: {
          navy: '#0F1E36',
          dark: '#0A1423',
          card: '#14243B',
          blue: '#1976D2',
          lightBlue: '#38BDF8',
          accent: '#0284C7',
          accorded: '#10B981',
          accordedDark: '#059669',
          refused: '#EF4444',
          refusedDark: '#DC2626',
          pending: '#F59E0B',
          pendingDark: '#D97706',
        }
      }
    },
  },
  plugins: [],
}

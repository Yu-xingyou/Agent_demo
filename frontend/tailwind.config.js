/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        teal: {
          700: '#0f766e',
          800: '#115e59',
          900: '#134e4a',
        },
        amber: {
          500: '#f59e0b',
        },
        warm: '#f9f8f6',
      },
      fontFamily: {
        sans: ['Noto Sans SC', 'system-ui', 'sans-serif'],
      },
      boxShadow: {
        card: '0 1px 3px rgba(0,0,0,0.06), 0 4px 12px rgba(0,0,0,0.05)',
        'card-hover': '0 8px 24px rgba(15,118,110,0.18)',
      },
      borderRadius: {
        card: '16px',
      },
    },
  },
  plugins: [],
}

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
        brand: {
          teal: '#0f766e',
          indigo: '#6366f1',
          purple: '#a855f7',
        },
      },
      fontFamily: {
        sans: ['Noto Sans SC', 'system-ui', 'sans-serif'],
      },
      backgroundImage: {
        'grad-primary': 'linear-gradient(135deg, #0f766e 0%, #6366f1 50%, #a855f7 100%)',
        'grad-sleep': 'linear-gradient(135deg, #4f46e5, #6366f1)',
        'grad-water': 'linear-gradient(135deg, #0891b2, #06b6d4)',
        'grad-exercise': 'linear-gradient(135deg, #ea580c, #f59e0b)',
        'grad-diet': 'linear-gradient(135deg, #0d9488, #14b8a6)',
        'grad-mood': 'linear-gradient(135deg, #db2777, #ec4899)',
        'grad-custom': 'linear-gradient(135deg, #7c3aed, #a855f7)',
        'grad-soft': 'linear-gradient(135deg, rgba(15,118,110,0.16), rgba(168,85,247,0.16))',
      },
      boxShadow: {
        card: '0 1px 3px rgba(0,0,0,0.06), 0 4px 12px rgba(0,0,0,0.05)',
        'card-hover': '0 8px 24px rgba(15,118,110,0.18)',
        glow: '0 10px 40px rgba(99,102,241,0.35)',
        'glow-purple': '0 10px 40px rgba(168,85,247,0.35)',
      },
      borderRadius: {
        card: '16px',
        'card-xl': '24px',
      },
      keyframes: {
        float: {
          '0%,100%': { transform: 'translateY(0) rotate(0deg)' },
          '50%': { transform: 'translateY(-14px) rotate(2deg)' },
        },
        rise: {
          from: { opacity: 0, transform: 'translateY(26px)' },
          to: { opacity: 1, transform: 'translateY(0)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '200% 0' },
          '100%': { backgroundPosition: '-200% 0' },
        },
      },
      animation: {
        float: 'float 8s ease-in-out infinite',
        'float-slow': 'float 14s ease-in-out infinite',
        rise: 'rise 0.7s cubic-bezier(0.22,1,0.36,1) both',
        shimmer: 'shimmer 3.5s linear infinite',
      },
    },
  },
  plugins: [],
}

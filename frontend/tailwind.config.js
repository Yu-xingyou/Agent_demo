/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          DEFAULT: '#667eea',
          soft: '#764ba2',
          deep: '#5a67d8',
        },
      },
      fontFamily: {
        sans: ['Noto Sans SC', 'system-ui', 'sans-serif'],
      },
      backgroundImage: {
        'grad-primary': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'grad-sleep': 'linear-gradient(135deg, #60a5fa, #6366f1)',
        'grad-water': 'linear-gradient(135deg, #22d3ee, #38bdf8)',
        'grad-exercise': 'linear-gradient(135deg, #fb923c, #f59e0b)',
        'grad-diet': 'linear-gradient(135deg, #34d399, #10b981)',
        'grad-mood': 'linear-gradient(135deg, #f472b6, #ec4899)',
        'grad-custom': 'linear-gradient(135deg, #a78bfa, #8b5cf6)',
        'grad-soft': 'linear-gradient(135deg, rgba(102,126,234,0.16), rgba(118,75,162,0.16))',
      },
      boxShadow: {
        card: '0 10px 34px rgba(31,38,89,0.08), 0 2px 8px rgba(31,38,89,0.04)',
        'card-hover': '0 18px 48px rgba(102,126,234,0.16), 0 4px 12px rgba(31,38,89,0.06)',
        glow: '0 12px 30px rgba(102,126,234,0.32)',
        'glow-purple': '0 12px 30px rgba(236,72,153,0.26)',
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

/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        teal: {
          700: '#6f8a82',
          800: '#5f7a72',
          900: '#4f6a62',
        },
        amber: {
          500: '#c39b7e',
        },
        warm: '#f5f4f2',
        brand: {
          teal: '#6f8a82',
          indigo: '#7e88a3',
          purple: '#8f8196',
        },
      },
      fontFamily: {
        sans: ['Noto Sans SC', 'system-ui', 'sans-serif'],
      },
      backgroundImage: {
        'grad-primary': 'linear-gradient(135deg, #6f8a82 0%, #7e88a3 50%, #8f8196 100%)',
        'grad-sleep': 'linear-gradient(135deg, #6f7a99, #7e88a3)',
        'grad-water': 'linear-gradient(135deg, #5f8a92, #6f97a0)',
        'grad-exercise': 'linear-gradient(135deg, #b08a6f, #c39b7e)',
        'grad-diet': 'linear-gradient(135deg, #6f9a8a, #7faa9a)',
        'grad-mood': 'linear-gradient(135deg, #a87f8e, #b88f9e)',
        'grad-custom': 'linear-gradient(135deg, #8a7fa0, #9a8fb0)',
        'grad-soft': 'linear-gradient(135deg, rgba(111,138,130,0.14), rgba(143,129,150,0.14))',
      },
      boxShadow: {
        card: '0 1px 3px rgba(0,0,0,0.05), 0 4px 12px rgba(0,0,0,0.04)',
        'card-hover': '0 8px 24px rgba(111,138,130,0.14)',
        glow: '0 10px 40px rgba(126,136,163,0.22)',
        'glow-purple': '0 10px 40px rgba(143,129,150,0.22)',
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

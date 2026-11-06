/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        navy: {
          900: '#10324E',
          800: '#143C5E',
          700: '#1E4B72',
        },
        teal: {
          brand: '#2D8A7A',
          hover: '#247466',
          light: '#E0F2EB',
          halo: '#D8EFE4',
        },
        peach: {
          brand: '#F49E78',
          accent: '#E99A7C',
          light: '#FCEFE9',
        },
        cream: {
          canvas: '#F7F5EE',
          card: '#FAF8F5',
          pill: '#F5F2EB',
        },
        charcoal: {
          DEFAULT: '#242826',
        },
      },
      fontFamily: {
        sans: ['Inter', 'Plus Jakarta Sans', 'system-ui', '-apple-system', 'sans-serif'],
        display: ['Plus Jakarta Sans', 'Inter', 'sans-serif'],
        serif: ['Georgia', 'Cambria', 'serif'],
      },
      boxShadow: {
        'vetalya': '0 24px 55px rgba(16, 50, 78, 0.08)',
        'vetalya-card': '0 12px 32px rgba(16, 50, 78, 0.06)',
      },
    },
  },
  plugins: [],
};

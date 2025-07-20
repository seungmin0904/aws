/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'media',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 2px)',
        sm: 'calc(var(--radius) - 4px)'
      },
      colors: {
        // ✅ 투명한 유리효과 배경용
        'glass-dark': 'rgba(0, 0, 0, 0.3)',
        'glass-light': 'rgba(255, 255, 255, 0.2)',

        // ✅ 네온 강조용
        'soft-white': 'rgba(255, 255, 255, 0.8)',
        'soft-black': 'rgba(0, 0, 0, 0.8)',

        // ✅ 기존 구성 색상 유지 (수정 없음)
        card: {
          DEFAULT: '#1e1e1e',
          foreground: '#ffffff'
        },
        popover: {
          DEFAULT: '#1e1e1e',
          foreground: '#ffffff'
        },
        primary: {
          DEFAULT: '#2563eb',
          foreground: '#ffffff'
        },
        secondary: {
          DEFAULT: '#4b5563',
          foreground: '#ffffff'
        },
        muted: {
          DEFAULT: '#6b7280',
          foreground: '#d1d5db'
        },
        accent: {
          DEFAULT: '#3b82f6',
          foreground: '#ffffff'
        },
        destructive: {
          DEFAULT: '#dc2626',
          foreground: '#ffffff'
        },
        border: '#e5e7eb',
        input: '#e5e7eb',
        ring: '#3b82f6',
        chart: {
          '1': '#60a5fa',
          '2': '#34d399',
          '3': '#fbbf24',
          '4': '#f87171',
          '5': '#a78bfa'
        }
      },
      boxShadow: {
        // ✅ 네온 그림자
        neon: '0 0 20px rgba(0, 255, 255, 0.5)',
      },
      backdropBlur: {
        // ✅ 유리효과
        sm: '4px',
        md: '8px',
      },
    },
  },
  plugins: [
    require("tailwindcss-animate"),
  ],
};

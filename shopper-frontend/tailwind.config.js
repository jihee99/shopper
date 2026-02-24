/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        background: '#F8F5EF', // 샌드 화이트
        primary: '#7A9E8A',    // 세이지 그린
        secondary: '#E5D9C8',  // 웜 샌드
        text: '#2A3028',       // 딥 올리브
        accent: '#C87B50',     // 번트 오렌지
      },
    },
  },
  plugins: [],
}

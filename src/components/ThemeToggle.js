'use client';

import { FiSun, FiMoon } from 'react-icons/fi';
import { useTheme } from '../context/ThemeContext';
import { useEffect, useState } from 'react';

export default function ThemeToggle() {
  const { isDarkMode, toggleTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  // After mounting, we can render the theme toggle
  useEffect(() => {
    setMounted(true);
  }, []);

  if (!mounted) {
    return null;
  }

  return (
    <div className="fixed bottom-6 right-6 z-50">
      <button
        onClick={toggleTheme}
        className="p-3 rounded-full bg-cardBg hover:bg-opacity-80 shadow-lg transition-all duration-300 hover:scale-110"
        aria-label={isDarkMode ? 'Switch to light mode' : 'Switch to dark mode'}
      >
        {isDarkMode ? (
          <FiSun className="w-6 h-6 text-yellow-300" />
        ) : (
          <FiMoon className="w-6 h-6 text-blue-700" />
        )}
      </button>
    </div>
  );
} 
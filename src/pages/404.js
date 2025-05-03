import Head from 'next/head';
import { useRouter } from 'next/router';
import React, { useState, useEffect } from 'react';
import Navbar from '../components/Navbar';
import dynamic from 'next/dynamic';

// Import ThemeToggle dynamically to avoid SSR
const ThemeToggle = dynamic(() => import('../components/ThemeToggle'), { 
  ssr: false 
});

export default function Custom404() {
  const router = useRouter();
  const [mounted, setMounted] = useState(false);
  
  useEffect(() => {
    setMounted(true);
  }, []);
  
  return (
    <div className="min-h-screen flex flex-col bg-background">
      <Head>
        <title>404 - Page Not Found | Genius Chess Academy</title>
        <meta name="description" content="Page not found" />
      </Head>
      
      <Navbar />
      
      <div className="flex-grow flex flex-col items-center justify-center bg-background text-foreground p-4">
        <div className="text-center max-w-2xl mx-auto">
          <h1 className="text-6xl font-bold mb-4">404</h1>
          <h2 className="text-3xl font-semibold mb-6">Checkmate! Page Not Found</h2>
          
          <div className="relative w-64 h-64 mx-auto mb-8">
            {/* Broken chessboard */}
            <div className="absolute inset-0 grid grid-cols-8 grid-rows-8 opacity-60">
              {Array.from({ length: 64 }).map((_, index) => {
                const row = Math.floor(index / 8);
                const col = index % 8;
                const isBlack = (row + col) % 2 === 1;
                // Add some randomness to create broken effect
                const isDisplaced = Math.random() > 0.7;
                const translateX = isDisplaced ? `${(Math.random() * 20) - 10}px` : '0';
                const translateY = isDisplaced ? `${(Math.random() * 20) - 10}px` : '0';
                const rotate = isDisplaced ? `${(Math.random() * 20) - 10}deg` : '0deg';
                
                return (
                  <div 
                    key={index} 
                    className={`${isBlack ? 'bg-gray-700 dark:bg-gray-900' : 'bg-gray-300 dark:bg-gray-400'} transform transition-all duration-300`}
                    style={{
                      transform: `translate(${translateX}, ${translateY}) rotate(${rotate})`,
                    }}
                  />
                );
              })}
            </div>
            
            {/* Broken king piece */}
            <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2">
              <svg width="80" height="80" viewBox="0 0 24 24" className="text-red-500 dark:text-red-600 opacity-90">
                <path 
                  fill="currentColor" 
                  d="M12,2L8,6H16L12,2M17,7V13H15V7H9V13H7V7H3V21H21V7H17Z" 
                />
              </svg>
              <div className="absolute w-full h-full top-0 left-0 bg-gradient-to-br from-transparent to-cardBg opacity-30" />
            </div>
          </div>
          
          <p className="text-xl mb-8 text-secondary">
            The move you're trying to make is invalid. This page has been captured!
          </p>
          
          <button 
            onClick={() => router.push('/')}
            className="px-6 py-3 bg-primary hover:bg-primary/80 text-white rounded-lg font-semibold transition-colors duration-300"
          >
            Return to Home Board
          </button>
        </div>
      </div>
      
      {/* Only render ThemeToggle after client-side hydration */}
      {mounted && <ThemeToggle />}
    </div>
  );
} 
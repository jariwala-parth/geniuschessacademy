import Navbar from '../components/Navbar';
import dynamic from 'next/dynamic';
import Image from 'next/image';
import { useTheme } from '../context/ThemeContext';
import { useEffect, useState } from 'react';

// Import ThemeToggle dynamically to avoid SSR
const ThemeToggle = dynamic(() => import('../components/ThemeToggle'), { 
  ssr: false 
});

export default function Coaches() {
  // Add mounted state to prevent hydration mismatch
  const [mounted, setMounted] = useState(false);
  
  useEffect(() => {
    setMounted(true);
  }, []);
  
  return (
    <main className="min-h-screen bg-background">
      <Navbar />
      
      <div className="max-w-6xl mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-foreground mb-8">Our Coaches</h1>
        
        <div className="bg-cardBg rounded-lg shadow-md mb-8 overflow-hidden">
          <div className="flex flex-col md:flex-row">
            <div className="md:w-2/5 lg:w-1/3 bg-gray-100 dark:bg-gray-800 relative">
              <div className="w-full h-full min-h-[320px]">
                <Image 
                  src="/chess-head-coach-optimized.png" 
                  alt="Kanaiyalal Jariwala - Head Coach" 
                  fill
                  className="object-cover object-top"
                  priority
                />
              </div>
            </div>
            
            <div className="md:w-3/5 lg:w-2/3 p-6">
              <h2 className="text-2xl font-bold text-foreground mb-2">Kanaiyalal Jariwala</h2>
              <p className="text-secondary text-lg mb-5 pb-4 border-b border-gray-200 dark:border-gray-700">Head Coach</p>
              
              <div className="flex items-center mb-6">
                <span className="font-medium text-foreground">Experience:</span>
                <span className="ml-2 text-secondary">25 years</span>
              </div>
              
              <h3 className="text-xl font-semibold text-foreground mb-3">About</h3>
              <p className="text-secondary mb-4">
                Kanaiyalal Jariwala is our head coach with 25 years of experience in teaching chess to students of all ages and skill levels. 
                He has trained numerous players and his teaching methodology focuses on developing strategic thinking and pattern recognition.
              </p>
            </div>
          </div>
        </div>
      </div>
      
      {/* Only render ThemeToggle after client-side hydration */}
      {mounted && <ThemeToggle />}
    </main>
  );
} 
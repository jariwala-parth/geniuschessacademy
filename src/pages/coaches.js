import Navbar from '../components/Navbar';
import dynamic from 'next/dynamic';
import Image from 'next/image';

// Import ThemeToggle dynamically to avoid SSR
const ThemeToggle = dynamic(() => import('../components/ThemeToggle'), { 
  ssr: false 
});

export default function Coaches() {
  return (
    <main className="min-h-screen bg-background">
      <Navbar />
      
      <div className="max-w-6xl mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-foreground mb-8">Our Coaches</h1>
        
        <div className="bg-cardBg p-6 rounded-lg shadow-md mb-8">
          <div className="flex flex-col md:flex-row gap-6">
            <div className="md:w-1/3 relative h-80">
              <div className="w-full h-full bg-gray-300 dark:bg-gray-700 rounded-md flex items-center justify-center">
                <span className="text-gray-500 dark:text-gray-400">Coach Image</span>
              </div>
            </div>
            
            <div className="md:w-2/3">
              <h2 className="text-2xl font-bold text-foreground mb-2">Kanaiyalal Jariwala</h2>
              <p className="text-secondary text-lg mb-4">Head Coach</p>
              
              <div className="flex items-center mb-4">
                <span className="font-medium text-foreground">Experience:</span>
                <span className="ml-2 text-secondary">25 years</span>
              </div>
              
              <h3 className="text-xl font-semibold text-foreground mb-3">About</h3>
              <p className="text-secondary mb-4">
                Kanaiyalal Jariwala is our head coach with 25 years of experience in teaching chess to students of all ages and skill levels. 
                He has trained numerous champions and his teaching methodology focuses on developing strategic thinking and pattern recognition.
              </p>
              
              <h3 className="text-xl font-semibold text-foreground mb-3">Achievements</h3>
              <ul className="list-disc pl-5 text-secondary">
                <li className="mb-2">International Master title holder</li>
                <li className="mb-2">5-time State Chess Champion</li>
                <li className="mb-2">Trained over 50 national-level players</li>
                <li>Author of "Strategic Chess Thinking" handbook</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
      
      {/* Theme Toggle */}
      <ThemeToggle />
    </main>
  );
} 
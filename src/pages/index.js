import Navbar from '../components/Navbar';
import Image from 'next/image';
import Link from 'next/link';
import dynamic from 'next/dynamic';

// Import ThemeToggle dynamically to avoid SSR
const ThemeToggle = dynamic(() => import('../components/ThemeToggle'), { 
  ssr: false 
});

export default function Home() {
  return (
    <main className="min-h-screen bg-background">
      <Navbar />
      
      <div className="max-w-6xl mx-auto px-4 py-8">
        {/* Hero Section */}
        <div className="relative rounded-lg overflow-hidden mb-12 shadow-xl">
          <div className="relative aspect-[16/9] w-full">
            <Image 
              src="/chess-kids.jpg" 
              alt="Children playing chess" 
              fill
              priority
              className="object-cover"
            />
            <div className="absolute inset-0 bg-black bg-opacity-40 flex flex-col justify-center items-start p-10">
              <h1 className="text-white font-bold mb-4">
                Learn and play chess with the best
              </h1>
              <p className="text-white text-lg mb-6">
                Join our academy and improve your game
              </p>
              <Link href="/join" className="px-6 py-3 bg-primary text-white rounded-md hover:bg-blue-700 transition">
                Join Now
              </Link>
            </div>
          </div>
        </div>
        
        {/* About Section */}
        <section className="py-8">
          <h2 className="text-foreground mb-6">About</h2>
          <div className="bg-cardBg rounded-lg p-6 shadow-md">
            <p className="text-foreground mb-4">
              The Genius Chess Academy is a local chess school that has been teaching children and adults from beginners to advanced 
              players since 2010. Our philosophy is to create a fun and engaging environment for our students to learn and grow their
              chess skills. We believe that every student has the potential to become a great chess player with the right guidance and
              support.
            </p>
            <p className="text-foreground">
              Our head coach, Kanaiyalal Jariwala, is an experienced chess player and coach with 25 years of experience who has helped many students achieve
              success in chess tournaments.
            </p>
          </div>
        </section>
      </div>

      {/* Theme Toggle */}
      <ThemeToggle />
    </main>
  );
} 
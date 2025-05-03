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
              className="object-cover object-top"
            />
            <div className="absolute inset-0 bg-gradient-to-t from-black via-black/60 to-transparent flex flex-col justify-end items-start p-10">
              <h1 className="text-white font-bold text-4xl mb-3">
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
            Since 2000, Genius Chess Academy has been a cornerstone of the local chess community in Surat, passionately nurturing the strategic minds of children and adults alike. From absolute beginners taking their first steps on the 64 squares to advanced players honing their tournament skills, we provide a fun, engaging, and supportive environment where every student can unlock their full chess potential.
            </p>
            <p className="text-foreground mb-4">
              At Genius Chess Academy, we believe that with the right guidance, every individual can develop into a formidable chess player. Our philosophy centers on creating a dynamic learning experience that fosters both a deep understanding of chess principles and a genuine love for the game.
            </p>
            <p className="text-foreground mb-4">
              Guiding our students is our esteemed head coach, Kanaiyalal Jariwala. With an impressive 25 years of experience in both playing and coaching chess, Kanaiyalal brings a wealth of knowledge and a proven track record of success. He has personally mentored numerous students who have achieved significant milestones in chess tournaments at various levels. His patient approach and insightful guidance are instrumental in helping each student not only grasp the intricacies of chess but also develop critical thinking, problem-solving skills, and the strategic foresight that extends far beyond the chessboard.
            </p>
            <p className="text-foreground mb-4">
              Join the Genius Chess Academy family and embark on your own exciting chess journey!
            </p>
          </div>
        </section>
      </div>

      {/* Theme Toggle */}
      <ThemeToggle />
    </main>
  );
} 
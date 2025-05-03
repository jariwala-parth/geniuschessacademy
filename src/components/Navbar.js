'use client';

import Link from 'next/link';
import { FiUser, FiShoppingCart } from 'react-icons/fi';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logout } = useAuth();

  return (
    <nav className="px-4 py-3 flex items-center justify-between border-b border-gray-200 dark:border-gray-700">
      <div className="flex items-center">
        <Link href="/" className="text-xl font-bold text-foreground">
          Genius Chess Academy
        </Link>
      </div>
      
      <div className="flex items-center space-x-6">
        <Link href="/" className="text-foreground hover:text-primary">Home</Link>
        <Link href="/lessons" className="text-foreground hover:text-primary">Lessons</Link>
        <Link href="/coaches" className="text-foreground hover:text-primary">Coaches</Link>
        <Link href="/tournaments" className="text-foreground hover:text-primary">Tournaments</Link>
        <Link href="/contact" className="text-foreground hover:text-primary">Contact Us</Link>
        
        {user ? (
          <>
            <Link 
              href={user.role === 'admin' ? '/admin/dashboard' : '/dashboard'} 
              className="text-foreground hover:text-primary p-2"
            >
              <FiUser className="w-5 h-5" />
            </Link>
            <Link href="/cart" className="text-foreground hover:text-primary p-2">
              <FiShoppingCart className="w-5 h-5" />
            </Link>
            <button
              onClick={logout}
              className="text-foreground hover:text-primary p-2"
            >
              Logout
            </button>
          </>
        ) : (
          <Link href="/login" className="text-foreground hover:text-primary">
            Login
          </Link>
        )}
      </div>
    </nav>
  );
} 
import { useState } from 'react';
import AdminSidebar from './AdminSidebar';
import { FiMenu, FiX } from 'react-icons/fi';
import ProtectedRoute from './ProtectedRoute';
import dynamic from 'next/dynamic';

// Import ThemeToggle dynamically to avoid SSR
const ThemeToggle = dynamic(() => import('./ThemeToggle'), { 
  ssr: false 
});

export default function AdminLayout({ children, title }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <ProtectedRoute adminOnly={true}>
      <div className="flex h-screen bg-background">
        {/* Sidebar for desktop */}
        <div className="hidden md:block">
          <AdminSidebar />
        </div>

        {/* Mobile sidebar */}
        {sidebarOpen && (
          <div className="fixed inset-0 z-40 md:hidden">
            <div className="fixed inset-0 bg-gray-600 bg-opacity-50" onClick={() => setSidebarOpen(false)}></div>
            <div className="fixed inset-y-0 left-0 z-40 w-64 transition duration-300 ease-in-out transform">
              <AdminSidebar />
            </div>
            <button
              className="absolute top-4 right-4 z-50 p-2 rounded-md text-gray-400 hover:text-white hover:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-white"
              onClick={() => setSidebarOpen(false)}
            >
              <FiX className="h-6 w-6" />
            </button>
          </div>
        )}

        {/* Main content */}
        <div className="flex-1 flex flex-col overflow-hidden">
          {/* Header */}
          <header className="bg-cardBg shadow-sm z-10">
            <div className="px-4 sm:px-6 py-4 flex justify-between items-center">
              <button
                className="md:hidden p-2 rounded-md text-gray-400 hover:text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-800 focus:outline-none"
                onClick={() => setSidebarOpen(true)}
              >
                <FiMenu className="h-6 w-6" />
              </button>
              <h1 className="text-lg font-semibold text-foreground">{title}</h1>
              <div className="flex items-center">
                <ThemeToggle />
              </div>
            </div>
          </header>

          {/* Main content */}
          <main className="flex-1 overflow-y-auto p-4 sm:p-6">
            {children}
          </main>
        </div>
      </div>
    </ProtectedRoute>
  );
} 
import Link from 'next/link';
import { useRouter } from 'next/router';
import { useAuth } from '../context/AuthContext';
import { 
  FiHome, 
  FiBook, 
  FiUsers, 
  FiCalendar, 
  FiSettings, 
  FiLogOut,
  FiGrid
} from 'react-icons/fi';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faIndianRupeeSign } from '@fortawesome/free-solid-svg-icons';

export default function AdminSidebar() {
  const router = useRouter();
  const { logout } = useAuth();

  const isActive = (path) => {
    return router.pathname === path || router.pathname.startsWith(`${path}/`);
  };

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  return (
    <aside className="w-64 bg-cardBg h-screen sticky top-0 overflow-y-auto border-r border-gray-200 dark:border-gray-700">
      <div className="p-4 border-b border-gray-200 dark:border-gray-700">
        <Link href="/admin/dashboard" className="text-xl font-bold text-foreground">
          Genius Chess Academy
        </Link>
      </div>
      <nav className="mt-6">
        <ul className="space-y-2">
          <li>
            <Link 
              href="/admin/dashboard" 
              className={`flex items-center px-4 py-3 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors ${
                isActive('/admin/dashboard') ? 'bg-gray-100 dark:bg-gray-800 text-primary' : 'text-foreground'
              }`}
            >
              <FiHome className="mr-3 w-5 h-5" />
              <span>Home</span>
            </Link>
          </li>
          <li>
            <Link 
              href="/admin/courses" 
              className={`flex items-center px-4 py-3 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors ${
                isActive('/admin/courses') ? 'bg-gray-100 dark:bg-gray-800 text-primary' : 'text-foreground'
              }`}
            >
              <FiBook className="mr-3 w-5 h-5" />
              <span>Courses</span>
            </Link>
          </li>
          <li>
            <Link 
              href="/admin/batches" 
              className={`flex items-center px-4 py-3 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors ${
                isActive('/admin/batches') ? 'bg-gray-100 dark:bg-gray-800 text-primary' : 'text-foreground'
              }`}
            >
              <FiGrid className="mr-3 w-5 h-5" />
              <span>Batches</span>
            </Link>
          </li>
          <li>
            <Link 
              href="/admin/students" 
              className={`flex items-center px-4 py-3 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors ${
                isActive('/admin/students') ? 'bg-gray-100 dark:bg-gray-800 text-primary' : 'text-foreground'
              }`}
            >
              <FiUsers className="mr-3 w-5 h-5" />
              <span>Students</span>
            </Link>
          </li>
          <li>
            <Link 
              href="/admin/attendance" 
              className={`flex items-center px-4 py-3 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors ${
                isActive('/admin/attendance') ? 'bg-gray-100 dark:bg-gray-800 text-primary' : 'text-foreground'
              }`}
            >
              <FiCalendar className="mr-3 w-5 h-5" />
              <span>Attendance</span>
            </Link>
          </li>
          <li>
            <Link 
              href="/admin/fees" 
              className={`flex items-center px-4 py-3 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors ${
                isActive('/admin/fees') ? 'bg-gray-100 dark:bg-gray-800 text-primary' : 'text-foreground'
              }`}
            >
              <FontAwesomeIcon icon={faIndianRupeeSign} className="mr-3 w-5 h-5" />
              <span>Fees</span>
            </Link>
          </li>
          <li>
            <Link 
              href="/admin/settings" 
              className={`flex items-center px-4 py-3 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors ${
                isActive('/admin/settings') ? 'bg-gray-100 dark:bg-gray-800 text-primary' : 'text-foreground'
              }`}
            >
              <FiSettings className="mr-3 w-5 h-5" />
              <span>Settings</span>
            </Link>
          </li>
        </ul>
      </nav>
      <div className="absolute bottom-0 w-full p-4 border-t border-gray-200 dark:border-gray-700">
        <button
          onClick={handleLogout}
          className="flex items-center w-full px-4 py-3 text-red-500 hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
        >
          <FiLogOut className="mr-3 w-5 h-5" />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  );
} 
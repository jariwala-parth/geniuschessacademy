import { useEffect } from 'react';
import AdminLayout from '../../components/AdminLayout';
import { FiUsers, FiBookOpen, FiCalendar, FiDollarSign } from 'react-icons/fi';
import Link from 'next/link';
import { useRouter } from 'next/router';
import { useAuth } from '../../context/AuthContext';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faRupeeSign } from '@fortawesome/free-solid-svg-icons';

export default function Dashboard() {
  const router = useRouter();
  const { user } = useAuth();

  // Redirect to batches page when visiting dashboard directly
  useEffect(() => {
    if (user && user.role === 'admin') {
      router.push('/admin/batches');
    }
  }, [user, router]);

  // This page won't actually be rendered due to the redirect
  // But we'll provide a component anyway
  return (
    <AdminLayout title="Dashboard">
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="text-secondary">Welcome to the admin dashboard</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="bg-cardBg p-6 rounded-lg shadow flex items-center">
            <div className="rounded-full bg-blue-100 p-3 mr-4">
              <FiUsers className="w-6 h-6 text-blue-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-foreground">36</h2>
              <p className="text-sm text-secondary">Total Students</p>
            </div>
          </div>

          <div className="bg-cardBg p-6 rounded-lg shadow flex items-center">
            <div className="rounded-full bg-green-100 p-3 mr-4">
              <FiBookOpen className="w-6 h-6 text-green-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-foreground">4</h2>
              <p className="text-sm text-secondary">Active Courses</p>
            </div>
          </div>

          <div className="bg-cardBg p-6 rounded-lg shadow flex items-center">
            <div className="rounded-full bg-purple-100 p-3 mr-4">
              <FiCalendar className="w-6 h-6 text-purple-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-foreground">4</h2>
              <p className="text-sm text-secondary">Active Batches</p>
            </div>
          </div>

          <div className="bg-cardBg p-6 rounded-lg shadow flex items-center">
            <div className="rounded-full bg-yellow-100 p-3 mr-4">
              <FiDollarSign className="w-6 h-6 text-yellow-600" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-foreground">
                <FontAwesomeIcon icon={faRupeeSign} className="mr-1" />7,000
              </h2>
              <p className="text-sm text-secondary">Monthly Revenue</p>
            </div>
          </div>
        </div>

        <div className="flex justify-center space-x-4">
          <Link 
            href="/admin/students"
            className="bg-primary text-white py-3 px-6 rounded-md hover:bg-blue-700 transition-colors"
          >
            View Students
          </Link>
          <Link 
            href="/admin/batches"
            className="bg-primary text-white py-3 px-6 rounded-md hover:bg-blue-700 transition-colors"
          >
            Manage Batches
          </Link>
        </div>
      </div>
    </AdminLayout>
  );
} 
import { useState } from 'react';
import AdminLayout from '../../components/AdminLayout';
import { FiSearch, FiEye } from 'react-icons/fi';
import Link from 'next/link';

// Mock batch data
const mockBatches = [
  {
    id: 1,
    name: "Beginner's Batch",
    course: "Beginner's Course",
    startDate: "15th July 2022",
    endDate: "15th September 2022",
    fee: 1000,
    students: 10
  },
  {
    id: 2,
    name: "Intermediate's Batch",
    course: "Intermediate's Course",
    startDate: "1st August 2022",
    endDate: "1st October 2022",
    fee: 1500,
    students: 8
  },
  {
    id: 3,
    name: "Advanced Batch",
    course: "Advanced Course",
    startDate: "5th September 2022",
    endDate: "5th November 2022",
    fee: 2000,
    students: 12
  },
  {
    id: 4,
    name: "Professional's Batch",
    course: "Professional's Course",
    startDate: "20th June 2022",
    endDate: "20th August 2022",
    fee: 2500,
    students: 6
  }
];

export default function Batches() {
  const [searchQuery, setSearchQuery] = useState('');
  const [batches, setBatches] = useState(mockBatches);

  const filteredBatches = batches.filter(
    batch => batch.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <AdminLayout title="Batches">
      <div className="space-y-6">
        <div className="flex justify-between items-center">
          <h1 className="text-2xl font-bold text-foreground">Batches</h1>
          <p className="text-secondary">Manage the data of batches</p>
        </div>

        <div className="relative">
          <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
            <FiSearch className="w-5 h-5 text-gray-400" />
          </div>
          <input
            type="text"
            className="bg-gray-100 dark:bg-gray-800 text-foreground rounded-lg pl-10 pr-4 py-3 w-full focus:outline-none focus:ring-2 focus:ring-primary"
            placeholder="Search for a batch"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>

        <div className="overflow-x-auto bg-cardBg rounded-lg shadow">
          <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead className="bg-gray-100 dark:bg-gray-800">
              <tr>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                  Batch Name
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                  Course
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                  Start Date
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                  End Date
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                  Fee
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                  Students
                </th>
                <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-secondary uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white dark:bg-gray-900 divide-y divide-gray-200 dark:divide-gray-700">
              {filteredBatches.map((batch) => (
                <tr key={batch.id} className="hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
                  <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-foreground">
                    {batch.name}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary">
                    <Link href="#" className="text-primary hover:underline">
                      {batch.course}
                    </Link>
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary">
                    {batch.startDate}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary">
                    {batch.endDate}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary">
                    {batch.fee}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-secondary">
                    {batch.students}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    <Link 
                      href={`/admin/batches/${batch.id}`}
                      className="text-primary hover:text-blue-700 transition-colors"
                    >
                      <span className="flex items-center">
                        <FiEye className="w-4 h-4 mr-1" />
                        View
                      </span>
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="flex justify-center">
          <Link 
            href="/admin/batches/new"
            className="bg-primary text-white py-3 px-6 rounded-md hover:bg-blue-700 transition-colors"
          >
            New Batch
          </Link>
        </div>
      </div>
    </AdminLayout>
  );
} 
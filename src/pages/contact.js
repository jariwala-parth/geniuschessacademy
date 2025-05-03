import { useState } from 'react';
import Navbar from '../components/Navbar';
import dynamic from 'next/dynamic';

// Import ThemeToggle dynamically to avoid SSR
const ThemeToggle = dynamic(() => import('../components/ThemeToggle'), { 
  ssr: false 
});

export default function Contact() {
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    message: ''
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    // In a real app, we would send this data to a server
    console.log('Form submitted:', formData);
    alert('Thank you for your message! We will get back to you soon.');
    setFormData({
      firstName: '',
      lastName: '',
      email: '',
      message: ''
    });
  };

  return (
    <main className="min-h-screen bg-background">
      <Navbar />
      
      <div className="max-w-2xl mx-auto px-4 py-12">
        <h1 className="text-3xl font-bold text-foreground mb-2">Contact Us</h1>
        <p className="text-secondary mb-8">How can we help you? We're here to answer your questions.</p>
        
        <form onSubmit={handleSubmit} className="space-y-6">
          <div>
            <label htmlFor="firstName" className="block text-foreground mb-2">First name</label>
            <input
              type="text"
              id="firstName"
              name="firstName"
              value={formData.firstName}
              onChange={handleChange}
              required
              className="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-foreground"
            />
          </div>
          
          <div>
            <label htmlFor="lastName" className="block text-foreground mb-2">Last name</label>
            <input
              type="text"
              id="lastName"
              name="lastName"
              value={formData.lastName}
              onChange={handleChange}
              required
              className="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-foreground"
            />
          </div>
          
          <div>
            <label htmlFor="email" className="block text-foreground mb-2">Email address</label>
            <input
              type="email"
              id="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              className="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-foreground"
            />
          </div>
          
          <div>
            <label htmlFor="message" className="block text-foreground mb-2">Message</label>
            <textarea
              id="message"
              name="message"
              value={formData.message}
              onChange={handleChange}
              required
              rows="6"
              className="w-full p-3 border border-gray-300 dark:border-gray-600 rounded-md bg-white dark:bg-gray-800 text-foreground resize-none"
            ></textarea>
          </div>
          
          <button
            type="submit"
            className="w-full bg-primary text-white font-medium py-3 px-4 rounded-md hover:bg-blue-700 transition-colors"
          >
            Send message
          </button>
        </form>
      </div>
      
      {/* Theme Toggle */}
      <ThemeToggle />
    </main>
  );
} 
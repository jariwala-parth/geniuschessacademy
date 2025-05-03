import React, { createContext, useState, useContext, useEffect } from 'react';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Check if user is already logged in on component mount
  useEffect(() => {
    const storedUser = localStorage.getItem('gcaUser');
    if (storedUser) {
      setUser(JSON.parse(storedUser));
    }
    setLoading(false);
  }, []);

  // Login function
  const login = async (username, password) => {
    // In a real app, this would be an API call
    // For now, we'll just simulate a successful login
    
    // Mock admin user
    if (username === 'admin' && password === 'admin123') {
      const userData = {
        id: 1,
        username: 'admin',
        name: 'Admin User',
        role: 'admin'
      };
      
      localStorage.setItem('gcaUser', JSON.stringify(userData));
      setUser(userData);
      return { success: true, user: userData };
    }
    
    // Mock student user
    if (username === 'student' && password === 'student123') {
      const userData = {
        id: 2,
        username: 'student',
        name: 'Student User',
        role: 'student'
      };
      
      localStorage.setItem('gcaUser', JSON.stringify(userData));
      setUser(userData);
      return { success: true, user: userData };
    }
    
    return { success: false, error: 'Invalid credentials' };
  };

  // Logout function
  const logout = () => {
    localStorage.removeItem('gcaUser');
    setUser(null);
  };

  // Check if user is admin
  const isAdmin = () => {
    return user && user.role === 'admin';
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, isAdmin, loading }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
} 
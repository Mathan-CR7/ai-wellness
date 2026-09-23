import React, { createContext, useContext, useState, useEffect } from 'react';
import { UserProfile, AuthResponse } from '../types';
import { userService } from '../api/userService';
import { authService } from '../api/authService';

interface AuthContextType {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (fullName: string, email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<UserProfile | null>(() => {
    const saved = localStorage.getItem('wellness_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('wellness_token'));
  const [isLoading, setIsLoading] = useState<boolean>(() => {
    const hasToken = !!localStorage.getItem('wellness_token');
    const hasUser = !!localStorage.getItem('wellness_user');
    // If no token, or if saved user exists, don't block UI with full screen spinner
    return hasToken && !hasUser;
  });

  const handleAuthSuccess = (data: AuthResponse) => {
    localStorage.setItem('wellness_token', data.token);
    localStorage.setItem('wellness_user', JSON.stringify(data.user));
    setToken(data.token);
    setUser(data.user);
  };

  const login = async (email: string, password: string) => {
    const data = await authService.login({ email, password });
    handleAuthSuccess(data);
  };

  const register = async (fullName: string, email: string, password: string) => {
    const data = await authService.register({ fullName, email, password });
    handleAuthSuccess(data);
  };

  const logout = () => {
    localStorage.removeItem('wellness_token');
    localStorage.removeItem('wellness_user');
    setToken(null);
    setUser(null);
  };

  const refreshProfile = async () => {
    if (!token) return;
    try {
      const profile = await userService.getCurrentProfile();
      setUser(profile);
      localStorage.setItem('wellness_user', JSON.stringify(profile));
    } catch (e) {
      console.error('Failed to refresh profile', e);
    }
  };

  useEffect(() => {
    let isMounted = true;
    const initAuth = async () => {
      if (token) {
        try {
          // Timeout race condition: if backend request takes > 3 seconds, unblock UI
          const timeoutPromise = new Promise((_, reject) =>
            setTimeout(() => reject(new Error('Auth timeout')), 3000)
          );
          const profile = (await Promise.race([
            userService.getCurrentProfile(),
            timeoutPromise,
          ])) as UserProfile;
          if (isMounted) {
            setUser(profile);
            localStorage.setItem('wellness_user', JSON.stringify(profile));
          }
        } catch (err) {
          console.warn('Auth init note:', err);
          // If token invalid (401), logout
          if (!localStorage.getItem('wellness_user')) {
            logout();
          }
        }
      }
      if (isMounted) {
        setIsLoading(false);
      }
    };
    initAuth();
    return () => {
      isMounted = false;
    };
  }, [token]);

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token && !!user,
        isLoading,
        login,
        register,
        logout,
        refreshProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

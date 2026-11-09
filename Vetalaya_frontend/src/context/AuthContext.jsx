import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import {
  loginApi,
  registerApi,
  googleAuthApi,
  demoLoginApi,
  getCurrentUserApi,
  logoutApi,
  exchangeCodeApi,
} from '../services/api';

const AuthContext = createContext(null);

export const DEMO_USERS = {
  PET_PARENT: {
    id: 'usr-parent-001',
    name: 'Rahul Sharma',
    email: 'rahul.sharma@example.com',
    role: 'PET_PARENT',
    phone: '+91 98765 43210',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
    petsCount: 3,
  },
  VETERINARIAN: {
    id: 'usr-vet-001',
    name: 'Dr. Priya Sharma',
    email: 'dr.priya@koramangalavet.in',
    role: 'VETERINARIAN',
    phone: '+91 98450 12345',
    avatar: 'https://images.unsplash.com/photo-1559839734-2b71ea197ec2?auto=format&fit=crop&w=200&q=80',
    qualification: 'BVSc & AH, MVSc',
    councilRegNumber: 'KVC-2018-4829',
  },
  CLINIC_STAFF: {
    id: 'usr-staff-001',
    name: 'Ananya Deshmukh',
    email: 'reception@koramangalavet.in',
    role: 'CLINIC_STAFF',
    phone: '+91 80 2553 1234',
    designation: 'Senior Clinic Administrator',
  },
  CLINIC_ADMIN: {
    id: 'usr-admin-001',
    name: 'Dr. Rajesh Kulkarni',
    email: 'director@koramangalavet.in',
    role: 'CLINIC_ADMIN',
    organization: 'Koramangala Pet Care Network',
  },
  PLATFORM_ADMIN: {
    id: 'usr-platform-001',
    name: 'Vetalya Compliance Officer',
    email: 'regulatory@vetalya.in',
    role: 'PLATFORM_ADMIN',
  },
};

export function AuthProvider({ children }) {
  const [jwtToken, setJwtToken] = useState(() => localStorage.getItem('vetalya_jwt_token'));
  const [currentUser, setCurrentUser] = useState(() => {
    const saved = localStorage.getItem('vetalya_auth_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [tokenTimeRemaining, setTokenTimeRemaining] = useState(null);
  const [sessionExpiredNotice, setSessionExpiredNotice] = useState(false);

  // Validate or synchronize active session with Spring Boot backend
  const refreshUserProfile = useCallback(async () => {
    const savedToken = localStorage.getItem('vetalya_jwt_token');
    if (!savedToken) return;

    try {
      const user = await getCurrentUserApi();
      if (user) {
        const mappedUser = {
          id: user.id || user.userId,
          name: user.fullName || user.name || (user.email ? user.email.split('@')[0] : 'Vetalya User'),
          email: user.email,
          role: user.role,
          phone: user.phone,
          avatar: user.avatar || user.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
        };
        setCurrentUser(mappedUser);
        localStorage.setItem('vetalya_auth_user', JSON.stringify(mappedUser));
      }
    } catch (err) {
      console.warn('Session check warning:', err.message);
      // If 401 unauthorized, logout automatically
      if (err.message && err.message.toLowerCase().includes('unauthorized')) {
        logout(true);
      }
    }
  }, []);

  useEffect(() => {
    if (jwtToken) {
      refreshUserProfile();
    }
  }, [jwtToken, refreshUserProfile]);

  // Monitor token expiration countdown & automatically logout when countdown reaches 0
  useEffect(() => {
    if (!jwtToken) {
      setTokenTimeRemaining(null);
      return;
    }

    try {
      const payload = JSON.parse(atob(jwtToken.split('.')[1]));
      if (!payload.exp) return;

      const updateRemaining = () => {
        const remaining = Math.max(0, Math.floor(payload.exp - Date.now() / 1000));
        setTokenTimeRemaining(remaining);
        if (remaining <= 0) {
          console.warn('⚡ JWT token expired! Automatically logging out...');
          logout(true);
        }
      };

      updateRemaining();
      const interval = setInterval(updateRemaining, 1000);
      return () => clearInterval(interval);
    } catch (e) {
      console.warn('Could not parse token expiration:', e);
    }
  }, [jwtToken]);

  // Handle OAuth2 callback (?code=...) on page load
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const code = params.get('code');
    if (code) {
      window.history.replaceState({}, document.title, window.location.pathname);
      setIsLoading(true);
      exchangeCodeApi(code)
        .then((response) => {
          handleAuthSuccess(response);
        })
        .catch((err) => {
          console.error('OAuth exchange error:', err);
          setError(err.message || 'OAuth exchange failed');
        })
        .finally(() => {
          setIsLoading(false);
        });
    }
  }, []);

  const handleAuthSuccess = (authResponse) => {
    if (!authResponse || typeof authResponse !== 'object') {
      throw new Error('Authentication response was invalid or empty from backend.');
    }
    const token = authResponse.token || authResponse.accessToken;
    let user = authResponse.user;

    if (token) {
      setJwtToken(token);
      localStorage.setItem('vetalya_jwt_token', token);
      setSessionExpiredNotice(false);

      if (!user) {
        // Decode user info from JWT claims if backend returned flat token response
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          const roles = payload.roles || [];
          const role = roles[0] || 'ROLE_PET_OWNER';
          const sub = payload.sub || 'user@vetalya.local';
          const isEmail = sub.includes('@');
          user = {
            id: sub,
            name: isEmail ? sub.split('@')[0] : 'Vetalya User',
            email: isEmail ? sub : '',
            role: role,
            authProvider: 'LOCAL / JWT',
          };
        } catch (e) {
          console.warn('Could not decode token claims', e);
        }
      }
    }

    if (user) {
      // Map entity fields to frontend schema
      const mappedUser = {
        id: user.id || user.userId,
        name: user.fullName || user.name || (user.email ? user.email.split('@')[0] : 'Vetalya User'),
        email: user.email,
        role: user.role,
        authProvider: user.authProvider || 'LOCAL',
        phone: user.phone,
        avatar: user.avatarUrl || user.avatar || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
      };
      setCurrentUser(mappedUser);
      localStorage.setItem('vetalya_auth_user', JSON.stringify(mappedUser));
      return mappedUser;
    }
    return user;
  };

  // Standard Email/Password Login
  const login = async (email, password, rememberMe = true) => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await loginApi(email, password, rememberMe);
      const user = handleAuthSuccess(response);
      setIsLoading(false);
      return user;
    } catch (err) {
      setIsLoading(false);
      setError(err.message || 'Login failed');
      throw err;
    }
  };

  // Sign up (supports object payload or individual arguments)
  const signup = async (data, email, password, role = 'ROLE_PET_OWNER') => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await registerApi(data, email, password, role);
      const user = handleAuthSuccess(response);
      setIsLoading(false);
      return user;
    } catch (err) {
      setIsLoading(false);
      setError(err.message || 'Registration failed');
      throw err;
    }
  };

  // Google SSO with Google ID Token
  const loginWithGoogle = async (idToken, role = 'PET_PARENT') => {
    setIsLoading(true);
    setError(null);

    try {
      const tokenToSend = idToken || `mock-google-token:google.parent@vetalya.in:Vetalya Google Parent`;
      const response = await googleAuthApi(tokenToSend, role);
      const user = handleAuthSuccess(response);
      setIsLoading(false);
      return user;
    } catch (err) {
      setIsLoading(false);
      setError(err.message || 'Google Sign-In failed');
      throw err;
    }
  };

  // 1-Click Role Login for instant role verification
  const loginWithDemo = async (roleKey = 'PET_PARENT') => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await demoLoginApi(roleKey);
      const user = handleAuthSuccess(response);
      setIsLoading(false);
      return user;
    } catch (err) {
      console.warn('Backend demo-login unavailable, falling back to local demo user:', err.message);
      const localUser = DEMO_USERS[roleKey] || DEMO_USERS.PET_PARENT;
      setCurrentUser(localUser);
      localStorage.setItem('vetalya_auth_user', JSON.stringify(localUser));
      setIsLoading(false);
      return localUser;
    }
  };

  const logout = async (isAuto = false) => {
    try {
      await logoutApi();
    } catch (e) {
      // ignore
    }
    setCurrentUser(null);
    setJwtToken(null);
    setTokenTimeRemaining(null);
    localStorage.removeItem('vetalya_auth_user');
    localStorage.removeItem('vetalya_jwt_token');
    if (isAuto === true) {
      setSessionExpiredNotice(true);
    } else {
      setSessionExpiredNotice(false);
    }
  };

  const triggerExpireNow = () => {
    logout(true);
  };

  return (
    <AuthContext.Provider
      value={{
        currentUser,
        jwtToken,
        isAuthenticated: Boolean(currentUser && jwtToken),
        role: currentUser?.role || 'ROLE_PET_OWNER',
        isLoading,
        error,
        tokenTimeRemaining,
        sessionExpiredNotice,
        setSessionExpiredNotice,
        triggerExpireNow,
        login,
        signup,
        loginWithGoogle,
        loginWithDemo,
        logout,
        refreshUserProfile,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

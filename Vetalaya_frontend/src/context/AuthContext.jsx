import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
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

  // Validate or synchronize active session with Spring Boot backend on app startup
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

  // On initial page mount only: validate or synchronize saved session with backend
  const initialMountChecked = useRef(false);
  useEffect(() => {
    if (!initialMountChecked.current) {
      initialMountChecked.current = true;
      const savedToken = localStorage.getItem('vetalya_jwt_token');
      if (savedToken) {
        refreshUserProfile();
      }
    }
  }, [refreshUserProfile]);

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

  const handleAuthSuccess = useCallback((authResponse) => {
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
          const role = roles[0] || '';
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
  }, []);

  // Standard Email/Password Login
  const login = useCallback(async (email, password, rememberMe = true) => {
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
  }, [handleAuthSuccess]);

  // Sign up (supports object payload or individual arguments)
  const signup = useCallback(async (data, email, password, role = 'ROLE_PET_OWNER') => {
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
  }, [handleAuthSuccess]);

  // Google SSO with verified Google ID Token
  const loginWithGoogle = useCallback(async (idToken, role = 'PET_PARENT') => {
    if (!idToken) {
      throw new Error('Google credential token was not provided.');
    }
    setIsLoading(true);
    setError(null);

    try {
      const response = await googleAuthApi(idToken, role);
      const user = handleAuthSuccess(response);
      setIsLoading(false);
      return user;
    } catch (err) {
      setIsLoading(false);
      setError(err.message || 'Google Sign-In failed');
      throw err;
    }
  }, [handleAuthSuccess]);

  // 1-Click Role Login for instant role verification via DB
  const loginWithDemo = useCallback(async (roleKey = 'PET_PARENT') => {
    setIsLoading(true);
    setError(null);

    try {
      const response = await demoLoginApi(roleKey);
      const user = handleAuthSuccess(response);
      setIsLoading(false);
      return user;
    } catch (err) {
      setIsLoading(false);
      setError(err.message || 'Demo authentication failed. Database service may be initializing.');
      throw err;
    }
  }, [handleAuthSuccess]);

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
        role: currentUser?.role || '',
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

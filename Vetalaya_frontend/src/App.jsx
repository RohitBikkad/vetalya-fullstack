import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import LandingPage from './components/landing/LandingPage';
import OwnerAuthPage from './components/auth/OwnerAuthPage';
import VetAuthPage from './components/auth/VetAuthPage';
import LoginPage from './components/auth/LoginPage';
import DashboardRouter from './components/dashboard/DashboardRouter';
import VetalyaBrandLogo from './components/auth/VetalyaBrandLogo';
import { getCurrentUserApi, request } from './services/api';
import {
  LogOut,
  ShieldCheck,
  CheckCircle2,
  Key,
  Copy,
  Check,
  RefreshCw,
  Send,
  Lock,
} from 'lucide-react';

function AuthenticatedProfileView() {
  const {
    currentUser,
    role,
    jwtToken,
    logout,
    refreshUserProfile,
    tokenTimeRemaining,
    triggerExpireNow,
  } = useAuth();
  const [copied, setCopied] = useState(false);
  const [apiTesting, setApiTesting] = useState(false);
  const [apiResponse, setApiResponse] = useState(null);

  const handleCopyToken = () => {
    if (!jwtToken) return;
    navigator.clipboard.writeText(jwtToken);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleTestProtectedEndpoint = async () => {
    setApiTesting(true);
    setApiResponse(null);
    try {
      const data = await request('/users/profile');
      setApiResponse({ success: true, data });
    } catch (err) {
      setApiResponse({ success: false, error: err.message });
    } finally {
      setApiTesting(false);
    }
  };

  const handleTestRoleEndpoint = async () => {
    setApiTesting(true);
    setApiResponse(null);
    try {
      const data = await request('/users/vet-portal');
      setApiResponse({ success: true, data });
    } catch (err) {
      setApiResponse({ success: false, error: err.message });
    } finally {
      setApiTesting(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F5EE] flex flex-col justify-between p-6 sm:p-12">
      <header className="max-w-4xl w-full mx-auto flex items-center justify-between pb-8 border-b border-[#10324E]/10">
        <VetalyaBrandLogo />
        <button
          onClick={() => logout()}
          className="flex items-center space-x-2 px-4 py-2.5 rounded-2xl bg-white hover:bg-slate-50 border border-[#10324E]/15 text-xs font-bold text-[#10324E] shadow-2xs transition-all cursor-pointer"
        >
          <LogOut className="w-4 h-4" />
          <span>Log out</span>
        </button>
      </header>

      <main className="max-w-xl w-full mx-auto my-8 bg-white rounded-[32px] p-6 sm:p-8 shadow-vetalya border border-[#10324E]/8 text-center space-y-5 animate-in fade-in">
        <div className="relative mx-auto w-24 h-24">
          <img
            src={currentUser?.avatar || "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80"}
            alt={currentUser?.name}
            className="w-full h-full object-cover rounded-full ring-4 ring-[#2D8A7A]/30 shadow-md"
          />
          <div className="absolute -bottom-1 -right-1 bg-[#2D8A7A] text-white p-1 rounded-full shadow-xs">
            <CheckCircle2 className="w-4 h-4" />
          </div>
        </div>

        <div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#E0F2EB] text-[#2D8A7A] text-[11px] font-extrabold uppercase tracking-wider mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Active JWT Session ({role})</span>
          </span>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-[#10324E] font-display">
            Welcome, {currentUser?.name}!
          </h2>
          <p className="text-xs text-[#242826]/70 mt-1">
            {currentUser?.email}
          </p>
        </div>

        {/* Live Token Expiration Countdown & Auto-Logout Test */}
        <div className="flex items-center justify-between p-3.5 rounded-2xl bg-amber-50/80 border border-amber-200/90 text-xs text-amber-900">
          <div className="flex items-center gap-2">
            <span className="font-bold">⏱️ Session Timer:</span>
            <span className="font-mono font-bold bg-white px-2 py-0.5 rounded-lg border border-amber-300 shadow-2xs">
              {tokenTimeRemaining !== null
                ? `${Math.floor(tokenTimeRemaining / 60).toString().padStart(2, '0')}:${(tokenTimeRemaining % 60).toString().padStart(2, '0')}`
                : '--:--'}
            </span>
            <span className="text-[11px] text-amber-700 hidden sm:inline">(Auto-logout on expiry)</span>
          </div>
          <button
            type="button"
            onClick={triggerExpireNow}
            className="px-2.5 py-1 rounded-xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-[11px] transition-all shadow-xs cursor-pointer flex items-center gap-1"
          >
            <span>⚡ Expire Now (Test)</span>
          </button>
        </div>

        {/* User Details Grid */}
        <div className="p-4 rounded-2xl bg-[#F7F5EE] border border-[#10324E]/8 text-left space-y-2 text-xs">
          <div className="flex justify-between items-center">
            <span className="text-[#242826]/60">Account Role:</span>
            <span className="font-bold text-[#10324E] bg-white px-2 py-0.5 rounded-lg border border-[#10324E]/10">{role}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-[#242826]/60">Auth Provider:</span>
            <span className="font-semibold text-[#2D8A7A] uppercase">{currentUser?.authProvider || 'LOCAL / JWT'}</span>
          </div>
          <div className="flex justify-between items-center">
            <span className="text-[#242826]/60">Database User ID:</span>
            <span className="font-mono font-semibold text-[#10324E]">{currentUser?.id}</span>
          </div>
          {currentUser?.phone && (
            <div className="flex justify-between items-center">
              <span className="text-[#242826]/60">Phone:</span>
              <span className="font-semibold text-[#10324E]">{currentUser?.phone}</span>
            </div>
          )}
        </div>

        {/* JWT Bearer Token View & Copy */}
        {jwtToken && (
          <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/10 text-left space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-1.5 text-xs font-bold text-[#10324E]">
                <Key className="w-3.5 h-3.5 text-[#2D8A7A]" />
                <span>Active JWT Bearer Token</span>
              </div>
              <button
                type="button"
                onClick={handleCopyToken}
                className="flex items-center gap-1 text-[11px] font-bold text-[#2D8A7A] hover:text-[#236e61] cursor-pointer bg-white px-2 py-1 rounded-lg border border-[#2D8A7A]/30 transition-all shadow-2xs"
              >
                {copied ? <Check className="w-3 h-3 text-emerald-600" /> : <Copy className="w-3 h-3" />}
                <span>{copied ? 'Copied!' : 'Copy JWT'}</span>
              </button>
            </div>
            <div className="p-2.5 rounded-xl bg-slate-900 text-emerald-400 font-mono text-[10px] break-all max-h-20 overflow-y-auto leading-relaxed border border-slate-800">
              {jwtToken}
            </div>
            <p className="text-[10px] text-[#242826]/50">
              Stateless HMAC-SHA256 signature verified by Spring Boot filter chain on every request.
            </p>
          </div>
        )}

        {/* Live Backend API Verification Sandbox */}
        <div className="p-4 rounded-2xl bg-[#E0F2EB]/30 border border-[#2D8A7A]/20 text-left space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-[#10324E] flex items-center gap-1.5">
              <Lock className="w-3.5 h-3.5 text-[#2D8A7A]" />
              <span>Verify Spring Boot Protected API</span>
            </span>
          </div>

          <div className="flex flex-wrap gap-2">
            <button
              onClick={handleTestProtectedEndpoint}
              disabled={apiTesting}
              className="flex-1 py-2 px-3 rounded-xl bg-white hover:bg-slate-50 border border-[#2D8A7A]/30 text-xs font-bold text-[#10324E] transition-all cursor-pointer flex items-center justify-center gap-1.5 shadow-2xs"
            >
              <Send className="w-3 h-3 text-[#2D8A7A]" />
              <span>GET /api/users/profile</span>
            </button>

            <button
              onClick={handleTestRoleEndpoint}
              disabled={apiTesting}
              className="flex-1 py-2 px-3 rounded-xl bg-white hover:bg-slate-50 border border-[#2D8A7A]/30 text-xs font-bold text-[#10324E] transition-all cursor-pointer flex items-center justify-center gap-1.5 shadow-2xs"
            >
              <Send className="w-3 h-3 text-[#2D8A7A]" />
              <span>GET /api/users/vet-portal</span>
            </button>
          </div>

          {apiResponse && (
            <div className={`p-2.5 rounded-xl text-[11px] font-mono leading-relaxed border ${
              apiResponse.success
                ? 'bg-emerald-50 text-emerald-800 border-emerald-200'
                : 'bg-rose-50 text-rose-800 border-rose-200'
            }`}>
              <div className="font-bold mb-1">
                {apiResponse.success ? '✓ 200 OK Response from Spring Boot:' : '✗ Forbidden / Error:'}
              </div>
              <pre className="whitespace-pre-wrap overflow-x-auto text-[10px]">
                {JSON.stringify(apiResponse.data || apiResponse.error, null, 2)}
              </pre>
            </div>
          )}
        </div>

        <button
          onClick={() => logout()}
          className="w-full py-3.5 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white font-bold text-xs shadow-md transition-all cursor-pointer"
        >
          Sign Out &amp; Return to Auth Page
        </button>
      </main>

      <footer className="text-center text-xs text-[#242826]/50">
        Vetalya Digital Animal Healthcare Platform • Java Spring Boot &amp; React JWT Architecture
      </footer>
    </div>
  );
}

function MainContent() {
  const { isAuthenticated, isLoading } = useAuth();
  const [currentPath, setCurrentPath] = useState(() => window.location.pathname || '/');

  useEffect(() => {
    const handlePopState = () => {
      setCurrentPath(window.location.pathname || '/');
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const handleNavigate = (path) => {
    if (path !== window.location.pathname) {
      window.history.pushState({}, '', path);
    }
    setCurrentPath(path);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  if (isLoading && !isAuthenticated) {
    return (
      <div className="min-h-screen bg-[#F7F5EE] flex flex-col items-center justify-center p-6 text-center select-none">
        <div className="relative mb-6">
          <div className="w-20 h-20 rounded-full bg-[#E0F2EB] flex items-center justify-center shadow-sm">
            <div className="w-10 h-10 border-3 border-[#2D8A7A] border-t-transparent rounded-full animate-spin" />
          </div>
          <div className="absolute inset-0 rounded-full border border-[#2D8A7A]/20 animate-ping pointer-events-none" />
        </div>
        <h2 className="text-2xl font-extrabold text-[#10324E] font-display tracking-tight">
          Signing into Vetalya...
        </h2>
        <p className="text-xs text-[#242826]/70 mt-2 max-w-sm leading-relaxed">
          Preparing your secure care workspace.
        </p>
      </div>
    );
  }

  // Authenticated users go to their role-gated dashboard
  if (isAuthenticated) {
    return <DashboardRouter onNavigate={handleNavigate} />;
  }

  // Role-tailored & Public Unauthenticated Routing
  if (currentPath === '/owner/login') {
    return <OwnerAuthPage initialMode="login" onNavigate={handleNavigate} />;
  }
  if (currentPath === '/owner/register') {
    return <OwnerAuthPage initialMode="register" onNavigate={handleNavigate} />;
  }
  if (currentPath === '/vet/login') {
    return <VetAuthPage initialMode="login" onNavigate={handleNavigate} />;
  }
  if (currentPath === '/vet/register') {
    return <VetAuthPage initialMode="register" onNavigate={handleNavigate} />;
  }
  if (currentPath === '/login') {
    return <LoginPage onNavigate={handleNavigate} />;
  }

  // Default to Landing Page for '/' and all general marketing views
  return <LandingPage onNavigate={handleNavigate} />;
}

export default function App() {
  return (
    <AuthProvider>
      <MainContent />
    </AuthProvider>
  );
}

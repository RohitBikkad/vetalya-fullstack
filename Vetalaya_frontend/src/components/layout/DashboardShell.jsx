import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import VetalyaBrandLogo from '../auth/VetalyaBrandLogo';
import {
  Bell,
  LogOut,
  ShieldCheck,
  Stethoscope,
  Heart,
  ChevronDown,
  Database,
  X,
  Clock,
  Menu,
} from 'lucide-react';

export default function DashboardShell({ children }) {
  const {
    currentUser,
    role,
    logout,
    tokenTimeRemaining,
    triggerExpireNow,
  } = useAuth();

  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showMobileMenu, setShowMobileMenu] = useState(false);
  const [showDbInspector, setShowDbInspector] = useState(false);
  const [dbData, setDbData] = useState(null);
  const [loadingDb, setLoadingDb] = useState(false);

  // Normalize effective role
  const isVet = role === 'ROLE_VET' || role === 'VETERINARIAN';
  const isAdmin = role === 'ROLE_PLATFORM_ADMIN' || role === 'ROLE_CLINIC_ADMIN' || role === 'PLATFORM_ADMIN' || role === 'CLINIC_ADMIN';

  const roleMeta = isAdmin
    ? { label: 'Platform Admin', icon: ShieldCheck, color: 'text-amber-800', bg: 'bg-amber-100', border: 'border-amber-300' }
    : isVet
    ? { label: 'Veterinarian', icon: Stethoscope, color: 'text-[#10324E]', bg: 'bg-[#E8EEF4]', border: 'border-[#10324E]/20' }
    : { label: 'Pet Parent', icon: Heart, color: 'text-[#2D8A7A]', bg: 'bg-[#E0F2EB]', border: 'border-[#2D8A7A]/30' };

  // Contextual navigation links tailored strictly to the authenticated user's role
  const navItems = isAdmin
    ? [
        { label: 'Executive KPIs', href: '#executive-kpis' },
        { label: 'Council Approvals', href: '#vet-approvals' },
        { label: 'Security & Audit', href: '#security-audit' },
        { label: 'Live Telemetry', href: '#telemetry' },
      ]
    : isVet
    ? [
        { label: 'OPD Queue', href: '#opd-queue' },
        { label: 'SOAP Consultation', href: '#soap-consult' },
        { label: 'Prescription Desk', href: '#rx-generator' },
        { label: 'Diagnostic Imaging', href: '#diagnostics' },
      ]
    : [
        { label: 'Health Passports', href: '#passports' },
        { label: 'Clinic Visits', href: '#appointments' },
        { label: 'Vaccines & Deworming', href: '#vaccines' },
        { label: 'Digital Prescriptions', href: '#prescriptions' },
      ];

  const notifications = isVet
    ? [
        { id: 1, title: 'OPD Queue Active', time: 'Just now', text: 'Live consultation queue is initialized and ready for check-ins.', icon: Clock },
        { id: 2, title: 'SOAP Records Synchronized', time: '1h ago', text: 'Digital prescription templates and drug formularies loaded.', icon: Stethoscope },
      ]
    : isAdmin
    ? [
        { id: 1, title: 'Security Audit Active', time: 'Active', text: 'OAuth2 exchange codes and JWT rotation monitors running.', icon: ShieldCheck },
      ]
    : [
        { id: 1, title: 'Welcome to Vetalya', time: 'Just now', text: 'Your pet parent health portal is ready. Register your companion to track health passports and vaccines.', icon: Heart },
        { id: 2, title: 'Veterinary Network', time: 'Active', text: 'Access 24/7 emergency telephone triage and book clinic consultations.', icon: Stethoscope },
      ];

  const handleFetchDb = async () => {
    setLoadingDb(true);
    try {
      const res = await fetch('/api/public/db-inspect');
      const data = await res.json();
      setDbData(data);
    } catch (e) {
      setDbData({ error: 'Failed to inspect live DB: ' + e.message });
    } finally {
      setLoadingDb(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#F7F5EE] text-[#242826] flex flex-col font-sans selection:bg-[#10324E] selection:text-white">
      {/* ── Top Brand Header Bar ── */}
      <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-[#10324E]/10 shadow-2xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-20 flex items-center justify-between gap-4">
          
          {/* Logo & Platform Role Tag */}
          <div className="flex items-center gap-3 sm:gap-4 shrink-0">
            <div className="flex items-center text-left">
              <VetalyaBrandLogo emblemSize="w-10 h-10" wordmarkHeight="h-8 sm:h-9" />
            </div>

            {/* Role Badge */}
            <div className={`hidden md:inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border ${roleMeta.bg} ${roleMeta.color} ${roleMeta.border} shadow-2xs`}>
              <roleMeta.icon className="w-3.5 h-3.5" />
              <span>{roleMeta.label}</span>
            </div>
          </div>

          {/* Role-Specific Navigation Menu (Desktop) */}
          <nav className="hidden lg:flex items-center gap-1 p-1 bg-[#FAF8F5] rounded-2xl border border-[#10324E]/10 text-xs font-bold text-[#10324E]">
            {navItems.map((item, idx) => (
              <a
                key={item.label}
                href={item.href}
                className={`px-3.5 py-1.5 rounded-xl transition-all flex items-center gap-1.5 ${
                  idx === 0
                    ? isVet
                      ? 'bg-white text-[#10324E] shadow-2xs font-extrabold'
                      : isAdmin
                      ? 'bg-white text-amber-800 shadow-2xs font-extrabold'
                      : 'bg-white text-[#2D8A7A] shadow-2xs font-extrabold'
                    : 'text-[#242826]/70 hover:text-[#10324E] hover:bg-white/60'
                }`}
              >
                <span>{item.label}</span>
              </a>
            ))}
          </nav>

          {/* Right Header Controls */}
          <div className="flex items-center gap-2 sm:gap-3 shrink-0">
            
            {/* Live JWT Session Timer */}
            <div className="flex items-center gap-1.5 px-2.5 py-1 sm:px-3 sm:py-1.5 rounded-xl bg-amber-50 border border-amber-200/90 text-[11px] sm:text-xs text-amber-900 shadow-2xs">
              <span className="font-bold">⏱️</span>
              <span className="font-mono font-bold">
                {tokenTimeRemaining !== null
                  ? `${Math.floor(tokenTimeRemaining / 60).toString().padStart(2, '0')}:${(tokenTimeRemaining % 60).toString().padStart(2, '0')}`
                  : '15:00'}
              </span>
              <button
                type="button"
                onClick={triggerExpireNow}
                title="Force session expiration to test automatic logout"
                className="hidden sm:inline-block ml-1 px-1.5 py-0.5 rounded-md bg-amber-200/70 hover:bg-amber-300 text-amber-900 font-extrabold text-[10px] cursor-pointer transition-colors"
              >
                Expire
              </button>
            </div>

            {/* Notification Bell */}
            <div className="relative">
              <button
                onClick={() => setShowNotifications(!showNotifications)}
                className="relative p-2.5 rounded-2xl bg-white hover:bg-slate-50 border border-[#10324E]/10 text-[#10324E] shadow-2xs transition cursor-pointer"
                aria-label="Notifications"
              >
                <Bell className="w-4 h-4" />
                <span className="absolute top-1 right-1 w-2 h-2 rounded-full bg-[#E99A7C] ring-2 ring-white" />
              </button>

              {showNotifications && (
                <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-3xl p-4 shadow-vetalya border border-[#10324E]/10 z-50 animate-in fade-in">
                  <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/10">
                    <h4 className="font-bold text-xs uppercase tracking-wider text-[#10324E]">Activity Feed</h4>
                    <span className="text-[10px] text-[#2D8A7A] font-bold bg-[#E0F2EB] px-2 py-0.5 rounded-full">3 New</span>
                  </div>
                  <div className="mt-3 space-y-2.5 max-h-72 overflow-y-auto">
                    {notifications.map((n) => (
                      <div key={n.id} className="p-3 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/5 hover:bg-white transition flex items-start gap-3">
                        <div className="w-7 h-7 rounded-xl bg-[#E0F2EB] text-[#2D8A7A] flex items-center justify-center shrink-0 mt-0.5">
                          <n.icon className="w-3.5 h-3.5" />
                        </div>
                        <div className="text-left flex-1 min-w-0">
                          <div className="flex justify-between items-center">
                            <p className="font-bold text-xs text-[#10324E]">{n.title}</p>
                            <span className="text-[10px] text-[#242826]/50">{n.time}</span>
                          </div>
                          <p className="text-[11px] text-[#242826]/75 mt-0.5 leading-snug">{n.text}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                  <button
                    onClick={() => setShowNotifications(false)}
                    className="w-full mt-3 py-2 text-center text-xs font-bold text-[#10324E] hover:text-[#2D8A7A] cursor-pointer"
                  >
                    Close
                  </button>
                </div>
              )}
            </div>

            {/* User Profile Pill & Dropdown */}
            <div className="relative">
              <button
                onClick={() => setShowProfileMenu(!showProfileMenu)}
                className="flex items-center gap-2 p-1.5 sm:px-3 sm:py-2 rounded-2xl bg-white hover:bg-slate-50 border border-[#10324E]/12 shadow-2xs transition cursor-pointer"
              >
                <img
                  src={currentUser?.avatar || "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=120&q=80"}
                  alt={currentUser?.name}
                  className="w-7 h-7 sm:w-8 sm:h-8 rounded-full object-cover ring-2 ring-[#2D8A7A]/30"
                />
                <span className="hidden sm:inline-block text-xs font-bold text-[#10324E] max-w-[120px] truncate">
                  {currentUser?.name || 'Account'}
                </span>
                <ChevronDown className="w-3.5 h-3.5 text-[#10324E]/60 hidden sm:inline" />
              </button>

              {showProfileMenu && (
                <div className="absolute right-0 mt-2 w-72 bg-white rounded-3xl p-3 shadow-vetalya border border-[#10324E]/10 z-50 animate-in fade-in">
                  <div className="p-3 rounded-2xl bg-[#F7F5EE] border border-[#10324E]/8 text-left mb-2">
                    <p className="font-extrabold text-xs text-[#10324E] truncate">{currentUser?.name}</p>
                    <p className="text-[11px] text-[#242826]/70 truncate">{currentUser?.email}</p>
                    <div className="mt-2 flex items-center justify-between text-[10px]">
                      <span className="text-[#242826]/60">Verified Role:</span>
                      <span className="font-bold text-[#2D8A7A] uppercase">{role}</span>
                    </div>
                  </div>

                  <div className="space-y-1">
                    <button
                      onClick={() => {
                        setShowProfileMenu(false);
                        setShowDbInspector(true);
                        handleFetchDb();
                      }}
                      className="w-full flex items-center gap-2 px-3 py-2 text-xs font-bold text-[#10324E] hover:bg-[#FAF8F5] rounded-xl transition cursor-pointer text-left"
                    >
                      <Database className="w-3.5 h-3.5 text-[#2D8A7A]" />
                      <span>Live Supabase Inspector</span>
                    </button>

                    <button
                      onClick={() => {
                        setShowProfileMenu(false);
                        triggerExpireNow();
                      }}
                      className="w-full flex items-center gap-2 px-3 py-2 text-xs font-bold text-amber-800 hover:bg-amber-50 rounded-xl transition cursor-pointer text-left"
                    >
                      <Clock className="w-3.5 h-3.5 text-amber-600" />
                      <span>Simulate Token Expiry</span>
                    </button>

                    <hr className="my-1 border-[#10324E]/8" />

                    <button
                      onClick={() => logout()}
                      className="w-full flex items-center gap-2 px-3 py-2 text-xs font-bold text-rose-700 hover:bg-rose-50 rounded-xl transition cursor-pointer text-left"
                    >
                      <LogOut className="w-3.5 h-3.5 text-rose-600" />
                      <span>Sign Out</span>
                    </button>
                  </div>
                </div>
              )}
            </div>

            {/* Mobile Hamburger Menu Toggle */}
            <button
              onClick={() => setShowMobileMenu(!showMobileMenu)}
              className="lg:hidden p-2.5 rounded-2xl bg-white border border-[#10324E]/10 text-[#10324E] cursor-pointer"
              aria-label="Toggle menu"
            >
              <Menu className="w-4 h-4" />
            </button>

          </div>
        </div>

        {/* Mobile Navigation Drawer */}
        {showMobileMenu && (
          <div className="lg:hidden px-4 py-3 bg-white border-t border-[#10324E]/10 space-y-2 text-xs font-bold animate-in slide-in-from-top-2">
            <p className="text-[10px] text-[#242826]/50 uppercase tracking-wider mb-1">
              {roleMeta.label} Menu
            </p>
            <div className="grid grid-cols-2 gap-2">
              {navItems.map((item) => (
                <a
                  key={item.label}
                  href={item.href}
                  onClick={() => setShowMobileMenu(false)}
                  className="py-2.5 px-3 rounded-xl bg-[#FAF8F5] border border-[#10324E]/8 text-[#10324E] hover:bg-[#E0F2EB] hover:text-[#2D8A7A] text-center transition"
                >
                  {item.label}
                </a>
              ))}
            </div>
          </div>
        )}
      </header>

      {/* ── Main Content Container ── */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
        {children}
      </main>

      {/* ── Live Database Inspector Modal ── */}
      {showDbInspector && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-3xl w-full p-6 shadow-vetalya border border-[#10324E]/10 max-h-[85vh] flex flex-col animate-in zoom-in-95">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10">
              <div className="flex items-center gap-2">
                <Database className="w-5 h-5 text-[#2D8A7A]" />
                <h3 className="text-base font-extrabold text-[#10324E]">Live Supabase Database Inspector</h3>
              </div>
              <button
                onClick={() => setShowDbInspector(false)}
                className="p-1.5 rounded-xl hover:bg-slate-100 text-[#242826]/70 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="py-4 flex-1 overflow-y-auto font-mono text-xs text-left">
              {loadingDb ? (
                <div className="p-8 text-center text-[#242826]/60">Querying live Supabase PostgreSQL...</div>
              ) : (
                <pre className="bg-[#FAF8F5] p-4 rounded-2xl border border-[#10324E]/10 overflow-x-auto text-[11px] leading-relaxed">
                  {JSON.stringify(dbData, null, 2)}
                </pre>
              )}
            </div>

            <div className="pt-3 border-t border-[#10324E]/10 flex justify-between items-center">
              <button
                onClick={handleFetchDb}
                className="px-4 py-2 rounded-xl bg-[#FAF8F5] hover:bg-slate-100 border border-[#10324E]/15 text-xs font-bold text-[#10324E] cursor-pointer"
              >
                Refresh Data
              </button>
              <button
                onClick={() => setShowDbInspector(false)}
                className="px-5 py-2 rounded-xl bg-[#10324E] text-white text-xs font-bold hover:bg-[#0c273e] cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Footer ── */}
      <footer className="mt-auto border-t border-[#10324E]/8 bg-white/70 py-6 text-center text-xs text-[#242826]/60">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>Vetalya Animal Healthcare Platform • Active Role: <strong className="text-[#10324E]">{role}</strong></span>
          <span className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            Supabase DB &amp; Spring Boot API Connected
          </span>
        </div>
      </footer>
    </div>
  );
}

import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import {
  X,
  Mail,
  CheckCircle2,
} from 'lucide-react';

import AuthIllustration from './AuthIllustration';
import AuthCard from './AuthCard';

/**
 * LoginPage
 * Production Authentication Page implementing the Vetalya Community Design Language:
 * - Two-column responsive canvas on warm porcelain background (#F7F5EE)
 * - Left column: Brand logo, "Where every pet parent belongs" copy, and heart emblem
 * - Right column: Consumer-facing single Login & Dual Role-Tailored Sign-Up
 * - Automatic role routing based on credentials
 */
export default function LoginPage() {
  const { sessionExpiredNotice, setSessionExpiredNotice } = useAuth();
  const [showForgot, setShowForgot] = useState(false);
  const [forgotEmail, setForgotEmail] = useState('');
  const [forgotSent, setForgotSent] = useState(false);

  const handleForgotSubmit = (e) => {
    e.preventDefault();
    if (!forgotEmail.trim()) return;
    setForgotSent(true);
    setTimeout(() => {
      setShowForgot(false);
      setForgotSent(false);
      setForgotEmail('');
    }, 2200);
  };

  return (
    <div className="min-h-screen bg-[#F7F5EE] text-[#242826] relative overflow-hidden flex flex-col justify-between selection:bg-[#10324E] selection:text-white">
      
      {/* ── Left Edge Peach Semi-Circle Graphic ── */}
      <div className="absolute top-[40%] -left-16 sm:-left-20 w-36 h-36 sm:w-44 sm:h-44 rounded-full bg-[#F49E78] pointer-events-none z-0" />

      {/* ── Soft Background Ambience ── */}
      <div className="absolute top-[-10%] right-[-5%] w-[450px] h-[450px] rounded-full bg-[#E0F2EB]/50 blur-3xl pointer-events-none" />

      {/* ── Session Expired Banner ── */}
      {sessionExpiredNotice && (
        <div className="relative z-30 max-w-xl mx-auto mt-6 px-4 py-3 rounded-2xl bg-amber-50 border border-amber-300 text-amber-900 text-xs flex items-center justify-between shadow-sm">
          <div className="flex items-center gap-2.5">
            <span className="text-lg">⏱️</span>
            <div>
              <p className="font-bold">Session Expired</p>
              <p className="text-[11px] text-amber-700">Your authentication token expired and you were automatically logged out.</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setSessionExpiredNotice(false)}
            className="p-1 px-2 rounded-lg hover:bg-amber-100 text-amber-800 font-bold text-xs cursor-pointer"
          >
            ✕
          </button>
        </div>
      )}

      {/* ── Main Two-Column Container ── */}
      <div className="max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-12 py-8 lg:py-16 my-auto relative z-10">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-8 items-center">
          
          {/* Left Column: Brand & Value Props (7 Cols on desktop) */}
          <div className="lg:col-span-7">
            <AuthIllustration />
          </div>

          {/* Right Column: Tabbed Log In / Role-Tailored Sign Up Card (5 Cols on desktop) */}
          <div className="lg:col-span-5 flex justify-center lg:justify-end">
            <AuthCard onToggleForgot={() => setShowForgot(true)} />
          </div>

        </div>
      </div>

      {/* ── Clean Consumer Footer ── */}
      <footer className="relative z-20 border-t border-[#10324E]/8 bg-white/60 py-4 text-center text-xs text-[#242826]/60">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>© 2026 Vetalya • Animal Healthcare &amp; Veterinary Platform</span>
          <span className="text-[11px] text-[#242826]/50">Every Pet. Health. Always.</span>
        </div>
      </footer>

      {/* ── Forgot Password Modal ── */}
      {showForgot && (
        <div className="fixed inset-0 z-50 bg-[#10324E]/70 backdrop-blur-md flex items-center justify-center p-4 animate-in fade-in">
          <div className="bg-white rounded-[32px] max-w-md w-full p-6 sm:p-8 shadow-2xl border border-[#10324E]/10 relative">
            <button
              onClick={() => {
                setShowForgot(false);
                setForgotSent(false);
              }}
              className="absolute top-5 right-5 w-8 h-8 rounded-full bg-[#F5F2EB] flex items-center justify-center text-[#10324E] hover:bg-[#EAE6DD] cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="w-12 h-12 rounded-2xl bg-[#DDEBDD] text-[#2D8A7A] flex items-center justify-center mb-4">
              <Mail className="w-6 h-6" />
            </div>

            <h3 className="text-xl font-extrabold text-[#10324E]">
              Reset your password
            </h3>
            <p className="text-xs text-[#242826]/65 mt-1 leading-relaxed">
              Enter your registered email address and we'll send a secure password reset link.
            </p>

            {forgotSent ? (
              <div className="mt-5 p-4 rounded-2xl bg-[#DDEBDD] text-[#173F35] text-xs font-bold flex items-center gap-2">
                <CheckCircle2 className="w-5 h-5 text-[#2D8A7A] shrink-0" />
                <span>Password reset link sent to {forgotEmail}!</span>
              </div>
            ) : (
              <form onSubmit={handleForgotSubmit} className="mt-5 space-y-4">
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1.5">
                    Email address
                  </label>
                  <input
                    type="email"
                    required
                    value={forgotEmail}
                    onChange={(e) => setForgotEmail(e.target.value)}
                    placeholder="you@example.com"
                    className="w-full px-4 py-3 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-sm text-[#242826] focus:outline-hidden focus:ring-2 focus:ring-[#2D8A7A]"
                  />
                </div>

                <button
                  type="submit"
                  className="w-full py-3.5 px-4 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white font-bold text-xs shadow-md transition-all cursor-pointer"
                >
                  Send Reset Link
                </button>
              </form>
            )}
          </div>
        </div>
      )}

    </div>
  );
}

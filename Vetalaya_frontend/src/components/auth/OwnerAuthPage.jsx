import React, { useState, useEffect, useRef } from 'react';
import {
  Heart,
  Mail,
  Lock,
  Eye,
  EyeOff,
  User,
  Phone,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  ShieldCheck,
  Sparkles
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import VetalyaBrandLogo from './VetalyaBrandLogo';

export default function OwnerAuthPage({ initialMode = 'login', onNavigate }) {
  const { login, signup, loginWithGoogle, isLoading } = useAuth();
  const [mode, setMode] = useState(initialMode); // 'login' | 'register'

  // Form Fields
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [petName, setPetName] = useState('');
  const [petSpecies, setPetSpecies] = useState('Canine');
  const [petBreed, setPetBreed] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [agreeTerms, setAgreeTerms] = useState(false);

  // Status & Error
  const [error, setError] = useState(null);
  const [successNotice, setSuccessNotice] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Google SSO Button
  const googleBtnRef = useRef(null);

  useEffect(() => {
    const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!googleClientId || googleClientId.includes('placeholder')) return;

    if (window.google?.accounts?.id && googleBtnRef.current) {
      try {
        window.google.accounts.id.initialize({
          client_id: googleClientId,
          callback: async (response) => {
            if (response.credential) {
              try {
                await loginWithGoogle(response.credential);
              } catch (err) {
                setError(err.message || 'Google authentication failed');
              }
            }
          },
          ux_mode: 'popup',
          auto_select: false,
        });

        window.google.accounts.id.renderButton(googleBtnRef.current, {
          type: 'standard',
          theme: 'outline',
          size: 'large',
          text: 'signin_with',
          shape: 'pill',
          width: '100%',
        });
      } catch (err) {
        console.warn('Google GIS init warning:', err);
      }
    }
  }, [loginWithGoogle, mode]);

  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessNotice(null);

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim() || !emailRegex.test(email.trim())) {
      setError('Please enter a valid email address.');
      return;
    }

    if (!password || password.length < 6) {
      setError('Password must contain at least 6 characters.');
      return;
    }

    if (mode === 'login') {
      setIsSubmitting(true);
      try {
        await login(email.trim(), password, true);
      } catch (err) {
        setError(err.message || 'Authentication failed. Please check your credentials.');
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    // Mode is 'register': Validate all pet parent fields
    if (!fullName.trim()) {
      setError('Please enter your full name.');
      return;
    }
    if (!phone.trim() || phone.trim().replace(/\D/g, '').length < 10) {
      setError('Please enter a valid 10-digit mobile number.');
      return;
    }
    if (!petName.trim()) {
      setError('Please enter your companion’s name.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    if (!agreeTerms) {
      setError('Please agree to the Vetalya Terms of Service.');
      return;
    }

    // Form inputs valid -> Directly register Pet Parent
    setIsSubmitting(true);
    try {
      const cleanEmail = email.trim().toLowerCase();
      const cleanPhone = phone.trim();

      await signup({
        fullName: fullName.trim(),
        email: cleanEmail,
        phone: cleanPhone,
        password,
        role: 'ROLE_PET_OWNER',
        petName: petName.trim(),
        petSpecies,
        petBreed: petBreed.trim() || 'Companion',
      });

      setSuccessNotice('Pet Parent account created! Redirecting to passport...');
    } catch (err) {
      setError(err.message || 'Registration failed. Please check your credentials or sign in.');
    } finally {
      setIsSubmitting(false);
    }
  };



  return (
    <div className="min-h-screen bg-[#F7F5EE] flex flex-col justify-between selection:bg-[#10324E] selection:text-white relative">
      
      {/* Top Navbar */}
      <header className="max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-5 flex items-center justify-between">
        <div onClick={() => onNavigate('/')} className="cursor-pointer">
          <VetalyaBrandLogo />
        </div>

        <div className="flex items-center space-x-3 text-xs font-bold">
          <button
            onClick={() => onNavigate('/vet/login')}
            className="text-[#10324E]/70 hover:text-[#10324E] transition-colors hidden sm:inline"
          >
            Are you a Veterinarian? Login to Clinic Desk
          </button>
          <button
            onClick={() => onNavigate('/')}
            className="px-3.5 py-1.5 rounded-xl bg-white border border-[#10324E]/10 text-[#10324E] hover:bg-slate-50 transition-colors shadow-2xs cursor-pointer flex items-center gap-1"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Home</span>
          </button>
        </div>
      </header>

      {/* Main Content Card */}
      <main className="max-w-md w-full mx-auto my-6 px-4 sm:px-0">
        <div className="bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left space-y-6">
          
          {/* Header Badge & Title */}
          <div className="space-y-2">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#E0F2EB] text-[#2D8A7A] text-[10px] font-extrabold uppercase tracking-widest">
              <Heart className="w-3.5 h-3.5" />
              <span>Pet Parent Portal</span>
            </div>
            <h1 className="text-2xl font-black text-[#10324E] tracking-tight">
              {mode === 'login'
                ? 'Welcome Back!'
                : step === 'form'
                ? 'Create Pet Passport'
                : 'Verify Contact Details'}
            </h1>
            <p className="text-xs text-[#242826]/70 leading-relaxed">
              {mode === 'login'
                ? 'Sign in to access your companion’s health records and reminders.'
                : step === 'form'
                ? 'Join Vetalya to manage lifelong vaccinations and emergency passports.'
                : 'Enter the 6-digit codes sent to your email and mobile phone.'}
            </p>
          </div>

          {/* Mode Switcher Pills */}
          {step === 'form' && (
            <div className="flex p-1 bg-[#F5F2EB] rounded-2xl">
              <button
                type="button"
                onClick={() => { setMode('login'); setError(null); setSuccessNotice(null); }}
                className={`flex-1 py-2.5 rounded-xl text-xs font-black transition-all cursor-pointer ${
                  mode === 'login'
                    ? 'bg-white text-[#10324E] shadow-2xs'
                    : 'text-[#242826]/60 hover:text-[#10324E]'
                }`}
              >
                Log in
              </button>
              <button
                type="button"
                onClick={() => { setMode('register'); setError(null); setSuccessNotice(null); }}
                className={`flex-1 py-2.5 rounded-xl text-xs font-black transition-all cursor-pointer ${
                  mode === 'register'
                    ? 'bg-white text-[#10324E] shadow-2xs'
                    : 'text-[#242826]/60 hover:text-[#10324E]'
                }`}
              >
                Sign up
              </button>
            </div>
          )}

          {/* Alert Notices */}
          {error && (
            <div className="p-3 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2 animate-in fade-in">
              <AlertCircle className="w-4 h-4 shrink-0 mt-0.5 text-rose-600" />
              <span>{error}</span>
            </div>
          )}

          {successNotice && (
            <div className="p-3 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs flex items-start gap-2 animate-in fade-in">
              <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5 text-emerald-600" />
              <span>{successNotice}</span>
            </div>
          )}

          {/* ── Main Form ── */}
          <form onSubmit={handleFormSubmit} className="space-y-4">
              
              {mode === 'register' && (
                <>
                  <div>
                    <label className="block text-[11px] font-bold text-[#10324E] mb-1">Your Full Name</label>
                    <div className="relative">
                      <User className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                      <input
                        type="text"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        placeholder="e.g. Rahul Sharma"
                        className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none focus:border-[#10324E] transition-colors"
                        required
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-[11px] font-bold text-[#10324E] mb-1">Mobile Phone Number (+91)</label>
                    <div className="relative">
                      <Phone className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                      <input
                        type="tel"
                        value={phone}
                        onChange={(e) => setPhone(e.target.value)}
                        placeholder="98765 43210"
                        className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none focus:border-[#10324E] transition-colors"
                        required
                      />
                    </div>
                  </div>

                  <div className="p-3 rounded-2xl bg-[#F5F2EB]/60 border border-[#10324E]/8 space-y-3">
                    <div className="text-[10px] font-black uppercase tracking-wider text-[#2D8A7A]">
                      Initial Companion Details
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                      <div>
                        <input
                          type="text"
                          value={petName}
                          onChange={(e) => setPetName(e.target.value)}
                          placeholder="Pet Name (e.g. Leo)"
                          className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/10 text-xs focus:outline-none"
                          required
                        />
                      </div>
                      <div>
                        <select
                          value={petSpecies}
                          onChange={(e) => setPetSpecies(e.target.value)}
                          className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/10 text-xs focus:outline-none"
                        >
                          <option value="Canine">Canine (Dog)</option>
                          <option value="Feline">Feline (Cat)</option>
                          <option value="Avian">Avian (Bird)</option>
                          <option value="Exotic">Exotic</option>
                        </select>
                      </div>
                    </div>
                    <input
                      type="text"
                      value={petBreed}
                      onChange={(e) => setPetBreed(e.target.value)}
                      placeholder="Breed (e.g. Golden Retriever, Indie, Persian)"
                      className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/10 text-xs focus:outline-none"
                    />
                  </div>
                </>
              )}

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">
                  {mode === 'login' ? 'Email or Mobile Number' : 'Email Address'}
                </label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                  <input
                    type="text"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="parent@example.com"
                    className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none focus:border-[#10324E] transition-colors"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Password</label>
                <div className="relative">
                  <Lock className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    className="w-full pl-10 pr-10 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none focus:border-[#10324E] transition-colors"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3.5 top-3 text-[#242826]/40 hover:text-[#10324E] cursor-pointer"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {mode === 'register' && (
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Confirm Password</label>
                  <div className="relative">
                    <Lock className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                    <input
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none focus:border-[#10324E] transition-colors"
                      required
                    />
                  </div>
                </div>
              )}

              {mode === 'register' && (
                <label className="flex items-start gap-2 pt-1 text-[11px] text-[#242826]/75 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={agreeTerms}
                    onChange={(e) => setAgreeTerms(e.target.checked)}
                    className="mt-0.5 rounded border-slate-300 text-[#10324E] focus:ring-0"
                  />
                  <span>I agree to the Vetalya Privacy Policy and Clinical Data Terms.</span>
                </label>
              )}

              <button
                type="submit"
                disabled={isSubmitting || isLoading}
                className="w-full py-3.5 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white font-extrabold text-xs shadow-md transition-all cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {isSubmitting ? (
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                ) : (
                  <>
                    <span>{mode === 'login' ? 'Sign In to Pet Passport' : 'Create Pet Parent Account & Get Pet ID →'}</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </form>

          {/* Google GIS One-Tap */}
          {mode === 'login' && (
            <div className="space-y-3 pt-2">
              <div className="relative flex items-center justify-center">
                <div className="w-full border-t border-[#10324E]/10" />
                <span className="bg-white px-3 text-[10px] uppercase font-bold text-[#242826]/40 relative">
                  Or continue with
                </span>
              </div>
              <div ref={googleBtnRef} className="w-full flex justify-center" />
            </div>
          )}

          {/* Quick Role Switcher Footer */}
          <div className="pt-2 text-center text-[11px] text-[#242826]/60">
            <span>Are you a licensed veterinarian? </span>
            <button
              onClick={() => onNavigate('/vet/login')}
              className="font-bold text-[#10324E] hover:underline"
            >
              Login to Clinic Desk →
            </button>
          </div>

        </div>
      </main>

      {/* Footer */}
      <footer className="text-center py-4 text-xs text-[#242826]/50">
        © 2026 Vetalya Health Technologies • Pet Parent Portal
      </footer>

    </div>
  );
}

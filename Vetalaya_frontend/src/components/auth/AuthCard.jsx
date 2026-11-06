import React, { useState, useEffect, useRef } from 'react';
import {
  Eye,
  EyeOff,
  AlertCircle,
  CheckCircle2,
  Heart,
  Stethoscope,
  ShieldCheck,
  Building2,
  Lock,
  Mail,
  User,
  Phone,
  Award,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';

export default function AuthCard({ onToggleForgot }) {
  const { login, signup, loginWithGoogle, isLoading } = useAuth();

  // Mode: 'login' | 'signup'
  const [mode, setMode] = useState('login');

  // Sign-up Role Choice: 'PET_OWNER' | 'VET'
  const [signupRole, setSignupRole] = useState('PET_OWNER');

  // Shared Form Fields
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(true);

  // Pet Parent Specific Fields
  const [parentName, setParentName] = useState('');
  const [parentPhone, setParentPhone] = useState('');
  const [petName, setPetName] = useState('');
  const [petSpecies, setPetSpecies] = useState('Canine');
  const [petBreed, setPetBreed] = useState('');
  const [agreeTermsParent, setAgreeTermsParent] = useState(false);

  // Vet Specific Fields
  const [vetName, setVetName] = useState('');
  const [vetPhone, setVetPhone] = useState('');
  const [councilRegNo, setCouncilRegNo] = useState('');
  const [councilState, setCouncilState] = useState('Karnataka Veterinary Council (KVC)');
  const [degree, setDegree] = useState('BVSc & AH');
  const [clinicName, setClinicName] = useState('');
  const [agreeCouncilVet, setAgreeCouncilVet] = useState(false);

  // Google SSO
  const googleButtonContainerRef = useRef(null);

  // Status & Error
  const [error, setError] = useState(null);
  const [successNotice, setSuccessNotice] = useState(null);

  // Initialize official Google Sign-In button
  useEffect(() => {
    const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID;
    if (!googleClientId || googleClientId.includes('placeholder')) return;

    let timer;
    let isSubscribed = true;

    const setupGoogleSignIn = () => {
      if (!isSubscribed) return;

      if (window.google?.accounts?.id && googleButtonContainerRef.current) {
        try {
          window.google.accounts.id.initialize({
            client_id: googleClientId,
            callback: async (response) => {
              if (response.credential) {
                try {
                  const roleToUse = mode === 'signup' && signupRole === 'VET' ? 'ROLE_VET' : 'ROLE_PET_OWNER';
                  await loginWithGoogle(response.credential, roleToUse);
                } catch (err) {
                  setError(err.message || 'Google verification failed.');
                }
              }
            },
          });

          const container = googleButtonContainerRef.current;
          if (container && document.body.contains(container)) {
            container.innerHTML = '';
            window.google.accounts.id.renderButton(container, {
              theme: 'outline',
              size: 'large',
              type: 'standard',
              shape: 'pill',
              text: mode === 'login' ? 'signin_with' : 'signup_with',
              logo_alignment: 'left',
              width: 320,
            });
          }
        } catch (e) {
          console.warn('Google Identity Services setup warning:', e);
        }
      } else {
        timer = setTimeout(setupGoogleSignIn, 150);
      }
    };

    setupGoogleSignIn();

    return () => {
      isSubscribed = false;
      if (timer) clearTimeout(timer);
    };
  }, [mode, signupRole, loginWithGoogle]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessNotice(null);

    // Basic Validation
    if (!email.trim() || !email.includes('@')) {
      setError('Please enter a valid email address.');
      return;
    }

    if (!password || password.length < 6) {
      setError('Password must be at least 6 characters.');
      return;
    }

    try {
      if (mode === 'login') {
        // Single unified login: credential verifies role and routes automatically
        await login(email, password, rememberMe);
      } else {
        // Sign-up flow
        if (password !== confirmPassword) {
          setError('Passwords do not match. Please verify.');
          return;
        }

        if (signupRole === 'PET_OWNER') {
          if (!parentName.trim()) {
            setError('Please enter your full name.');
            return;
          }
          if (!agreeTermsParent) {
            setError('Please accept the Terms of Service & Privacy Policy.');
            return;
          }

          const payload = {
            fullName: parentName.trim(),
            email: email.trim(),
            password: password,
            phone: parentPhone.trim() || null,
            role: 'ROLE_PET_OWNER',
            petName: petName.trim() || null,
            petSpecies: petSpecies,
            petBreed: petBreed.trim() || null,
          };
          await signup(payload);
          setSuccessNotice('Pet Parent account created! Redirecting to your passport portal...');
        } else {
          // Vet Sign-up
          if (!vetName.trim()) {
            setError('Please enter your doctor full name.');
            return;
          }
          if (!councilRegNo.trim()) {
            setError('Please provide your State/National Veterinary Council Registration Number.');
            return;
          }
          if (!agreeCouncilVet) {
            setError('Please confirm the Veterinary Council licensing declaration.');
            return;
          }

          const formattedDoctorName = vetName.trim().startsWith('Dr.') ? vetName.trim() : `Dr. ${vetName.trim()}`;
          const payload = {
            fullName: formattedDoctorName,
            email: email.trim(),
            password: password,
            phone: vetPhone.trim() || null,
            role: 'ROLE_VET',
            councilRegNumber: councilRegNo.trim(),
            councilState: councilState,
            qualification: degree,
            clinicName: clinicName.trim() || 'Independent Clinic',
          };
          await signup(payload);
          setSuccessNotice('Veterinary Doctor account registered! Redirecting to OPD desk...');
        }
      }
    } catch (err) {
      setError(err.message || 'Authentication failed. Please check your credentials.');
    }
  };

  return (
    <div className="w-full max-w-[440px] bg-white rounded-[32px] p-6 sm:p-8 shadow-vetalya border border-[#10324E]/8 text-left transition-all">
      
      {/* ── Mode Pill Switcher (Log in / Sign up) ── */}
      <div className="flex p-1 bg-[#F5F2EB] rounded-2xl mb-6">
        <button
          type="button"
          onClick={() => {
            setMode('login');
            setError(null);
            setSuccessNotice(null);
          }}
          className={`flex-1 py-2.5 rounded-xl text-xs font-extrabold transition-all cursor-pointer ${
            mode === 'login'
              ? 'bg-white text-[#10324E] shadow-2xs'
              : 'text-[#242826]/60 hover:text-[#10324E]'
          }`}
        >
          Log in
        </button>
        <button
          type="button"
          onClick={() => {
            setMode('signup');
            setError(null);
            setSuccessNotice(null);
          }}
          className={`flex-1 py-2.5 rounded-xl text-xs font-extrabold transition-all cursor-pointer ${
            mode === 'signup'
              ? 'bg-white text-[#10324E] shadow-2xs'
              : 'text-[#242826]/60 hover:text-[#10324E]'
          }`}
        >
          Sign up
        </button>
      </div>

      {/* ── Sign-up Role Tailoring Selector (Shown only when in Sign up mode) ── */}
      {mode === 'signup' && (
        <div className="mb-6 space-y-2">
          <label className="block text-[11px] font-extrabold text-[#10324E] uppercase tracking-wider">
            Choose your account type
          </label>
          <div className="grid grid-cols-2 gap-2.5">
            <button
              type="button"
              onClick={() => {
                setSignupRole('PET_OWNER');
                setError(null);
              }}
              className={`p-3 rounded-2xl border text-left transition-all cursor-pointer ${
                signupRole === 'PET_OWNER'
                  ? 'border-[#2D8A7A] bg-[#E0F2EB]/50 ring-2 ring-[#2D8A7A]/20'
                  : 'border-[#10324E]/10 bg-[#FAF8F5] hover:bg-white'
              }`}
            >
              <div className="flex items-center gap-1.5 text-xs font-extrabold text-[#10324E]">
                <Heart className="w-3.5 h-3.5 text-[#2D8A7A]" />
                <span>Pet Parent</span>
              </div>
              <p className="text-[10px] text-[#242826]/65 mt-0.5 leading-tight">
                Health passport &amp; consults
              </p>
            </button>

            <button
              type="button"
              onClick={() => {
                setSignupRole('VET');
                setError(null);
              }}
              className={`p-3 rounded-2xl border text-left transition-all cursor-pointer ${
                signupRole === 'VET'
                  ? 'border-[#10324E] bg-[#E8EEF4]/60 ring-2 ring-[#10324E]/20'
                  : 'border-[#10324E]/10 bg-[#FAF8F5] hover:bg-white'
              }`}
            >
              <div className="flex items-center gap-1.5 text-xs font-extrabold text-[#10324E]">
                <Stethoscope className="w-3.5 h-3.5 text-[#10324E]" />
                <span>Veterinarian</span>
              </div>
              <p className="text-[10px] text-[#242826]/65 mt-0.5 leading-tight">
                OPD queue &amp; digital Rx
              </p>
            </button>
          </div>
        </div>
      )}

      {/* Header Titles */}
      <div className="mb-5">
        <h2 className="text-xl sm:text-2xl font-extrabold text-[#10324E] font-display">
          {mode === 'login'
            ? 'Sign in to your account'
            : signupRole === 'PET_OWNER'
            ? 'Create Pet Parent Account'
            : 'Register Licensed Veterinarian'}
        </h2>
        <p className="text-xs text-[#242826]/70 mt-1">
          {mode === 'login'
            ? 'Enter your credentials to access your personalized portal.'
            : signupRole === 'PET_OWNER'
            ? 'Join our community for digital health passports and trusted pet care.'
            : 'Access OPD queues, patient histories, and signed digital prescriptions.'}
        </p>
      </div>

      {/* Alert Notices */}
      {error && (
        <div className="mb-4 p-3 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-start gap-2 animate-in fade-in">
          <AlertCircle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {successNotice && (
        <div className="mb-4 p-3 rounded-2xl bg-[#E0F2EB] border border-[#2D8A7A]/30 text-[#173F35] text-xs font-bold flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 className="w-4 h-4 text-[#2D8A7A] shrink-0" />
          <span>{successNotice}</span>
        </div>
      )}

      {/* Main Form */}
      <form onSubmit={handleSubmit} className="space-y-3.5">
        
        {/* ── Pet Parent Fields ── */}
        {mode === 'signup' && signupRole === 'PET_OWNER' && (
          <>
            <div>
              <label className="block text-xs font-bold text-[#10324E] mb-1">Full Name *</label>
              <div className="relative">
                <User className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
                <input
                  type="text"
                  required
                  value={parentName}
                  onChange={(e) => setParentName(e.target.value)}
                  placeholder="e.g. Rahul Sharma"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#242826] focus:outline-hidden focus:ring-2 focus:ring-[#2D8A7A]"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-[#10324E] mb-1">Mobile Phone (Optional)</label>
              <div className="relative">
                <Phone className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
                <input
                  type="tel"
                  value={parentPhone}
                  onChange={(e) => setParentPhone(e.target.value)}
                  placeholder="+91 98765 43210"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#242826] focus:outline-hidden focus:ring-2 focus:ring-[#2D8A7A]"
                />
              </div>
            </div>

            {/* Companion Quick Add (Optional) */}
            <div className="p-3.5 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-2">
              <span className="text-[10px] font-extrabold uppercase text-[#2D8A7A] tracking-wider block">
                First Companion (Optional)
              </span>
              <div className="grid grid-cols-2 gap-2">
                <input
                  type="text"
                  value={petName}
                  onChange={(e) => setPetName(e.target.value)}
                  placeholder="Pet Name (e.g. Leo)"
                  className="p-2 rounded-xl bg-white border border-[#10324E]/10 text-xs"
                />
                <select
                  value={petSpecies}
                  onChange={(e) => setPetSpecies(e.target.value)}
                  className="p-2 rounded-xl bg-white border border-[#10324E]/10 text-xs font-bold"
                >
                  <option value="Canine">Dog (Canine)</option>
                  <option value="Feline">Cat (Feline)</option>
                  <option value="Avian">Bird (Avian)</option>
                  <option value="Other">Other</option>
                </select>
              </div>
            </div>
          </>
        )}

        {/* ── Veterinarian Professional Fields ── */}
        {mode === 'signup' && signupRole === 'VET' && (
          <>
            <div>
              <label className="block text-xs font-bold text-[#10324E] mb-1">Doctor Full Name *</label>
              <div className="relative">
                <User className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
                <input
                  type="text"
                  required
                  value={vetName}
                  onChange={(e) => setVetName(e.target.value)}
                  placeholder="e.g. Priya Sharma"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#242826] focus:outline-hidden focus:ring-2 focus:ring-[#10324E]"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Council Reg No *</label>
                <input
                  type="text"
                  required
                  value={councilRegNo}
                  onChange={(e) => setCouncilRegNo(e.target.value)}
                  placeholder="e.g. KVC-2022-8192"
                  className="w-full px-3 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#242826] font-mono"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Qualification *</label>
                <select
                  value={degree}
                  onChange={(e) => setDegree(e.target.value)}
                  className="w-full px-2 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs font-bold text-[#10324E]"
                >
                  <option value="BVSc & AH">BVSc &amp; AH</option>
                  <option value="MVSc (Surgery)">MVSc (Surgery)</option>
                  <option value="MVSc (Medicine)">MVSc (Medicine)</option>
                  <option value="PhD Veterinary">PhD Veterinary</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-[#10324E] mb-1">State Council</label>
              <select
                value={councilState}
                onChange={(e) => setCouncilState(e.target.value)}
                className="w-full px-3 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#10324E]"
              >
                <option value="Karnataka Veterinary Council (KVC)">Karnataka Veterinary Council (KVC)</option>
                <option value="Maharashtra State Veterinary Council">Maharashtra State Veterinary Council</option>
                <option value="Delhi Veterinary Council">Delhi Veterinary Council</option>
                <option value="Tamil Nadu Veterinary Council">Tamil Nadu Veterinary Council</option>
                <option value="Veterinary Council of India (National)">Veterinary Council of India (National)</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-bold text-[#10324E] mb-1">Clinic / Hospital Affiliation</label>
              <div className="relative">
                <Building2 className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
                <input
                  type="text"
                  value={clinicName}
                  onChange={(e) => setClinicName(e.target.value)}
                  placeholder="e.g. Koramangala Companion Hospital"
                  className="w-full pl-10 pr-3.5 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#242826]"
                />
              </div>
            </div>
          </>
        )}

        {/* ── Universal Email Field ── */}
        <div>
          <label className="block text-xs font-bold text-[#10324E] mb-1">Email Address *</label>
          <div className="relative">
            <Mail className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="name@example.com"
              className="w-full pl-10 pr-3.5 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#242826] focus:outline-hidden focus:ring-2 focus:ring-[#2D8A7A]"
            />
          </div>
        </div>

        {/* ── Universal Password Field ── */}
        <div>
          <label className="block text-xs font-bold text-[#10324E] mb-1">Password *</label>
          <div className="relative">
            <Lock className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
            <input
              type={showPassword ? 'text' : 'password'}
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="••••••••"
              className="w-full pl-10 pr-10 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#242826] focus:outline-hidden focus:ring-2 focus:ring-[#2D8A7A]"
            />
            <button
              type="button"
              onClick={() => setShowPassword(!showPassword)}
              className="absolute right-3 top-2.5 text-[#242826]/40 hover:text-[#10324E] cursor-pointer"
            >
              {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Confirm Password (Sign-up only) */}
        {mode === 'signup' && (
          <div>
            <label className="block text-xs font-bold text-[#10324E] mb-1">Confirm Password *</label>
            <div className="relative">
              <Lock className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
              <input
                type={showPassword ? 'text' : 'password'}
                required
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full pl-10 pr-3.5 py-2.5 rounded-2xl border border-[#E5E2D8] bg-[#FAF8F5] text-xs text-[#242826] focus:outline-hidden focus:ring-2 focus:ring-[#2D8A7A]"
              />
            </div>
          </div>
        )}

        {/* ── Login Checkboxes & Forgot Password ── */}
        {mode === 'login' && (
          <div className="flex items-center justify-between text-xs pt-1">
            <label className="flex items-center space-x-2 text-[#242826]/75 cursor-pointer">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="rounded border-[#E5E2D8] text-[#2D8A7A] focus:ring-[#2D8A7A]"
              />
              <span className="text-[11px]">Remember me</span>
            </label>
            <button
              type="button"
              onClick={onToggleForgot}
              className="text-[11px] font-bold text-[#10324E] hover:text-[#2D8A7A] hover:underline cursor-pointer"
            >
              Forgot password?
            </button>
          </div>
        )}

        {/* ── Sign-up Agreement Checkboxes ── */}
        {mode === 'signup' && signupRole === 'PET_OWNER' && (
          <label className="flex items-start space-x-2 text-[11px] text-[#242826]/75 cursor-pointer pt-1">
            <input
              type="checkbox"
              required
              checked={agreeTermsParent}
              onChange={(e) => setAgreeTermsParent(e.target.checked)}
              className="mt-0.5 rounded border-[#E5E2D8] text-[#2D8A7A] focus:ring-[#2D8A7A]"
            />
            <span>I agree to Vetalya's Terms of Service and Privacy Policy.</span>
          </label>
        )}

        {mode === 'signup' && signupRole === 'VET' && (
          <label className="flex items-start space-x-2 text-[11px] text-[#242826]/75 cursor-pointer pt-1">
            <input
              type="checkbox"
              required
              checked={agreeCouncilVet}
              onChange={(e) => setAgreeCouncilVet(e.target.checked)}
              className="mt-0.5 rounded border-[#E5E2D8] text-[#10324E] focus:ring-[#10324E]"
            />
            <span>I certify that I am a registered Veterinary Practitioner with the Veterinary Council.</span>
          </label>
        )}

        {/* Submit Button */}
        <button
          type="submit"
          disabled={isLoading}
          className="w-full py-3.5 px-4 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white font-bold text-xs shadow-md transition-all cursor-pointer disabled:opacity-50 mt-2"
        >
          {isLoading
            ? 'Authenticating...'
            : mode === 'login'
            ? 'Sign In'
            : signupRole === 'PET_OWNER'
            ? 'Create Pet Parent Account'
            : 'Register Licensed Veterinarian'}
        </button>
      </form>

      {/* ── Divider ── */}
      <div className="relative my-5">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-[#10324E]/8" />
        </div>
        <div className="relative flex justify-center text-[10px] uppercase font-bold text-[#242826]/40">
          <span className="bg-white px-2">or continue with</span>
        </div>
      </div>

      {/* ── Official Google Sign-In Button Container ── */}
      <div className="flex justify-center min-h-[44px]">
        <div ref={googleButtonContainerRef} />
      </div>

      {/* ── Bottom Switch Link ── */}
      <div className="mt-5 text-center text-xs text-[#242826]/70">
        {mode === 'login' ? (
          <p>
            Don't have an account yet?{' '}
            <button
              type="button"
              onClick={() => {
                setMode('signup');
                setError(null);
              }}
              className="font-extrabold text-[#2D8A7A] hover:underline cursor-pointer"
            >
              Sign up here
            </button>
          </p>
        ) : (
          <p>
            Already registered on Vetalya?{' '}
            <button
              type="button"
              onClick={() => {
                setMode('login');
                setError(null);
              }}
              className="font-extrabold text-[#10324E] hover:underline cursor-pointer"
            >
              Sign in
            </button>
          </p>
        )}
      </div>

    </div>
  );
}

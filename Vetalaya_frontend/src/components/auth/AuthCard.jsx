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
  ArrowLeft,
  Smartphone,
  KeyRound,
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { sendOtpApi, verifyOtpApi } from '../../services/api';

export default function AuthCard({ onToggleForgot }) {
  const { login, signup, loginWithGoogle, isLoading } = useAuth();

  // Mode: 'login' | 'signup'
  const [mode, setMode] = useState('login');

  // Sign-up Role Choice: 'PET_OWNER' | 'VET'
  const [signupRole, setSignupRole] = useState('PET_OWNER');

  // Sign-up Step: 'form' | 'otp_verify'
  const [signupStep, setSignupStep] = useState('form');

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

  // Dual OTP Verification States
  const [emailOtp, setEmailOtp] = useState('');
  const [phoneOtp, setPhoneOtp] = useState('');
  const [emailVerified, setEmailVerified] = useState(false);
  const [phoneVerified, setPhoneVerified] = useState(false);
  const [emailDevOtp, setEmailDevOtp] = useState(null);
  const [phoneDevOtp, setPhoneDevOtp] = useState(null);
  const [isSendingOtp, setIsSendingOtp] = useState(false);
  const [isVerifyingEmail, setIsVerifyingEmail] = useState(false);
  const [isVerifyingPhone, setIsVerifyingPhone] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);

  // Google SSO
  const googleButtonContainerRef = useRef(null);

  // Status & Error
  const [error, setError] = useState(null);
  const [successNotice, setSuccessNotice] = useState(null);

  // Countdown timer for OTP resend
  useEffect(() => {
    let interval;
    if (resendTimer > 0) {
      interval = setInterval(() => {
        setResendTimer((prev) => Math.max(0, prev - 1));
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [resendTimer]);

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
            ux_mode: 'popup',
            auto_select: false,
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

  // Validate form details and dispatch dual OTPs (Email + Mobile)
  const handleProceedToOtp = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessNotice(null);

    // Standard RFC-compliant Email Validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim() || !emailRegex.test(email.trim())) {
      setError('Please enter a valid email address (e.g., doctor@vetalya.com).');
      return;
    }

    if (!password || password.length < 6) {
      setError('Password must contain at least 6 characters.');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match. Please re-enter your password.');
      return;
    }

    const phoneRegex = /^[+]?[\d\s-]{10,15}$/;
    const activePhone = signupRole === 'PET_OWNER' ? parentPhone.trim() : vetPhone.trim();

    if (!activePhone || !phoneRegex.test(activePhone) || activePhone.replace(/\D/g, '').length < 10) {
      setError('Please enter a valid 10-digit mobile phone number for OTP verification.');
      return;
    }

    if (signupRole === 'PET_OWNER') {
      if (!parentName.trim() || parentName.trim().length < 2) {
        setError('Please enter your full legal name (minimum 2 characters).');
        return;
      }
      if (!agreeTermsParent) {
        setError('Please accept the Terms of Service & Privacy Policy to proceed.');
        return;
      }
    } else {
      if (!vetName.trim() || vetName.trim().length < 2) {
        setError('Please enter your doctor name (minimum 2 characters).');
        return;
      }
      if (!councilRegNo.trim() || councilRegNo.trim().length < 3) {
        setError('Please provide a valid State/National Veterinary Council Registration Number.');
        return;
      }
      if (!agreeCouncilVet) {
        setError('Please certify your Veterinary Council licensure declaration.');
        return;
      }
    }

    // Form inputs valid -> Dispatch OTP to both Email and Mobile Phone
    setIsSendingOtp(true);
    try {
      const cleanEmail = email.trim().toLowerCase();
      const [emailRes, phoneRes] = await Promise.all([
        sendOtpApi(cleanEmail, 'REGISTRATION'),
        sendOtpApi(activePhone, 'REGISTRATION'),
      ]);

      if (emailRes.devOtp) setEmailDevOtp(emailRes.devOtp);
      if (phoneRes.devOtp) setPhoneDevOtp(phoneRes.devOtp);

      setEmailVerified(false);
      setPhoneVerified(false);
      setEmailOtp('');
      setPhoneOtp('');
      setResendTimer(60);
      setSignupStep('otp_verify');
      setSuccessNotice(`6-digit verification codes sent to ${cleanEmail} and ${activePhone}!`);
    } catch (err) {
      setError(err.message || 'Failed to dispatch verification OTPs. Please check your credentials.');
    } finally {
      setIsSendingOtp(false);
    }
  };

  // Verify Email OTP individually
  const handleVerifyEmailOtp = async () => {
    if (!emailOtp || emailOtp.trim().length !== 6) {
      setError('Please enter the 6-digit OTP code sent to your email.');
      return;
    }
    setError(null);
    setIsVerifyingEmail(true);
    try {
      await verifyOtpApi(email.trim().toLowerCase(), emailOtp.trim(), 'REGISTRATION');
      setEmailVerified(true);
      setSuccessNotice('Email address successfully verified!');
    } catch (err) {
      setError(err.message || 'Invalid or expired email OTP code. Please try again.');
    } finally {
      setIsVerifyingEmail(false);
    }
  };

  // Verify Mobile OTP individually
  const handleVerifyPhoneOtp = async () => {
    const activePhone = signupRole === 'PET_OWNER' ? parentPhone.trim() : vetPhone.trim();
    if (!phoneOtp || phoneOtp.trim().length !== 6) {
      setError('Please enter the 6-digit OTP code sent to your mobile phone.');
      return;
    }
    setError(null);
    setIsVerifyingPhone(true);
    try {
      await verifyOtpApi(activePhone, phoneOtp.trim(), 'REGISTRATION');
      setPhoneVerified(true);
      setSuccessNotice('Mobile phone number successfully verified!');
    } catch (err) {
      setError(err.message || 'Invalid or expired mobile OTP code. Please try again.');
    } finally {
      setIsVerifyingPhone(false);
    }
  };

  // Resend Email OTP
  const handleResendEmailOtp = async () => {
    setError(null);
    setIsSendingOtp(true);
    try {
      const res = await sendOtpApi(email.trim().toLowerCase(), 'REGISTRATION');
      if (res.devOtp) setEmailDevOtp(res.devOtp);
      setResendTimer(60);
      setSuccessNotice(`New verification OTP sent to ${email.trim()}`);
    } catch (err) {
      setError(err.message || 'Failed to resend email OTP');
    } finally {
      setIsSendingOtp(false);
    }
  };

  // Resend Mobile OTP
  const handleResendPhoneOtp = async () => {
    const activePhone = signupRole === 'PET_OWNER' ? parentPhone.trim() : vetPhone.trim();
    setError(null);
    setIsSendingOtp(true);
    try {
      const res = await sendOtpApi(activePhone, 'REGISTRATION');
      if (res.devOtp) setPhoneDevOtp(res.devOtp);
      setResendTimer(60);
      setSuccessNotice(`New verification OTP sent to ${activePhone}`);
    } catch (err) {
      setError(err.message || 'Failed to resend mobile OTP');
    } finally {
      setIsSendingOtp(false);
    }
  };

  // Complete Registration with verified OTPs
  const handleCompleteRegistration = async () => {
    setError(null);
    setSuccessNotice(null);

    const activePhone = signupRole === 'PET_OWNER' ? parentPhone.trim() : vetPhone.trim();
    const cleanEmail = email.trim().toLowerCase();

    if (!emailVerified && (!emailOtp || emailOtp.trim().length !== 6)) {
      setError('Please enter or verify the 6-digit OTP sent to your email.');
      return;
    }

    if (!phoneVerified && (!phoneOtp || phoneOtp.trim().length !== 6)) {
      setError('Please enter or verify the 6-digit OTP sent to your mobile phone.');
      return;
    }

    try {
      const payload = {
        fullName: signupRole === 'PET_OWNER'
          ? parentName.trim()
          : (vetName.trim().startsWith('Dr.') ? vetName.trim() : `Dr. ${vetName.trim()}`),
        email: cleanEmail,
        password: password,
        phone: activePhone,
        role: signupRole === 'PET_OWNER' ? 'ROLE_PET_OWNER' : 'ROLE_VET',
        emailOtp: emailOtp.trim(),
        phoneOtp: phoneOtp.trim(),
        ...(signupRole === 'PET_OWNER' ? {
          petName: petName.trim() || null,
          petSpecies: petSpecies,
          petBreed: petBreed.trim() || null,
        } : {
          councilRegNumber: councilRegNo.trim(),
          councilState: councilState,
          qualification: degree,
          clinicName: clinicName.trim() || 'Independent Clinic',
        }),
      };

      await signup(payload);
      setSuccessNotice('Account successfully verified & created! Redirecting to your portal...');
    } catch (err) {
      setError(err.message || 'Registration failed. Please check your verification codes.');
    }
  };

  // Standard Login Submission
  const handleLoginSubmit = async (e) => {
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

    try {
      await login(email.trim(), password, rememberMe);
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
            setSignupStep('form');
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
            setSignupStep('form');
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

      {/* ── Sign-up Role Tailoring Selector (Shown only when in Sign up mode and on form step) ── */}
      {mode === 'signup' && signupStep === 'form' && (
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
      {signupStep === 'form' && (
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
      )}

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

      {/* ── STEP 2: Dual OTP Verification Screen ── */}
      {mode === 'signup' && signupStep === 'otp_verify' ? (
        <div className="space-y-4 animate-in fade-in">
          {/* Step Navigation Header */}
          <div className="flex items-center justify-between pb-2 border-b border-[#10324E]/8">
            <button
              type="button"
              onClick={() => {
                setSignupStep('form');
                setError(null);
              }}
              className="flex items-center gap-1.5 text-xs font-extrabold text-[#10324E] hover:text-[#2D8A7A] transition-colors cursor-pointer"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back to Edit</span>
            </button>
            <span className="text-[10px] font-extrabold px-2.5 py-1 rounded-full bg-[#E0F2EB] text-[#2D8A7A] uppercase tracking-wider">
              Step 2 of 2
            </span>
          </div>

          <div>
            <h3 className="text-lg font-extrabold text-[#10324E] font-display flex items-center gap-2">
              <ShieldCheck className="w-5 h-5 text-[#2D8A7A]" />
              Verify Email &amp; Mobile Phone
            </h3>
            <p className="text-xs text-[#242826]/70 mt-1">
              Enter the 6-digit OTP verification codes sent to confirm your email and phone number.
            </p>
          </div>

          {/* Dev/Simulated Gateway Codes Display */}
          {(emailDevOtp || phoneDevOtp) && (
            <div className="p-3.5 rounded-2xl bg-amber-50/90 border border-amber-200/80 text-amber-900 text-xs space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-extrabold text-[11px] uppercase tracking-wider text-amber-800 flex items-center gap-1.5">
                  <span>🧪</span> Simulated Gateway OTPs
                </span>
                <button
                  type="button"
                  onClick={() => {
                    if (emailDevOtp) setEmailOtp(emailDevOtp);
                    if (phoneDevOtp) setPhoneOtp(phoneDevOtp);
                    setError(null);
                  }}
                  className="text-[11px] font-extrabold text-amber-800 hover:text-amber-950 underline cursor-pointer"
                >
                  Auto-fill Codes
                </button>
              </div>
              <div className="grid grid-cols-2 gap-2 text-[11px] font-mono">
                <div className="bg-white/80 p-2 rounded-xl border border-amber-200/60">
                  <span className="text-amber-700 block text-[9px] uppercase font-sans font-bold">Email OTP:</span>
                  <span className="text-sm font-extrabold text-[#10324E]">{emailDevOtp || '—'}</span>
                </div>
                <div className="bg-white/80 p-2 rounded-xl border border-amber-200/60">
                  <span className="text-amber-700 block text-[9px] uppercase font-sans font-bold">Mobile OTP:</span>
                  <span className="text-sm font-extrabold text-[#10324E]">{phoneDevOtp || '—'}</span>
                </div>
              </div>
            </div>
          )}

          {/* Target 1: Email OTP */}
          <div className={`p-4 rounded-2xl border transition-all ${
            emailVerified ? 'bg-[#E0F2EB]/40 border-[#2D8A7A]/40' : 'bg-[#FAF8F5] border-[#10324E]/10'
          }`}>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-1.5 text-xs font-bold text-[#10324E]">
                <Mail className="w-3.5 h-3.5 text-[#2D8A7A]" />
                <span>Email Verification</span>
              </div>
              {emailVerified ? (
                <span className="inline-flex items-center gap-1 text-[11px] font-extrabold text-[#2D8A7A] bg-white px-2 py-0.5 rounded-full border border-[#2D8A7A]/30">
                  <CheckCircle2 className="w-3 h-3" />
                  Verified
                </span>
              ) : (
                <span className="text-[10px] text-[#242826]/60 font-mono truncate max-w-[170px]">
                  {email}
                </span>
              )}
            </div>

            {!emailVerified && (
              <div className="space-y-2 mt-2">
                <div className="flex gap-2">
                  <input
                    type="text"
                    maxLength={6}
                    value={emailOtp}
                    onChange={(e) => setEmailOtp(e.target.value.replace(/\D/g, ''))}
                    placeholder="6-digit Email OTP"
                    className="flex-1 px-3 py-2.5 rounded-xl border border-[#E5E2D8] bg-white text-xs font-mono tracking-widest text-center text-[#10324E] font-bold focus:outline-hidden focus:ring-2 focus:ring-[#2D8A7A]"
                  />
                  <button
                    type="button"
                    disabled={isVerifyingEmail || emailOtp.length !== 6}
                    onClick={handleVerifyEmailOtp}
                    className="px-4 py-2.5 rounded-xl bg-[#2D8A7A] hover:bg-[#236e62] disabled:opacity-40 text-white text-xs font-bold transition-all cursor-pointer"
                  >
                    {isVerifyingEmail ? '...' : 'Verify'}
                  </button>
                </div>
                <div className="flex justify-end">
                  <button
                    type="button"
                    disabled={resendTimer > 0 || isSendingOtp}
                    onClick={handleResendEmailOtp}
                    className="text-[10px] font-bold text-[#10324E] hover:text-[#2D8A7A] disabled:opacity-40 cursor-pointer"
                  >
                    {resendTimer > 0 ? `Resend email code in ${resendTimer}s` : 'Resend Email OTP'}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Target 2: Mobile Phone OTP */}
          <div className={`p-4 rounded-2xl border transition-all ${
            phoneVerified ? 'bg-[#E0F2EB]/40 border-[#2D8A7A]/40' : 'bg-[#FAF8F5] border-[#10324E]/10'
          }`}>
            <div className="flex items-center justify-between mb-2">
              <div className="flex items-center gap-1.5 text-xs font-bold text-[#10324E]">
                <Smartphone className="w-3.5 h-3.5 text-[#10324E]" />
                <span>Mobile Phone Verification</span>
              </div>
              {phoneVerified ? (
                <span className="inline-flex items-center gap-1 text-[11px] font-extrabold text-[#2D8A7A] bg-white px-2 py-0.5 rounded-full border border-[#2D8A7A]/30">
                  <CheckCircle2 className="w-3 h-3" />
                  Verified
                </span>
              ) : (
                <span className="text-[10px] text-[#242826]/60 font-mono">
                  {signupRole === 'PET_OWNER' ? parentPhone : vetPhone}
                </span>
              )}
            </div>

            {!phoneVerified && (
              <div className="space-y-2 mt-2">
                <div className="flex gap-2">
                  <input
                    type="text"
                    maxLength={6}
                    value={phoneOtp}
                    onChange={(e) => setPhoneOtp(e.target.value.replace(/\D/g, ''))}
                    placeholder="6-digit Mobile OTP"
                    className="flex-1 px-3 py-2.5 rounded-xl border border-[#E5E2D8] bg-white text-xs font-mono tracking-widest text-center text-[#10324E] font-bold focus:outline-hidden focus:ring-2 focus:ring-[#10324E]"
                  />
                  <button
                    type="button"
                    disabled={isVerifyingPhone || phoneOtp.length !== 6}
                    onClick={handleVerifyPhoneOtp}
                    className="px-4 py-2.5 rounded-xl bg-[#10324E] hover:bg-[#0c273e] disabled:opacity-40 text-white text-xs font-bold transition-all cursor-pointer"
                  >
                    {isVerifyingPhone ? '...' : 'Verify'}
                  </button>
                </div>
                <div className="flex justify-end">
                  <button
                    type="button"
                    disabled={resendTimer > 0 || isSendingOtp}
                    onClick={handleResendPhoneOtp}
                    className="text-[10px] font-bold text-[#10324E] hover:text-[#2D8A7A] disabled:opacity-40 cursor-pointer"
                  >
                    {resendTimer > 0 ? `Resend mobile code in ${resendTimer}s` : 'Resend Mobile OTP'}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* Complete Registration Action Button */}
          <button
            type="button"
            disabled={isLoading || (!emailVerified && emailOtp.length !== 6) || (!phoneVerified && phoneOtp.length !== 6)}
            onClick={handleCompleteRegistration}
            className="w-full py-3.5 px-4 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white font-bold text-xs shadow-md transition-all cursor-pointer disabled:opacity-40 mt-3"
          >
            {isLoading ? 'Verifying & Registering...' : 'Complete Registration & Access Portal'}
          </button>
        </div>
      ) : (

        /* ── STEP 1: Main Login / Signup Form ── */
        <form onSubmit={mode === 'login' ? handleLoginSubmit : handleProceedToOtp} className="space-y-3.5">
          
          {/* ── Pet Parent Fields ── */}
          {mode === 'signup' && signupRole === 'PET_OWNER' && (
            <>
              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Full Legal Name *</label>
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
                <label className="block text-xs font-bold text-[#10324E] mb-1">Mobile Phone (for OTP Verification) *</label>
                <div className="relative">
                  <Phone className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
                  <input
                    type="tel"
                    required
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

              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Clinic / Doctor Mobile (for OTP Verification) *</label>
                <div className="relative">
                  <Phone className="w-4 h-4 absolute left-3.5 top-3 text-[#242826]/40" />
                  <input
                    type="tel"
                    required
                    value={vetPhone}
                    onChange={(e) => setVetPhone(e.target.value)}
                    placeholder="+91 98450 12345"
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
            <label className="block text-xs font-bold text-[#10324E] mb-1">
              Email Address {mode === 'signup' && '(for OTP Verification)'} *
            </label>
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

          {/* Submit / Proceed Button */}
          <button
            type="submit"
            disabled={isLoading || isSendingOtp}
            className="w-full py-3.5 px-4 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white font-bold text-xs shadow-md transition-all cursor-pointer disabled:opacity-50 mt-2"
          >
            {isLoading || isSendingOtp
              ? 'Sending OTP Codes...'
              : mode === 'login'
              ? 'Sign In'
              : 'Proceed to OTP Verification →'}
          </button>
        </form>
      )}

      {/* ── Divider & Google SSO (Only on initial form screen) ── */}
      {signupStep === 'form' && (
        <>
          <div className="relative my-5">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-[#10324E]/8" />
            </div>
            <div className="relative flex justify-center text-[10px] uppercase font-bold text-[#242826]/40">
              <span className="bg-white px-2">or continue with</span>
            </div>
          </div>

          <div className="flex justify-center min-h-[44px]">
            <div ref={googleButtonContainerRef} />
          </div>
        </>
      )}

      {/* ── Bottom Switch Link ── */}
      <div className="mt-5 text-center text-xs text-[#242826]/70">
        {mode === 'login' ? (
          <p>
            Don't have an account yet?{' '}
            <button
              type="button"
              onClick={() => {
                setMode('signup');
                setSignupStep('form');
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
                setSignupStep('form');
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

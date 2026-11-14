import React, { useState, useEffect } from 'react';
import {
  Stethoscope,
  Mail,
  Lock,
  Eye,
  EyeOff,
  User,
  Phone,
  Building2,
  Award,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
  AlertCircle,
  ShieldCheck,
  FileCheck,
  KeyRound,
  RefreshCw,
  Sparkles
} from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { sendOtpApi, verifyOtpApi } from '../../services/api';
import VetalyaBrandLogo from './VetalyaBrandLogo';

export default function VetAuthPage({ initialMode = 'login', onNavigate }) {
  const { login, signup, isLoading } = useAuth();
  const [mode, setMode] = useState(initialMode); // 'login' | 'register'

  // Form Fields
  const [vetName, setVetName] = useState('');
  const [email, setEmail] = useState('');
  const [pendingRegistration, setPendingRegistration] = useState(null);
  const [lastCodeRequest, setLastCodeRequest] = useState(null);
  const [code, setCode] = useState('');
  const [emailProof, setEmailProof] = useState('');
  const [resendIn, setResendIn] = useState(0);

  useEffect(() => {
    if (!resendIn) return;
    const timer = setTimeout(() => setResendIn(resendIn - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendIn]);

  async function sendCode(target) {
    await sendOtpApi(target, 'REGISTRATION');
    setLastCodeRequest({ email: target, sentAt: Date.now() });
    setCode('');
    setEmailProof('');
    setResendIn(60);
  }
  const [phone, setPhone] = useState('');
  const [councilRegNo, setCouncilRegNo] = useState('');
  const [councilState, setCouncilState] = useState('Karnataka Veterinary Council (KVC)');
  const [degree, setDegree] = useState('BVSc & AH');
  const [clinicName, setClinicName] = useState('');
  const [city, setCity] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [agreeDeclaration, setAgreeDeclaration] = useState(false);

  // Status & Error
  const [error, setError] = useState(null);
  const [successNotice, setSuccessNotice] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Handle Initial Form Submission (Login OR Step 1 of Registration)
  const handleFormSubmit = async (e) => {
    e.preventDefault();
    if (isSubmitting || isLoading) return;
    setError(null);
    setSuccessNotice(null);

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!email.trim() || !emailRegex.test(email.trim())) {
      setError('Please enter a valid official email address.');
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
        setError(err.message || 'Authentication failed. Please verify your doctor credentials.');
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    // Mode is 'register': Validate all clinical registration fields
    if (!vetName.trim()) {
      setError('Please enter your full name with professional title.');
      return;
    }
    if (!phone.trim() || phone.trim().replace(/\D/g, '').length < 10) {
      setError('Please enter a valid 10-digit contact mobile number.');
      return;
    }
    if (!councilRegNo.trim()) {
      setError('Please provide your State Veterinary Council Registration Number.');
      return;
    }
    if (!clinicName.trim()) {
      setError('Please enter your primary hospital or clinic name.');
      return;
    }
    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }
    if (!agreeDeclaration) {
      setError('Please confirm the statutory Veterinary Council declaration.');
      return;
    }

    const cleanEmail = email.trim().toLowerCase();
    if (password.length < 8) {
      setError('Use a password of at least 8 characters to register.');
      return;
    }
    const registration = {
      fullName: vetName.trim().startsWith('Dr.') ? vetName.trim() : `Dr. ${vetName.trim()}`,
      email: cleanEmail,
      phone: phone.trim(),
      password,
      role: 'ROLE_VET',
      councilRegNumber: councilRegNo.trim(),
      councilState,
      qualification: degree,
      degree,
      clinicName: clinicName.trim(),
      city: city.trim() || 'Bengaluru',
    };

    setIsSubmitting(true);
    try {
      if (lastCodeRequest?.email !== cleanEmail || Date.now() - lastCodeRequest.sentAt >= 600_000) {
        await sendCode(cleanEmail);
      }
      setPendingRegistration(registration);
    } catch (err) {
      setError(err.message || 'Could not send the verification code.');
    } finally {
      setIsSubmitting(false);
    }
  };

  async function finishRegistration(event) {
    event.preventDefault();
    if (isSubmitting || isLoading || (!emailProof && code.length !== 6)) return;
    setIsSubmitting(true);
    setError(null);
    try {
      let proof = emailProof;
      if (!proof) {
        const response = await verifyOtpApi(pendingRegistration.email, code, 'REGISTRATION');
        proof = response.emailVerificationToken;
        if (!proof) throw new Error('Could not verify your email. Request a new code.');
        setEmailProof(proof);
      }
      await signup({ ...pendingRegistration, emailVerificationToken: proof });
      setSuccessNotice('Doctor registered successfully! Entering clinical dashboard...');
    } catch (err) {
      setError(err.message || 'Registration failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  }

  async function resendCode() {
    setError(null);
    setIsSubmitting(true);
    try {
      await sendCode(pendingRegistration.email);
    } catch (err) {
      setError(err.message || 'Could not resend the verification code.');
    } finally {
      setIsSubmitting(false);
    }
  }

  // Quick helper to fill pre-seeded doctor credentials for testing
  const handleFillDemoVet = () => {
    setEmail('dr.priya@vetalya.com');
    setPassword('Priya@Vetalya2026!');
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
            onClick={() => onNavigate('/owner/login')}
            className="text-[#10324E]/70 hover:text-[#10324E] transition-colors hidden sm:inline"
          >
            Are you a Pet Parent? Login here
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
      <main className="max-w-lg w-full mx-auto my-6 px-4 sm:px-0">
        <div className="bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left space-y-6">
          
          {/* Header Badge & Title */}
          <div className="space-y-2">
            <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-50 text-[#10324E] border border-blue-200 text-[10px] font-extrabold uppercase tracking-widest">
              <Stethoscope className="w-3.5 h-3.5" />
              <span>Veterinarian Clinical Desk</span>
            </div>
            <h1 className="text-2xl font-black text-[#10324E] tracking-tight">
              {mode === 'login'
                ? 'Doctor OPD Login'
                : 'Register Clinic Practice'}
            </h1>
            <p className="text-xs text-[#242826]/70 leading-relaxed">
              {mode === 'login'
                ? 'Sign in to access patient health timelines, digital Rx builder, and OPD consultation desk.'
                : 'Verified practitioner registration. Requires State Council Registration Number for audit compliance.'}
            </p>
          </div>

          {/* Mode Switcher Pills */}
          <div className="flex p-1 bg-[#F5F2EB] rounded-2xl">
            <button
              type="button"
              disabled={isSubmitting || isLoading}
              onClick={() => { setPendingRegistration(null); setMode('login'); setError(null); setSuccessNotice(null); }}
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
              disabled={isSubmitting || isLoading}
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

          {/* Quick Doctor Seed Banner (Login Mode Only) */}
          {mode === 'login' && (
            <div className="p-3 rounded-2xl bg-blue-50/70 border border-blue-200 text-xs flex items-center justify-between text-[#10324E]">
              <div className="space-y-0.5">
                <span className="font-bold block text-[11px]">Verified Practitioner Account:</span>
                <span className="text-[10px] text-[#242826]/70">dr.priya@vetalya.com • Priya@Vetalya2026!</span>
              </div>
              <button
                type="button"
                onClick={handleFillDemoVet}
                className="px-2.5 py-1 rounded-lg bg-[#10324E] text-white text-[10px] font-bold cursor-pointer hover:bg-[#0c273e]"
              >
                Auto-fill
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

          {/* ── Main Form (Login OR Registration) ── */}
          {pendingRegistration ? (
            <form onSubmit={finishRegistration} className="space-y-4">
              <h2 className="text-lg font-bold text-[#10324E]">Verify your email</h2>
              <p className="text-xs">Enter the code sent to {pendingRegistration.email}. It expires in 10 minutes.</p>
              {!emailProof && (
                <label className="block text-xs font-bold">
                  6-digit email code
                  <input autoFocus autoComplete="one-time-code" inputMode="numeric" maxLength={6}
                    value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))}
                    disabled={isSubmitting} className="block w-full mt-2 p-3 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10" />
                </label>
              )}
              <button type="submit" disabled={isSubmitting || isLoading || (!emailProof && code.length !== 6)}
                className="w-full p-3 rounded-2xl bg-[#10324E] text-white text-xs font-bold disabled:opacity-50">
                {isSubmitting ? 'Please wait…' : emailProof ? 'Retry registration' : 'Verify and register'}
              </button>
              <div className="flex flex-wrap gap-4 text-xs font-bold text-[#10324E]">
                <button type="button" disabled={isSubmitting || resendIn > 0} onClick={resendCode}>
                  {resendIn ? `Resend in ${resendIn}s` : 'Resend code'}
                </button>
                <button type="button" disabled={isSubmitting}
                  onClick={() => { setPendingRegistration(null); setError(null); }}>
                  Back to registration
                </button>
              </div>
            </form>
          ) : (
          <form onSubmit={handleFormSubmit} className="space-y-4">
            <fieldset disabled={isSubmitting || isLoading} className="space-y-4">
              
              {mode === 'register' && (
                <>
                  <div>
                    <label className="block text-[11px] font-bold text-[#10324E] mb-1">Doctor Full Name</label>
                    <div className="relative">
                      <User className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                      <input
                        type="text"
                        value={vetName}
                        onChange={(e) => setVetName(e.target.value)}
                        placeholder="Dr. Priya Sharma"
                        className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none focus:border-[#10324E] transition-colors"
                        required
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-bold text-[#10324E] mb-1">Council Registration No.</label>
                      <div className="relative">
                        <Award className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                        <input
                          type="text"
                          value={councilRegNo}
                          onChange={(e) => setCouncilRegNo(e.target.value)}
                          placeholder="e.g. KVC-2022-8192"
                          className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                          required
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-[11px] font-bold text-[#10324E] mb-1">State Council</label>
                      <select
                        value={councilState}
                        onChange={(e) => setCouncilState(e.target.value)}
                        className="w-full px-3 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                      >
                        <option value="Karnataka Veterinary Council (KVC)">Karnataka (KVC)</option>
                        <option value="Maharashtra Veterinary Council (MVC)">Maharashtra (MVC)</option>
                        <option value="Delhi Veterinary Council (DVC)">Delhi (DVC)</option>
                        <option value="Tamil Nadu Veterinary Council (TNVC)">Tamil Nadu (TNVC)</option>
                        <option value="Veterinary Council of India (VCI)">Other / VCI Direct</option>
                      </select>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-bold text-[#10324E] mb-1">Primary Degree</label>
                      <select
                        value={degree}
                        onChange={(e) => setDegree(e.target.value)}
                        className="w-full px-3 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                      >
                        <option value="BVSc & AH">BVSc &amp; AH</option>
                        <option value="MVSc Surgery">MVSc (Surgery)</option>
                        <option value="MVSc Medicine">MVSc (Medicine)</option>
                        <option value="PhD Veterinary">PhD Veterinary Sciences</option>
                      </select>
                    </div>

                    <div>
                      <label className="block text-[11px] font-bold text-[#10324E] mb-1">Contact Phone (+91)</label>
                      <div className="relative">
                        <Phone className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                        <input
                          type="tel"
                          value={phone}
                          onChange={(e) => setPhone(e.target.value)}
                          placeholder="98450 12345"
                          className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                          required
                        />
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <div>
                      <label className="block text-[11px] font-bold text-[#10324E] mb-1">Hospital / Clinic Name</label>
                      <div className="relative">
                        <Building2 className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                        <input
                          type="text"
                          value={clinicName}
                          onChange={(e) => setClinicName(e.target.value)}
                          placeholder="e.g. Companion Hospital"
                          className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                          required
                        />
                      </div>
                    </div>

                    <div>
                      <label className="block text-[11px] font-bold text-[#10324E] mb-1">City</label>
                      <input
                        type="text"
                        value={city}
                        onChange={(e) => setCity(e.target.value)}
                        placeholder="e.g. Bengaluru"
                        className="w-full px-3.5 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                        required
                      />
                    </div>
                  </div>
                </>
              )}

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">
                  {mode === 'login' ? 'Doctor Email or Mobile' : 'Official Email Address'}
                </label>
                <div className="relative">
                  <Mail className="absolute left-3.5 top-3 w-4 h-4 text-[#242826]/40" />
                  <input
                    type="text"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="doctor@hospital.in"
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
                      className="w-full pl-10 pr-4 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                      required
                    />
                  </div>
                </div>
              )}

              {mode === 'register' && (
                <label className="flex items-start gap-2 pt-1 text-[11px] text-[#242826]/75 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={agreeDeclaration}
                    onChange={(e) => setAgreeDeclaration(e.target.checked)}
                    className="mt-0.5 rounded border-slate-300 text-[#10324E] focus:ring-0"
                  />
                  <span>I affirm that I am a registered veterinary practitioner compliant with VCI / State Council regulations.</span>
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
                    <span>{mode === 'login' ? 'Sign In to OPD Desk' : 'Register Doctor Account'}</span>
                    <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </fieldset>
            </form>
          )}

          {/* Quick Role Switcher Footer */}
          <div className="pt-2 text-center text-[11px] text-[#242826]/60">
            <span>Are you a Pet Parent? </span>
            <button
              onClick={() => onNavigate('/owner/login')}
              className="font-bold text-[#10324E] hover:underline"
            >
              Login to Pet Passport →
            </button>
          </div>

        </div>
      </main>

      {/* Footer */}
      <footer className="text-center py-4 text-xs text-[#242826]/50">
        © 2026 Vetalya Health Technologies • Veterinarian Clinical Desk
      </footer>

    </div>
  );
}

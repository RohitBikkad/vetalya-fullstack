import React, { useState, useEffect } from 'react';
import {
  Shield,
  Heart,
  Stethoscope,
  ChevronDown,
  Menu,
  X,
  ArrowRight,
  CheckCircle2,
  FileText,
  AlertTriangle,
  QrCode,
  Calendar,
  Lock,
  Award,
  Sparkles,
  Phone,
  Mail,
  User,
  ExternalLink,
  ChevronRight,
  Clock,
  Activity,
  FileCheck
} from 'lucide-react';
import VetalyaBrandLogo from '../auth/VetalyaBrandLogo';

export default function LandingPage({ onNavigate }) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [loginDropdownOpen, setLoginDropdownOpen] = useState(false);
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen bg-[#F7F5EE] text-[#242826] font-sans selection:bg-[#10324E] selection:text-white">

      {/* ── 1. STICKY NAVBAR ── */}
      <header
        className={`sticky top-0 z-50 transition-all duration-300 ${
          isScrolled
            ? 'bg-[#F7F5EE]/90 backdrop-blur-md shadow-xs border-b border-[#10324E]/8 py-3'
            : 'bg-transparent py-4 sm:py-5'
        }`}
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex items-center justify-between">
          {/* Logo & Brand */}
          <div
            onClick={() => onNavigate('/')}
            className="cursor-pointer transition-transform hover:scale-[1.01]"
          >
            <VetalyaBrandLogo />
          </div>

          {/* Desktop Nav Links */}
          <nav className="hidden lg:flex items-center space-x-7 text-xs font-bold text-[#10324E]/80">
            <a href="#features" className="hover:text-[#10324E] transition-colors">Features</a>
            <a href="#how-it-works" className="hover:text-[#10324E] transition-colors">How It Works</a>
            <a href="#for-owners" className="hover:text-[#10324E] transition-colors">For Pet Owners</a>
            <a href="#for-vets" className="hover:text-[#10324E] transition-colors">For Veterinarians</a>
            <a href="#trust" className="hover:text-[#10324E] transition-colors">Trust &amp; Privacy</a>
          </nav>

          {/* Desktop Auth Actions */}
          <div className="hidden sm:flex items-center space-x-3">
            {/* Login Dropdown */}
            <div className="relative">
              <button
                type="button"
                onClick={() => setLoginDropdownOpen(!loginDropdownOpen)}
                className="flex items-center gap-1.5 px-4 py-2.5 rounded-2xl bg-white hover:bg-slate-50 border border-[#10324E]/15 text-xs font-extrabold text-[#10324E] shadow-2xs transition-all cursor-pointer"
              >
                <span>Login</span>
                <ChevronDown className={`w-3.5 h-3.5 text-[#10324E]/70 transition-transform ${loginDropdownOpen ? 'rotate-180' : ''}`} />
              </button>

              {loginDropdownOpen && (
                <>
                  <div
                    className="fixed inset-0 z-40"
                    onClick={() => setLoginDropdownOpen(false)}
                  />
                  <div className="absolute right-0 mt-2 w-56 rounded-2xl bg-white border border-[#10324E]/10 p-2 shadow-vetalya z-50 animate-in fade-in zoom-in-95">
                    <button
                      onClick={() => {
                        setLoginDropdownOpen(false);
                        onNavigate('/owner/login');
                      }}
                      className="w-full text-left p-3 rounded-xl hover:bg-[#F5F2EB] transition-colors flex items-center gap-2.5 cursor-pointer group"
                    >
                      <div className="w-8 h-8 rounded-lg bg-[#E0F2EB] text-[#2D8A7A] flex items-center justify-center font-bold">
                        <Heart className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="text-xs font-bold text-[#10324E] group-hover:text-[#2D8A7A]">Pet Owner Login</div>
                        <div className="text-[10px] text-[#242826]/60">Access digital pet passports</div>
                      </div>
                    </button>

                    <div className="h-px bg-[#10324E]/6 my-1" />

                    <button
                      onClick={() => {
                        setLoginDropdownOpen(false);
                        onNavigate('/vet/login');
                      }}
                      className="w-full text-left p-3 rounded-xl hover:bg-[#F5F2EB] transition-colors flex items-center gap-2.5 cursor-pointer group"
                    >
                      <div className="w-8 h-8 rounded-lg bg-blue-50 text-[#10324E] flex items-center justify-center font-bold">
                        <Stethoscope className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="text-xs font-bold text-[#10324E] group-hover:text-[#10324E]">Veterinarian Login</div>
                        <div className="text-[10px] text-[#242826]/60">Doctor OPD consultation desk</div>
                      </div>
                    </button>
                  </div>
                </>
              )}
            </div>

            {/* Primary Get Started Button */}
            <button
              onClick={() => onNavigate('/owner/register')}
              className="px-5 py-2.5 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white text-xs font-extrabold shadow-sm hover:shadow-md transition-all cursor-pointer flex items-center gap-1.5"
            >
              <span>Get Started</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>

          {/* Mobile Hamburger Button */}
          <div className="sm:hidden flex items-center">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-xl bg-white border border-[#10324E]/10 text-[#10324E] cursor-pointer"
            >
              {mobileMenuOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>
          </div>
        </div>

        {/* Mobile Dropdown Drawer */}
        {mobileMenuOpen && (
          <div className="sm:hidden bg-[#F7F5EE] border-b border-[#10324E]/10 px-4 pt-3 pb-6 space-y-4 animate-in slide-in-from-top-4">
            <div className="grid grid-cols-2 gap-2 pt-2">
              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  onNavigate('/owner/login');
                }}
                className="p-3 rounded-2xl bg-white border border-[#10324E]/10 text-xs font-extrabold text-[#10324E] flex flex-col items-center gap-1 shadow-2xs"
              >
                <Heart className="w-4 h-4 text-[#F49E78]" />
                <span>Pet Owner Login</span>
              </button>
              <button
                onClick={() => {
                  setMobileMenuOpen(false);
                  onNavigate('/vet/login');
                }}
                className="p-3 rounded-2xl bg-white border border-[#10324E]/10 text-xs font-extrabold text-[#10324E] flex flex-col items-center gap-1 shadow-2xs"
              >
                <Stethoscope className="w-4 h-4 text-[#10324E]" />
                <span>Vet Clinic Login</span>
              </button>
            </div>

            <button
              onClick={() => {
                setMobileMenuOpen(false);
                onNavigate('/owner/register');
              }}
              className="w-full py-3 rounded-2xl bg-[#10324E] text-white text-xs font-extrabold flex items-center justify-center gap-2 shadow-sm"
            >
              <span>Create Free Pet Passport</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </header>

      {/* ── 2. HERO SECTION ── */}
      <section className="relative overflow-hidden pt-8 pb-16 lg:py-20">
        {/* Soft Background Accents */}
        <div className="absolute top-[20%] -left-20 w-52 h-52 rounded-full bg-[#F49E78]/20 blur-3xl pointer-events-none" />
        <div className="absolute top-[10%] -right-20 w-72 h-72 rounded-full bg-[#E0F2EB]/50 blur-3xl pointer-events-none" />

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
            
            {/* Left Content Column */}
            <div className="lg:col-span-7 space-y-6 text-left">
              {/* Badge */}
              <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-white border border-[#10324E]/10 shadow-2xs">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                <span className="text-xs font-extrabold text-[#10324E] tracking-tight">
                  Digital Pet Health Records • India
                </span>
              </div>

              {/* Headline */}
              <h1 className="text-3xl sm:text-4xl md:text-5xl lg:text-5xl font-black text-[#10324E] tracking-tight leading-[1.15]">
                Where Every Companion’s Lifelong Health Record Stays Together.
              </h1>

              {/* Subtitle */}
              <p className="text-sm sm:text-base text-[#242826]/75 max-w-xl leading-relaxed">
                Connect pet parents, veterinary OPD clinics, vaccination histories, and verified prescriptions into one unified digital passport. Accessible anywhere, anytime.
              </p>

              {/* Dual Action Buttons */}
              <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3.5 pt-2">
                <button
                  onClick={() => onNavigate('/owner/register')}
                  className="px-6 py-4 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white font-extrabold text-xs sm:text-sm shadow-md hover:shadow-lg transition-all flex items-center justify-center gap-2 cursor-pointer group"
                >
                  <Heart className="w-4 h-4 text-[#F49E78] group-hover:scale-110 transition-transform" />
                  <span>I’m a Pet Owner</span>
                  <ArrowRight className="w-4 h-4 ml-1" />
                </button>

                <button
                  onClick={() => onNavigate('/vet/register')}
                  className="px-6 py-4 rounded-2xl bg-white hover:bg-slate-50 border-2 border-[#10324E] text-[#10324E] font-extrabold text-xs sm:text-sm shadow-2xs hover:shadow-sm transition-all flex items-center justify-center gap-2 cursor-pointer"
                >
                  <Stethoscope className="w-4 h-4 text-[#10324E]" />
                  <span>I’m a Veterinarian</span>
                </button>
              </div>

              {/* Trust Indicators */}
              <div className="pt-4 flex flex-wrap items-center gap-4 text-[11px] font-bold text-[#242826]/60">
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Free for Pet Owners</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>VCI Registered Doctors</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                  <span>Instant Emergency QR</span>
                </div>
              </div>
            </div>

            {/* Right Hero Image & Floating Pet ID Card */}
            <div className="lg:col-span-5 relative flex justify-center">
              <div className="relative w-full max-w-[420px]">
                
                {/* Hero Pet Image (Golden Retriever and Cat) */}
                <div className="rounded-[32px] overflow-hidden shadow-vetalya border-4 border-white aspect-4/3 bg-slate-100">
                  <img
                    src="https://images.unsplash.com/photo-1548767797-d8c844163c4c?auto=format&fit=crop&w=800&q=80"
                    alt="Dog and Cat companion"
                    className="w-full h-full object-cover transform hover:scale-105 transition-transform duration-700"
                  />
                </div>

                {/* Floating Live Digital Pet Passport Card */}
                <div className="absolute -bottom-8 -left-4 sm:-left-8 bg-white/95 backdrop-blur-md rounded-2xl p-4 sm:p-5 shadow-vetalya border border-[#10324E]/10 max-w-[280px] sm:max-w-[300px] text-left animate-in fade-in slide-in-from-bottom-4">
                  <div className="flex items-center justify-between pb-2 mb-2 border-b border-[#10324E]/8">
                    <span className="text-[10px] font-black uppercase tracking-wider text-[#2D8A7A]">
                      Digital Pet Passport
                    </span>
                    <span className="inline-flex items-center px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 text-[9px] font-extrabold border border-emerald-200">
                      Vaccinated
                    </span>
                  </div>

                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-xl bg-amber-100 flex items-center justify-center text-lg shadow-2xs font-bold text-amber-800">
                      🐕
                    </div>
                    <div>
                      <div className="text-xs font-extrabold text-[#10324E]">Leo</div>
                      <div className="text-[10px] text-[#242826]/60">Golden Retriever • 2.5 yrs</div>
                    </div>
                  </div>

                  <div className="mt-3 pt-2 border-t border-[#10324E]/6 flex items-center justify-between text-[10px]">
                    <div>
                      <span className="text-[#242826]/50 block">Pet ID:</span>
                      <span className="font-mono font-bold text-[#10324E]">PET-8F2A41C09D3E</span>
                    </div>
                    <div className="p-1.5 rounded-lg bg-[#F5F2EB] border border-[#10324E]/10">
                      <QrCode className="w-4 h-4 text-[#10324E]" />
                    </div>
                  </div>
                </div>

              </div>
            </div>

          </div>
        </div>
      </section>

      {/* ── 3. THE PROBLEM WE SOLVE ── */}
      <section id="features" className="py-16 sm:py-20 bg-white border-y border-[#10324E]/8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-12">
          
          <div className="max-w-2xl mx-auto space-y-3">
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-[#F49E78]">
              The Healthcare Disconnect
            </span>
            <h2 className="text-2xl sm:text-3xl font-black text-[#10324E] tracking-tight">
              Why Animal Healthcare Records Are Broken Today
            </h2>
            <p className="text-xs sm:text-sm text-[#242826]/70">
              Pet parents struggle to preserve booklets, while veterinarians must practice medicine with zero historical patient visibility.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 sm:gap-8 text-left">
            {/* Card 1 */}
            <div className="bg-[#FAF8F5] rounded-[28px] p-6 sm:p-8 border border-[#10324E]/8 shadow-2xs hover:shadow-md transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-amber-100/80 text-amber-700 flex items-center justify-center">
                <FileText className="w-6 h-6" />
              </div>
              <h3 className="text-base font-extrabold text-[#10324E]">
                Paper files get lost
              </h3>
              <p className="text-xs text-[#242826]/75 leading-relaxed">
                Vaccination booklets wear out, get misplaced during relocations, or water-damaged. Puppies end up repeating mandatory vaccines needlessly.
              </p>
            </div>

            {/* Card 2 */}
            <div className="bg-[#FAF8F5] rounded-[28px] p-6 sm:p-8 border border-[#10324E]/8 shadow-2xs hover:shadow-md transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-rose-100/80 text-rose-700 flex items-center justify-center">
                <AlertTriangle className="w-6 h-6" />
              </div>
              <h3 className="text-base font-extrabold text-[#10324E]">
                Records buried in chat apps
              </h3>
              <p className="text-xs text-[#242826]/75 leading-relaxed">
                Lab reports, blood chemistries, and discharge summaries sit scattered across WhatsApp threads, email inboxes, and phone galleries.
              </p>
            </div>

            {/* Card 3 */}
            <div className="bg-[#FAF8F5] rounded-[28px] p-6 sm:p-8 border border-[#10324E]/8 shadow-2xs hover:shadow-md transition-all space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-blue-100/80 text-blue-700 flex items-center justify-center">
                <Activity className="w-6 h-6" />
              </div>
              <h3 className="text-base font-extrabold text-[#10324E]">
                Each clinic sees only part of the history
              </h3>
              <p className="text-xs text-[#242826]/75 leading-relaxed">
                When an emergency hits or you visit a new specialist, the attending doctor has zero insight into drug allergies, prior surgeries, or chronic conditions.
              </p>
            </div>
          </div>

        </div>
      </section>

      {/* ── 4. OUR VISION BANNER ── */}
      <section className="py-14 sm:py-16 bg-[#10324E] text-white text-center relative overflow-hidden">
        <div className="max-w-4xl mx-auto px-4 sm:px-6 relative z-10 space-y-4">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-white/10 text-[#F49E78] text-[10px] font-extrabold uppercase tracking-widest">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Our Vision</span>
          </div>
          <h2 className="text-xl sm:text-2xl md:text-3xl font-black leading-snug tracking-tight text-cream-canvas">
            “Every animal deserves a lifelong medical identity that travels with them — from their first 45-day puppy vaccine to senior companion care.”
          </h2>
        </div>
      </section>

      {/* ── 5. HOW IT WORKS ── */}
      <section id="how-it-works" className="py-16 sm:py-20 bg-[#F7F5EE]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-12">
          
          <div className="max-w-xl mx-auto space-y-2">
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-[#2D8A7A]">
              Simple &amp; Universal
            </span>
            <h2 className="text-2xl sm:text-3xl font-black text-[#10324E] tracking-tight">
              How Vetalya Works in 3 Steps
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 text-left">
            {/* Step 1 */}
            <div className="bg-white rounded-[28px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya space-y-4 relative">
              <span className="text-4xl font-black text-[#10324E]/15 font-mono">01</span>
              <h3 className="text-base font-extrabold text-[#10324E]">
                Create a pet profile &amp; get a unique Pet ID
              </h3>
              <p className="text-xs text-[#242826]/75 leading-relaxed">
                Add your companion’s photo, breed, microchip number, and birthdate. Vetalya instantly issues a tamper-proof lifelong identifier.
              </p>
            </div>

            {/* Step 2 */}
            <div className="bg-white rounded-[28px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya space-y-4 relative">
              <span className="text-4xl font-black text-[#2D8A7A]/25 font-mono">02</span>
              <h3 className="text-base font-extrabold text-[#10324E]">
                Upload records, vaccines &amp; SOAP notes
              </h3>
              <p className="text-xs text-[#242826]/75 leading-relaxed">
                Snap photos of old booklets or have attending veterinarians log digital prescriptions and clinic consultations directly to the timeline.
              </p>
            </div>

            {/* Step 3 */}
            <div className="bg-white rounded-[28px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya space-y-4 relative">
              <span className="text-4xl font-black text-[#F49E78]/35 font-mono">03</span>
              <h3 className="text-base font-extrabold text-[#10324E]">
                Share a QR code with emergency details
              </h3>
              <p className="text-xs text-[#242826]/75 leading-relaxed">
                Attach your pet's QR code to their collar. Anyone scanning it sees emergency contacts, drug allergies, and blood group as chosen by you.
              </p>
            </div>
          </div>

        </div>
      </section>

      {/* ── 6. FOR PET OWNERS ── */}
      <section id="for-owners" className="py-16 sm:py-20 bg-white border-t border-[#10324E]/8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
            
            <div className="lg:col-span-6 space-y-6 text-left">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#E0F2EB] text-[#2D8A7A] text-[10px] font-extrabold uppercase tracking-widest">
                <Heart className="w-3.5 h-3.5" />
                <span>Pet Parents Portal</span>
              </div>

              <h2 className="text-2xl sm:text-3xl md:text-4xl font-black text-[#10324E] tracking-tight">
                Complete Peace of Mind for Your Companion’s Care.
              </h2>

              <p className="text-xs sm:text-sm text-[#242826]/75 leading-relaxed">
                Never miss an annual DHPPiL booster or scramble for medical history at an emergency clinic again. Everything is organized in your pocket.
              </p>

              {/* Feature Checklist */}
              <div className="space-y-3.5 pt-2">
                {[
                  "Lifelong medical health & vaccination timeline",
                  "Automated WhatsApp & SMS reminders for due and overdue shots",
                  "Private, encrypted cloud vault for blood test PDFs and ultrasounds",
                  "Verified clinic consultation and digital prescription history"
                ].map((feat, idx) => (
                  <div key={idx} className="flex items-start gap-3">
                    <div className="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0 mt-0.5">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                    </div>
                    <span className="text-xs font-bold text-[#242826]">{feat}</span>
                  </div>
                ))}
              </div>

              <div className="pt-4">
                <button
                  onClick={() => onNavigate('/owner/login')}
                  className="px-6 py-3.5 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white font-extrabold text-xs shadow-md hover:shadow-lg transition-all flex items-center gap-2 cursor-pointer"
                >
                  <span>Pet Owner Login</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

            <div className="lg:col-span-6 flex justify-center">
              <div className="w-full max-w-md bg-[#FAF8F5] rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/8">
                  <span className="text-xs font-extrabold text-[#10324E]">Upcoming Reminders</span>
                  <span className="text-[10px] text-amber-700 font-bold bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">2 Due Soon</span>
                </div>

                <div className="space-y-2.5">
                  <div className="p-3 rounded-2xl bg-white border border-[#10324E]/6 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center font-bold text-xs">
                        💉
                      </div>
                      <div>
                        <div className="text-xs font-bold text-[#10324E]">Anti-Rabies Booster</div>
                        <div className="text-[10px] text-[#242826]/60">Annual vaccine renewal</div>
                      </div>
                    </div>
                    <span className="text-[10px] font-extrabold text-rose-600 bg-rose-50 px-2 py-0.5 rounded-md">Due in 5d</span>
                  </div>

                  <div className="p-3 rounded-2xl bg-white border border-[#10324E]/6 flex items-center justify-between">
                    <div className="flex items-center gap-2.5">
                      <div className="w-8 h-8 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center font-bold text-xs">
                        💊
                      </div>
                      <div>
                        <div className="text-xs font-bold text-[#10324E]">Deworming Dose (Drontal Plus)</div>
                        <div className="text-[10px] text-[#242826]/60">Quarterly preventative care</div>
                      </div>
                    </div>
                    <span className="text-[10px] font-extrabold text-blue-600 bg-blue-50 px-2 py-0.5 rounded-md">Due in 14d</span>
                  </div>
                </div>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* ── 7. FOR VETERINARIANS ── */}
      <section id="for-vets" className="py-16 sm:py-20 bg-[#F7F5EE]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-center">
            
            <div className="lg:col-span-6 order-2 lg:order-1 flex justify-center">
              <div className="w-full max-w-md bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/8">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded-lg bg-emerald-100 text-emerald-800 flex items-center justify-center font-bold text-xs">
                      🩺
                    </div>
                    <span className="text-xs font-extrabold text-[#10324E]">OPD Consultation Desk</span>
                  </div>
                  <span className="text-[10px] text-emerald-800 font-bold bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">KVC Verified</span>
                </div>

                <div className="p-3.5 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/6 space-y-1.5">
                  <div className="text-[10px] font-bold text-[#10324E]/60 uppercase">SOAP Assessment</div>
                  <div className="text-xs font-bold text-[#10324E]">Acute Allergic Dermatitis</div>
                  <div className="text-[11px] text-[#242826]/75">Prescribed Apoquel 5.4mg BD x 7 days with medicated chlorhexidine bath.</div>
                </div>

                <div className="flex items-center justify-between text-[11px] text-[#242826]/60 pt-1">
                  <span>Audit Trail ID: #SOAP-91820</span>
                  <span className="font-bold text-[#2D8A7A]">Signed with Council Lic.</span>
                </div>
              </div>
            </div>

            <div className="lg:col-span-6 order-1 lg:order-2 space-y-6 text-left">
              <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-50 text-[#10324E] text-[10px] font-extrabold uppercase tracking-widest border border-blue-200">
                <Stethoscope className="w-3.5 h-3.5" />
                <span>Veterinary Doctors</span>
              </div>

              <h2 className="text-2xl sm:text-3xl md:text-4xl font-black text-[#10324E] tracking-tight">
                Practice Better Medicine with Instant Patient History.
              </h2>

              <p className="text-xs sm:text-sm text-[#242826]/75 leading-relaxed">
                Vetalya gives doctors complete verified medical records before starting consultation, reducing diagnostic errors and streamlining OPD visits.
              </p>

              {/* Feature Checklist */}
              <div className="space-y-3.5 pt-2">
                {[
                  "View complete verified patient history before consultation with owner consent",
                  "Log structured SOAP notes and issue digital Rx prescriptions with dosage presets",
                  "Authenticate and verify owner-uploaded medical history records with an audit trail",
                  "Verified licensed professionals only (Veterinary Council Registration required)"
                ].map((feat, idx) => (
                  <div key={idx} className="flex items-start gap-3">
                    <div className="w-5 h-5 rounded-full bg-blue-100 text-[#10324E] flex items-center justify-center shrink-0 mt-0.5">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                    </div>
                    <span className="text-xs font-bold text-[#242826]">{feat}</span>
                  </div>
                ))}
              </div>

              <div className="pt-4">
                <button
                  onClick={() => onNavigate('/vet/login')}
                  className="px-6 py-3.5 rounded-2xl bg-white hover:bg-slate-50 border-2 border-[#10324E] text-[#10324E] font-extrabold text-xs shadow-2xs hover:shadow-sm transition-all flex items-center gap-2 cursor-pointer"
                >
                  <span>Veterinarian Login</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* ── 8. TRUST & PRIVACY ── */}
      <section id="trust" className="py-16 sm:py-20 bg-white border-y border-[#10324E]/8">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center space-y-12">
          
          <div className="max-w-xl mx-auto space-y-2">
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-[#F49E78]">
              Security by Design
            </span>
            <h2 className="text-2xl sm:text-3xl font-black text-[#10324E] tracking-tight">
              Clinical Integrity &amp; Strict Data Privacy
            </h2>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 text-left">
            {/* Card 1 */}
            <div className="bg-[#FAF8F5] rounded-[24px] p-6 border border-[#10324E]/8 space-y-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center font-bold">
                <Lock className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-extrabold text-[#10324E]">Private by default</h3>
              <p className="text-xs text-[#242826]/70 leading-relaxed">
                Your pet’s health data is never sold or shared. Only clinics and pet sitters you explicitly authorize can inspect medical histories.
              </p>
            </div>

            {/* Card 2 */}
            <div className="bg-[#FAF8F5] rounded-[24px] p-6 border border-[#10324E]/8 space-y-3">
              <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center font-bold">
                <FileCheck className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-extrabold text-[#10324E]">Verified clinical trail</h3>
              <p className="text-xs text-[#242826]/70 leading-relaxed">
                Owner uploads remain marked as “Unverified” until a licensed veterinarian reviews and confirms them during consultation.
              </p>
            </div>

            {/* Card 3 */}
            <div className="bg-[#FAF8F5] rounded-[24px] p-6 border border-[#10324E]/8 space-y-3">
              <div className="w-10 h-10 rounded-xl bg-blue-100 text-blue-800 flex items-center justify-center font-bold">
                <QrCode className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-extrabold text-[#10324E]">You control the QR</h3>
              <p className="text-xs text-[#242826]/70 leading-relaxed">
                Choose exactly what displays on public QR collar scans: emergency phone numbers and critical allergies, or complete passports.
              </p>
            </div>

            {/* Card 4 */}
            <div className="bg-[#FAF8F5] rounded-[24px] p-6 border border-[#10324E]/8 space-y-3">
              <div className="w-10 h-10 rounded-xl bg-purple-100 text-purple-800 flex items-center justify-center font-bold">
                <Shield className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-extrabold text-[#10324E]">Separate from microchips</h3>
              <p className="text-xs text-[#242826]/70 leading-relaxed">
                Your Pet ID is an independent medical identifier, working seamlessly alongside any existing 15-digit ISO microchip or municipal tag.
              </p>
            </div>
          </div>

        </div>
      </section>

      {/* ── 9. FINAL CTA BANNER ── */}
      <section className="py-16 sm:py-20 bg-[#10324E] text-white text-center relative overflow-hidden">
        <div className="absolute top-0 right-0 w-80 h-80 bg-[#F49E78]/10 rounded-full blur-3xl pointer-events-none" />
        <div className="max-w-3xl mx-auto px-4 sm:px-6 relative z-10 space-y-6">
          <h2 className="text-2xl sm:text-3xl md:text-4xl font-black tracking-tight text-white">
            Start your pet’s health story today.
          </h2>
          <p className="text-xs sm:text-sm text-cream-canvas/80 max-w-xl mx-auto leading-relaxed">
            Free to create. Takes about two minutes. Give your companion the lifelong healthcare identity they deserve.
          </p>

          <div className="pt-2">
            <button
              onClick={() => onNavigate('/owner/register')}
              className="px-8 py-4 rounded-2xl bg-[#F49E78] hover:bg-[#eb8f66] text-[#10324E] font-black text-xs sm:text-sm shadow-lg hover:shadow-xl transition-all cursor-pointer inline-flex items-center gap-2"
            >
              <span>Create Free Pet Passport</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </section>

      {/* ── 10. FOOTER ── */}
      <footer className="bg-[#FAF8F5] border-t border-[#10324E]/10 py-12 text-left text-xs text-[#242826]/70">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8 mb-8 pb-8 border-b border-[#10324E]/8">
            
            <div className="md:col-span-2 space-y-3">
              <VetalyaBrandLogo />
              <p className="text-xs text-[#242826]/60 max-w-sm leading-relaxed">
                One Pet. One Identity. One Lifelong Health Journey. Connected veterinary health records across India.
              </p>
              <div className="pt-2 text-[11px] text-[#242826]/50">
                Support: <a href="mailto:contact@vetalya.com" className="text-[#10324E] font-bold underline">contact@vetalya.com</a>
              </div>
            </div>

            <div className="space-y-2">
              <div className="font-extrabold text-[#10324E] text-xs uppercase tracking-wider">Platform</div>
              <ul className="space-y-1.5 text-xs text-[#242826]/70">
                <li><button onClick={() => onNavigate('/owner/login')} className="hover:text-[#10324E]">Pet Owner Login</button></li>
                <li><button onClick={() => onNavigate('/vet/login')} className="hover:text-[#10324E]">Veterinarian Login</button></li>
                <li><button onClick={() => onNavigate('/owner/register')} className="hover:text-[#10324E]">Create Pet Passport</button></li>
                <li><button onClick={() => onNavigate('/vet/register')} className="hover:text-[#10324E]">Register Clinic Desk</button></li>
              </ul>
            </div>

            <div className="space-y-2">
              <div className="font-extrabold text-[#10324E] text-xs uppercase tracking-wider">Compliance</div>
              <ul className="space-y-1.5 text-xs text-[#242826]/70">
                <li><a href="#trust" className="hover:text-[#10324E]">Privacy Policy</a></li>
                <li><a href="#trust" className="hover:text-[#10324E]">Terms of Service</a></li>
                <li><a href="#trust" className="hover:text-[#10324E]">VCI Council Guidelines</a></li>
                <li><a href="#trust" className="hover:text-[#10324E]">Data Protection</a></li>
              </ul>
            </div>

          </div>

          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 text-[11px] text-[#242826]/50">
            <span>© 2026 Vetalya Health Technologies • All rights reserved.</span>
            <span>Digital Animal Healthcare Platform</span>
          </div>
        </div>
      </footer>

    </div>
  );
}

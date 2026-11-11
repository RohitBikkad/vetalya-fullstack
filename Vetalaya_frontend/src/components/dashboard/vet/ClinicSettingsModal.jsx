import React, { useState } from 'react';
import { 
  X, Check, Building2, User, ShieldCheck, MapPin, Phone, 
  Clock, DollarSign, Award, Stethoscope, Sparkles, Upload, 
  FileText, Shield, CheckCircle2
} from 'lucide-react';
import { STATE_VETERINARY_COUNCILS, VET_SPECIALIZATIONS } from './data/vetFormulary';

export default function ClinicSettingsModal({ initialData, onClose, onSave }) {
  const [activeTab, setActiveTab] = useState('doctor'); // 'doctor' | 'clinic' | 'opd'
  const [saveSuccess, setSaveSuccess] = useState(false);

  // Form state
  const [formData, setFormData] = useState({
    // 1. Doctor Credentials
    doctorName: initialData?.doctorName || 'Dr. Vivek Deshmukh',
    honorific: 'Dr.',
    vciNumber: initialData?.vciNumber || 'VCI/MH/2016/0491',
    stateCouncil: initialData?.stateCouncil || 'Maharashtra State Veterinary Council',
    regYear: initialData?.regYear || '2016',
    expiryDate: initialData?.expiryDate || '2031-03-31',
    primaryDegree: initialData?.primaryDegree || 'BVSc & AH (Mumbai Veterinary College)',
    postGradDegree: initialData?.postGradDegree || 'MVSc in Veterinary Surgery & Radiology',
    additionalCerts: initialData?.additionalCerts || 'ISFM Advanced Feline Practitioner (UK)',
    yearsOfExperience: initialData?.yearsOfExperience || 10,
    specializations: initialData?.specializations || ['Small Animal Internal Medicine', 'Orthopedic & Soft Tissue Surgery', 'Veterinary Dermatology'],
    languages: initialData?.languages || ['English', 'Hindi', 'Marathi'],
    bio: initialData?.bio || 'Dedicated to evidence-based companion animal medicine and complex orthopedic reconstructive surgeries with over a decade of compassionate clinical care.',
    digitalSignatureName: initialData?.doctorName || 'Dr. Vivek Deshmukh, MVSc',

    // 2. Clinic Infrastructure
    clinicName: initialData?.clinicName || 'Vetalya Advanced Veterinary Hospital & Critical Care',
    practiceType: initialData?.practiceType || 'Multi-Doctor Hospital',
    street: initialData?.street || 'Shop 104-106, Royal Palms Commercial Arcade, Baner High Street',
    landmark: initialData?.landmark || 'Near Jupiter Hospital Signal',
    city: initialData?.city || 'Pune',
    state: initialData?.state || 'Maharashtra',
    pincode: initialData?.pincode || '411045',
    receptionPhone: initialData?.receptionPhone || '+91 20 2729 8812',
    whatsappNumber: initialData?.whatsappNumber || '+91 9921128469',
    facilities: initialData?.facilities || {
      pharmacy: true,
      digitalXray: true,
      ultrasound: true,
      hematologyLab: true,
      surgicalOt: true,
      oxygenIcu: true,
      emergency24x7: true
    },

    // 3. OPD & Pricing
    morningOpd: initialData?.morningOpd || '09:30 AM - 01:30 PM',
    eveningOpd: initialData?.eveningOpd || '05:00 PM - 08:30 PM',
    slotIntervalMins: initialData?.slotIntervalMins || 15,
    firstVisitFee: initialData?.firstVisitFee || 600,
    followUpFee: initialData?.followUpFee || 350,
    teleConsultFee: initialData?.teleConsultFee || 500,
    followUpWindowDays: initialData?.followUpWindowDays || 7
  });

  const toggleSpecialization = (spec) => {
    setFormData(prev => {
      const exists = prev.specializations.includes(spec);
      return {
        ...prev,
        specializations: exists
          ? prev.specializations.filter(s => s !== spec)
          : [...prev.specializations, spec]
      };
    });
  };

  const toggleFacility = (facilityKey) => {
    setFormData(prev => ({
      ...prev,
      facilities: {
        ...prev.facilities,
        [facilityKey]: !prev.facilities[facilityKey]
      }
    }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (onSave) {
      onSave(formData);
    }
    setSaveSuccess(true);
    setTimeout(() => {
      setSaveSuccess(false);
      onClose();
    }, 1200);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm overflow-y-auto">
      <div className="bg-white rounded-3xl max-w-4xl w-full max-h-[92vh] flex flex-col shadow-2xl overflow-hidden border border-gray-100">
        {/* Header */}
        <div className="px-6 py-5 bg-gradient-to-r from-gray-900 via-emerald-950 to-gray-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-emerald-500/20 rounded-xl border border-emerald-400/30">
              <Stethoscope className="w-6 h-6 text-emerald-400" />
            </div>
            <div>
              <h3 className="text-lg font-bold">Doctor Identity & Clinic Practice Infrastructure</h3>
              <p className="text-xs text-gray-300">VCI statutory compliance, clinic equipment & OPD pricing configuration</p>
            </div>
          </div>
          <button 
            onClick={onClose}
            className="p-2 text-gray-400 hover:text-white rounded-xl hover:bg-white/10 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab navigation */}
        <div className="flex border-b border-gray-200 px-6 bg-gray-50/80">
          <button
            onClick={() => setActiveTab('doctor')}
            className={`py-3.5 px-4 font-bold text-sm border-b-2 flex items-center gap-2 cursor-pointer transition-all ${
              activeTab === 'doctor'
                ? 'border-emerald-600 text-emerald-700 bg-white'
                : 'border-transparent text-gray-500 hover:text-gray-900'
            }`}
          >
            <User className="w-4 h-4" />
            <span>1. Doctor Credentials & VCI Compliance</span>
          </button>

          <button
            onClick={() => setActiveTab('clinic')}
            className={`py-3.5 px-4 font-bold text-sm border-b-2 flex items-center gap-2 cursor-pointer transition-all ${
              activeTab === 'clinic'
                ? 'border-emerald-600 text-emerald-700 bg-white'
                : 'border-transparent text-gray-500 hover:text-gray-900'
            }`}
          >
            <Building2 className="w-4 h-4" />
            <span>2. Hospital Practice & Facilities</span>
          </button>

          <button
            onClick={() => setActiveTab('opd')}
            className={`py-3.5 px-4 font-bold text-sm border-b-2 flex items-center gap-2 cursor-pointer transition-all ${
              activeTab === 'opd'
                ? 'border-emerald-600 text-emerald-700 bg-white'
                : 'border-transparent text-gray-500 hover:text-gray-900'
            }`}
          >
            <Clock className="w-4 h-4" />
            <span>3. OPD Timings & Consultation Fees</span>
          </button>
        </div>

        {/* Tab Content */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-6">
          {saveSuccess && (
            <div className="p-4 bg-emerald-50 border border-emerald-300 rounded-xl text-emerald-900 flex items-center gap-2 text-sm font-semibold">
              <CheckCircle2 className="w-5 h-5 text-emerald-600" />
              <span>Practice profile and statutory credentials successfully synchronized!</span>
            </div>
          )}

          {/* TAB 1: DOCTOR CREDENTIALS */}
          {activeTab === 'doctor' && (
            <div className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Doctor Full Name
                  </label>
                  <input
                    type="text"
                    value={formData.doctorName}
                    onChange={(e) => setFormData({ ...formData, doctorName: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-emerald-500 text-sm font-semibold text-gray-900"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Years of Active Practice
                  </label>
                  <input
                    type="number"
                    value={formData.yearsOfExperience}
                    onChange={(e) => setFormData({ ...formData, yearsOfExperience: Number(e.target.value) })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 focus:ring-2 focus:ring-emerald-500 text-sm font-semibold text-gray-900"
                  />
                </div>
              </div>

              {/* Statutory Council Verification */}
              <div className="p-4 rounded-2xl bg-amber-50/70 border border-amber-200 space-y-4">
                <div className="flex items-center gap-2 text-amber-900">
                  <ShieldCheck className="w-5 h-5 text-amber-600" />
                  <span className="font-bold text-sm">Statutory Veterinary Council Verification</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">
                      VCI / State Council Registration Number *
                    </label>
                    <input
                      type="text"
                      value={formData.vciNumber}
                      onChange={(e) => setFormData({ ...formData, vciNumber: e.target.value })}
                      placeholder="e.g. VCI/MH/2016/0491"
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 bg-white font-mono text-sm font-bold text-gray-900"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">
                      Governing State Council *
                    </label>
                    <select
                      value={formData.stateCouncil}
                      onChange={(e) => setFormData({ ...formData, stateCouncil: e.target.value })}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 bg-white text-sm font-semibold text-gray-900"
                    >
                      {STATE_VETERINARY_COUNCILS.map((council, idx) => (
                        <option key={idx} value={council}>{council}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">
                      Registration Year
                    </label>
                    <input
                      type="text"
                      value={formData.regYear}
                      onChange={(e) => setFormData({ ...formData, regYear: e.target.value })}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 bg-white text-sm font-semibold text-gray-900"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">
                      Council Renewal / Expiry Due Date
                    </label>
                    <input
                      type="date"
                      value={formData.expiryDate}
                      onChange={(e) => setFormData({ ...formData, expiryDate: e.target.value })}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 bg-white text-sm font-semibold text-gray-900"
                    />
                  </div>
                </div>
              </div>

              {/* Degrees & Qualifications */}
              <div className="space-y-3">
                <h4 className="font-bold text-sm text-gray-900 flex items-center gap-2">
                  <Award className="w-4 h-4 text-emerald-600" />
                  <span>Academic Degrees & Recognized Qualifications</span>
                </h4>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">Primary Degree (BVSc & AH)</label>
                    <input
                      type="text"
                      value={formData.primaryDegree}
                      onChange={(e) => setFormData({ ...formData, primaryDegree: e.target.value })}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 text-sm text-gray-900"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">Post-Graduation / MVSc Specialization</label>
                    <input
                      type="text"
                      value={formData.postGradDegree}
                      onChange={(e) => setFormData({ ...formData, postGradDegree: e.target.value })}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 text-sm text-gray-900"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 mb-1">Additional Certifications / Fellowships</label>
                  <input
                    type="text"
                    value={formData.additionalCerts}
                    onChange={(e) => setFormData({ ...formData, additionalCerts: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 text-sm text-gray-900"
                  />
                </div>
              </div>

              {/* Specializations Tag Cloud */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-2">
                  Clinical Specializations (Select all that apply)
                </label>
                <div className="flex flex-wrap gap-2">
                  {VET_SPECIALIZATIONS.map((spec, idx) => {
                    const isSelected = formData.specializations.includes(spec);
                    return (
                      <button
                        type="button"
                        key={idx}
                        onClick={() => toggleSpecialization(spec)}
                        className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                          isSelected
                            ? 'bg-emerald-600 text-white shadow-sm'
                            : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                      >
                        {isSelected && '✓ '}
                        {spec}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Clinical Philosophy / Bio */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Doctor Clinical Bio & Practice Philosophy
                </label>
                <textarea
                  rows={3}
                  value={formData.bio}
                  onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
                  className="w-full px-3.5 py-2 rounded-xl border border-gray-300 text-sm text-gray-800"
                />
              </div>

              {/* Digital Stamp Simulation */}
              <div className="p-4 bg-gray-50 border border-gray-200 rounded-2xl flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-gray-700 uppercase block">Digital Prescription Stamp</span>
                  <p className="text-xs text-gray-500 mt-0.5">Automated SVG seal attached to every digital Rx and Rabies certificate.</p>
                </div>
                <div className="border-2 border-emerald-600 border-dashed rounded-xl px-4 py-2 text-center bg-white">
                  <p className="font-serif font-black text-emerald-800 text-xs tracking-wider">SEAL OF REGISTERED VET</p>
                  <p className="text-[11px] font-bold text-gray-900">{formData.doctorName}</p>
                  <p className="text-[10px] font-mono text-emerald-700 font-semibold">{formData.vciNumber}</p>
                </div>
              </div>
            </div>
          )}

          {/* TAB 2: CLINIC INFRASTRUCTURE */}
          {activeTab === 'clinic' && (
            <div className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Hospital / Clinic Brand Name *
                  </label>
                  <input
                    type="text"
                    value={formData.clinicName}
                    onChange={(e) => setFormData({ ...formData, clinicName: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-bold text-sm text-gray-900"
                    required
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Practice Setup Type
                  </label>
                  <select
                    value={formData.practiceType}
                    onChange={(e) => setFormData({ ...formData, practiceType: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-semibold text-sm text-gray-900 bg-white"
                  >
                    <option value="Solo Practice Clinic">Solo Practice Clinic</option>
                    <option value="Multi-Doctor Hospital">Multi-Doctor 24/7 Hospital</option>
                    <option value="Specialist Referral Centre">Specialist Referral Centre</option>
                    <option value="Visiting Surgical Consultant">Visiting Surgical Consultant</option>
                  </select>
                </div>
              </div>

              {/* Complete Address */}
              <div className="space-y-3">
                <h4 className="font-bold text-sm text-gray-900 flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-emerald-600" />
                  <span>Physical Clinic Address (Appears on Owner Invoices & Emergency QR)</span>
                </h4>

                <div>
                  <input
                    type="text"
                    value={formData.street}
                    onChange={(e) => setFormData({ ...formData, street: e.target.value })}
                    placeholder="Street Address, Unit / Shop No."
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 text-sm text-gray-900"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                  <input
                    type="text"
                    value={formData.city}
                    onChange={(e) => setFormData({ ...formData, city: e.target.value })}
                    placeholder="City (e.g. Pune)"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 text-sm text-gray-900"
                  />
                  <input
                    type="text"
                    value={formData.state}
                    onChange={(e) => setFormData({ ...formData, state: e.target.value })}
                    placeholder="State"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 text-sm text-gray-900"
                  />
                  <input
                    type="text"
                    value={formData.pincode}
                    onChange={(e) => setFormData({ ...formData, pincode: e.target.value })}
                    placeholder="6-Digit PIN Code"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-mono text-sm text-gray-900"
                  />
                </div>
              </div>

              {/* Contact numbers */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Reception Landline / Mobile
                  </label>
                  <input
                    type="text"
                    value={formData.receptionPhone}
                    onChange={(e) => setFormData({ ...formData, receptionPhone: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 text-sm font-semibold text-gray-900"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Official WhatsApp Business Number
                  </label>
                  <input
                    type="text"
                    value={formData.whatsappNumber}
                    onChange={(e) => setFormData({ ...formData, whatsappNumber: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 text-sm font-semibold text-gray-900 font-mono"
                  />
                </div>
              </div>

              {/* Facility Toggles */}
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-3">
                  In-House Medical Facilities (Featured on Pet Parent Directory)
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {[
                    { key: 'pharmacy', label: 'In-House Veterinary Pharmacy & Dispensing', desc: 'Licensed Schedule H & Pet drugs' },
                    { key: 'digitalXray', label: 'Digital X-Ray / Radiography Suite', desc: 'High-frequency skeletal & chest imaging' },
                    { key: 'ultrasound', label: 'Diagnostic Ultrasound (USG)', desc: 'Abdominal & Doppler cardiology' },
                    { key: 'hematologyLab', label: 'In-House Hematology & Biochemistry Lab', desc: '15-min instant blood panel' },
                    { key: 'surgicalOt', label: 'Dedicated Sterile Surgical OT', desc: 'Isoflurane anesthesia + vital monitoring' },
                    { key: 'oxygenIcu', label: 'In-Patient Boarding & ICU / Oxygen Cage', desc: 'Post-operative recovery & critical care' },
                    { key: 'emergency24x7', label: 'Emergency 24/7 Triage & Critical Care', desc: 'Night shift veterinary staff on-site' },
                  ].map((item) => {
                    const active = formData.facilities[item.key];
                    return (
                      <div
                        key={item.key}
                        onClick={() => toggleFacility(item.key)}
                        className={`p-3.5 rounded-xl border cursor-pointer transition-all flex items-start gap-3 select-none ${
                          active ? 'bg-emerald-50/70 border-emerald-300' : 'bg-gray-50 border-gray-200'
                        }`}
                      >
                        <div className={`w-5 h-5 rounded-md flex items-center justify-center mt-0.5 ${
                          active ? 'bg-emerald-600 text-white' : 'border border-gray-300 bg-white'
                        }`}>
                          {active && <Check className="w-3.5 h-3.5" />}
                        </div>
                        <div>
                          <p className="text-xs font-bold text-gray-900">{item.label}</p>
                          <p className="text-[11px] text-gray-500 mt-0.5">{item.desc}</p>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* TAB 3: OPD & PRICING */}
          {activeTab === 'opd' && (
            <div className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Morning OPD Timings
                  </label>
                  <input
                    type="text"
                    value={formData.morningOpd}
                    onChange={(e) => setFormData({ ...formData, morningOpd: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-semibold text-sm text-gray-900"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                    Evening OPD Timings
                  </label>
                  <input
                    type="text"
                    value={formData.eveningOpd}
                    onChange={(e) => setFormData({ ...formData, eveningOpd: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 font-semibold text-sm text-gray-900"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">
                  Default Consultation Slot Interval
                </label>
                <div className="flex gap-3">
                  {[15, 20, 30].map((mins) => (
                    <button
                      type="button"
                      key={mins}
                      onClick={() => setFormData({ ...formData, slotIntervalMins: mins })}
                      className={`flex-1 py-2.5 rounded-xl text-sm font-bold border transition-all cursor-pointer ${
                        formData.slotIntervalMins === mins
                          ? 'bg-emerald-600 text-white border-emerald-600'
                          : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                      }`}
                    >
                      {mins} Minutes
                    </button>
                  ))}
                </div>
              </div>

              {/* Consultation Pricing */}
              <div className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-2xl space-y-4">
                <div className="flex items-center gap-2 text-emerald-900">
                  <DollarSign className="w-5 h-5 text-emerald-700" />
                  <span className="font-bold text-sm">Consultation Tariff Configuration</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">
                      First In-Clinic OPD Fee (₹)
                    </label>
                    <input
                      type="number"
                      value={formData.firstVisitFee}
                      onChange={(e) => setFormData({ ...formData, firstVisitFee: Number(e.target.value) })}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 bg-white font-black text-gray-900 text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">
                      Follow-up Visit Fee (₹)
                    </label>
                    <input
                      type="number"
                      value={formData.followUpFee}
                      onChange={(e) => setFormData({ ...formData, followUpFee: Number(e.target.value) })}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 bg-white font-black text-gray-900 text-sm"
                    />
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-gray-700 mb-1">
                      Tele-Consultation Fee (₹)
                    </label>
                    <input
                      type="number"
                      value={formData.teleConsultFee}
                      onChange={(e) => setFormData({ ...formData, teleConsultFee: Number(e.target.value) })}
                      className="w-full px-3.5 py-2.5 rounded-xl border border-gray-300 bg-white font-black text-gray-900 text-sm"
                    />
                  </div>
                </div>

                <div className="text-xs text-emerald-800">
                  Follow-up fee applies when patient returns within <strong>{formData.followUpWindowDays} days</strong> of initial consultation.
                </div>
              </div>
            </div>
          )}

          {/* Footer Save Actions */}
          <div className="pt-4 border-t border-gray-200 flex items-center justify-between">
            <button
              type="button"
              onClick={onClose}
              className="px-5 py-2.5 text-sm font-bold text-gray-600 hover:text-gray-900 cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-sm shadow-md transition-all flex items-center gap-2 cursor-pointer"
            >
              <Check className="w-4 h-4" />
              <span>Save & Publish Practice Profile</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

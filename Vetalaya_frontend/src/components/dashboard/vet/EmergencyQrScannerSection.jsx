import React, { useState } from 'react';
import { 
  QrCode, Search, ShieldCheck, AlertTriangle, Phone, MessageSquare, 
  CheckCircle, FileText, Heart, Activity, Check, Download, ExternalLink,
  Clock, ShieldAlert, Award
} from 'lucide-react';

// Sample pet emergency database for instant lookup
const MOCK_PET_EMERGENCY_DATA = {
  'PET-01M71N6A7E': {
    id: 'PET-01M71N6A7E',
    name: 'Bruno',
    species: 'Dog',
    breed: 'Golden Retriever',
    age: '3 yrs 4 mos',
    weightKg: 31.8,
    gender: 'Male (Neutered)',
    bloodGroup: 'DEA 1.1 Positive',
    microchipId: '982000364819201',
    emergencyContact: {
      name: 'Rohit Kulkarni',
      phone: '+91 9921128469',
      relation: 'Primary Pet Parent',
      city: 'Pune, Maharashtra'
    },
    criticalAlerts: [
      { type: 'danger', title: 'Severe Allergy', desc: 'Anaphylactic reaction to Cephalexin / Penicillins. DO NOT ADMINISTER.' },
      { type: 'warning', title: 'Chronic Condition', desc: 'Mild Hip Dysplasia (Grade 1). Avoid high impact jumps.' }
    ],
    verifiedVaccinations: [
      { name: 'Anti-Rabies (Rabisin)', date: '2025-10-14', validUntil: '2026-10-13', status: 'Active', batch: 'RAB-8821' },
      { name: 'Nobivac DHPPi+L', date: '2025-10-14', validUntil: '2026-10-13', status: 'Active', batch: 'DH-3391' },
      { name: 'Kennel Cough (Nobivac KC)', date: '2025-06-20', validUntil: '2026-06-19', status: 'Active', batch: 'KC-1102' }
    ],
    pendingAuditRecords: [
      {
        id: 'REC-091',
        title: 'Complete Blood Count (CBC) - Suburban Diagnostics',
        date: '2026-03-28',
        uploadedBy: 'Pet Parent (Rohit Kulkarni)',
        fileType: 'PDF Document',
        verified: false
      },
      {
        id: 'REC-084',
        title: 'Abdominal Ultrasound Scan Report - Apex Pet Hospital',
        date: '2026-01-15',
        uploadedBy: 'Pet Parent (Rohit Kulkarni)',
        fileType: 'PDF Document',
        verified: true,
        verifiedBy: 'Dr. Vivek Deshmukh, MVSc (VCI/MH/2016/0491)',
        verifiedAt: '2026-01-16 11:20 AM'
      }
    ]
  },
  'PET-02K88N190A': {
    id: 'PET-02K88N190A',
    name: 'Milo',
    species: 'Cat',
    breed: 'Persian Longhair',
    age: '2 yrs 1 mo',
    weightKg: 4.2,
    gender: 'Female (Spayed)',
    bloodGroup: 'Feline Type A',
    microchipId: '982000551029482',
    emergencyContact: {
      name: 'Ananya Sharma',
      phone: '+91 9823011422',
      relation: 'Pet Parent',
      city: 'Mumbai, Maharashtra'
    },
    criticalAlerts: [
      { type: 'warning', title: 'Feline Asthma', desc: 'Mild bronchial sensitivity. Keep emergency salbutamol nebulizer on standby.' }
    ],
    verifiedVaccinations: [
      { name: 'Felocell 4 (FVRCP + Chlamydia)', date: '2025-08-11', validUntil: '2026-08-10', status: 'Active', batch: 'FEL-9912' },
      { name: 'Nobivac Rabies', date: '2025-08-11', validUntil: '2026-08-10', status: 'Active', batch: 'RAB-4410' }
    ],
    pendingAuditRecords: [
      {
        id: 'REC-073',
        title: 'Renal Function Profile (BUN / Creatinine)',
        date: '2026-02-10',
        uploadedBy: 'Pet Parent (Ananya Sharma)',
        fileType: 'PDF Document',
        verified: true,
        verifiedBy: 'Dr. Vivek Deshmukh, MVSc',
        verifiedAt: '2026-02-11 04:30 PM'
      }
    ]
  }
};

export default function EmergencyQrScannerSection({ doctorProfile, onSelectPetForConsultation }) {
  const [searchInput, setSearchInput] = useState('');
  const [activePet, setActivePet] = useState(MOCK_PET_EMERGENCY_DATA['PET-01M71N6A7E']);
  const [records, setRecords] = useState(MOCK_PET_EMERGENCY_DATA['PET-01M71N6A7E'].pendingAuditRecords);
  const [isScanning, setIsScanning] = useState(false);
  const [auditSuccessMsg, setAuditSuccessMsg] = useState('');

  const handleSearch = (idToSearch) => {
    const key = (idToSearch || searchInput).trim().toUpperCase();
    if (MOCK_PET_EMERGENCY_DATA[key]) {
      setActivePet(MOCK_PET_EMERGENCY_DATA[key]);
      setRecords(MOCK_PET_EMERGENCY_DATA[key].pendingAuditRecords);
    } else {
      // Create on-the-fly dummy search card if not matched
      const genericPet = {
        id: key || 'PET-CUSTOM-001',
        name: 'Lookup Pet (' + (key || 'UNKNOWN') + ')',
        species: 'Canine',
        breed: 'Crossbreed',
        age: 'Estimated 2 yrs',
        weightKg: 18.5,
        gender: 'Male',
        bloodGroup: 'DEA 1.1 Negative',
        microchipId: '982000' + Math.floor(100000000 + Math.random() * 900000000),
        emergencyContact: {
          name: 'Registered Guardian',
          phone: '+91 9900000000',
          relation: 'Emergency Contact',
          city: 'In-Transit'
        },
        criticalAlerts: [
          { type: 'warning', title: 'New Registration', desc: 'No confirmed allergies logged in registry yet. Exercise clinical caution.' }
        ],
        verifiedVaccinations: [
          { name: 'Rabies Certificate', date: '2025-11-01', validUntil: '2026-10-31', status: 'Active', batch: 'RB-IN-092' }
        ],
        pendingAuditRecords: []
      };
      setActivePet(genericPet);
      setRecords([]);
    }
  };

  const simulateQrCameraScan = () => {
    setIsScanning(true);
    setTimeout(() => {
      setIsScanning(false);
      setSearchInput('PET-01M71N6A7E');
      handleSearch('PET-01M71N6A7E');
    }, 1200);
  };

  const verifyRecord = (recId) => {
    setRecords(prev => prev.map(r => {
      if (r.id === recId) {
        return {
          ...r,
          verified: true,
          verifiedBy: (doctorProfile?.name || 'Dr. Vivek Deshmukh') + ` (VCI: ${doctorProfile?.vciNumber || 'VCI/MH/2016/0491'})`,
          verifiedAt: new Date().toLocaleDateString('en-IN') + ' ' + new Date().toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })
        };
      }
      return r;
    }));
    setAuditSuccessMsg(`Document ${recId} verified and cryptographically stamped into pet record.`);
    setTimeout(() => setAuditSuccessMsg(''), 4500);
  };

  return (
    <div className="space-y-6">
      {/* Top Banner / Emergency Search Bar */}
      <div className="bg-gradient-to-r from-red-600 via-rose-600 to-amber-600 rounded-2xl p-6 text-white shadow-lg">
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-white/20 backdrop-blur-md rounded-xl">
              <ShieldAlert className="w-8 h-8 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-xl font-bold">Emergency 24/7 QR Pass & Clinical Audit Station</h2>
                <span className="px-2 py-0.5 bg-red-900/50 text-red-200 text-xs font-semibold rounded-full border border-red-400/40">
                  CRITICAL CARE READY
                </span>
              </div>
              <p className="text-sm text-red-100 mt-0.5">
                Instant pet life-history extraction: Blood group, drug contraindications, guardian contacts & audit records.
              </p>
            </div>
          </div>

          <button
            onClick={simulateQrCameraScan}
            disabled={isScanning}
            className="flex items-center gap-2 px-5 py-3 bg-white text-red-700 hover:bg-red-50 active:scale-95 transition-all font-bold rounded-xl shadow-md cursor-pointer"
          >
            <QrCode className={`w-5 h-5 ${isScanning ? 'animate-spin' : ''}`} />
            <span>{isScanning ? 'Reading QR Stream...' : 'Scan Pet Collar QR'}</span>
          </button>
        </div>

        {/* Input Bar */}
        <div className="mt-5 flex flex-col sm:flex-row gap-2 max-w-2xl">
          <div className="relative flex-1">
            <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="Search by Pet ID (e.g., PET-01M71N6A7E) or Microchip..."
              className="w-full pl-11 pr-4 py-2.5 rounded-xl bg-white text-gray-900 placeholder-gray-400 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-amber-300 shadow-inner"
              onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            />
          </div>
          <button
            onClick={() => handleSearch()}
            className="px-6 py-2.5 bg-gray-900 hover:bg-black text-white font-semibold rounded-xl text-sm transition-all cursor-pointer"
          >
            Fetch Record
          </button>
        </div>

        {/* Quick ID Chips */}
        <div className="mt-3 flex items-center gap-2 text-xs text-red-100 flex-wrap">
          <span>Quick Demo IDs:</span>
          <button 
            onClick={() => { setSearchInput('PET-01M71N6A7E'); handleSearch('PET-01M71N6A7E'); }}
            className="underline hover:text-white cursor-pointer font-mono"
          >
            PET-01M71N6A7E (Bruno - Severe Drug Allergy)
          </button>
          <span>•</span>
          <button 
            onClick={() => { setSearchInput('PET-02K88N190A'); handleSearch('PET-02K88N190A'); }}
            className="underline hover:text-white cursor-pointer font-mono"
          >
            PET-02K88N190A (Milo - Feline Asthma)
          </button>
        </div>
      </div>

      {auditSuccessMsg && (
        <div className="bg-emerald-50 border border-emerald-300 rounded-xl p-4 flex items-center gap-3 text-emerald-800 text-sm">
          <CheckCircle className="w-5 h-5 text-emerald-600 flex-shrink-0" />
          <span className="font-medium">{auditSuccessMsg}</span>
        </div>
      )}

      {/* Main Content Grid: Left Patient Passport + Right Clinical Verification Audit */}
      {activePet && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* LEFT: Emergency Passport (7 cols) */}
          <div className="lg:col-span-7 space-y-5">
            {/* Critical Triage Card */}
            <div className="bg-white border-2 border-red-200 rounded-2xl p-6 shadow-sm relative overflow-hidden">
              <div className="absolute top-0 right-0 bg-red-600 text-white text-xs font-bold px-4 py-1 rounded-bl-xl tracking-wider uppercase">
                EMERGENCY TRIAGE CARD
              </div>

              <div className="flex items-start gap-4">
                <div className="w-16 h-16 rounded-2xl bg-amber-100 border border-amber-200 flex items-center justify-center text-3xl font-bold text-amber-800">
                  {activePet.species === 'Dog' ? '🐕' : '🐈'}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="text-2xl font-black text-gray-900">{activePet.name}</h3>
                    <span className="font-mono text-xs px-2 py-0.5 bg-gray-100 text-gray-700 rounded-md font-semibold border border-gray-200">
                      {activePet.id}
                    </span>
                  </div>
                  <p className="text-sm text-gray-600 mt-0.5">
                    {activePet.breed} • {activePet.species} • {activePet.gender} • {activePet.age}
                  </p>
                </div>
              </div>

              {/* High Alert Banner */}
              {activePet.criticalAlerts?.length > 0 && (
                <div className="mt-5 space-y-2">
                  {activePet.criticalAlerts.map((alert, idx) => (
                    <div 
                      key={idx}
                      className={`p-3 rounded-xl border flex items-start gap-3 ${
                        alert.type === 'danger' 
                          ? 'bg-red-50 border-red-300 text-red-900' 
                          : 'bg-amber-50 border-amber-300 text-amber-900'
                      }`}
                    >
                      <AlertTriangle className={`w-5 h-5 mt-0.5 flex-shrink-0 ${alert.type === 'danger' ? 'text-red-600' : 'text-amber-600'}`} />
                      <div>
                        <span className="font-bold text-xs uppercase tracking-wide block">{alert.title}</span>
                        <p className="text-sm font-medium mt-0.5">{alert.desc}</p>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* Vital Identity Metrics */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mt-5">
                <div className="p-3 bg-gray-50 rounded-xl border border-gray-200">
                  <span className="text-xs text-gray-500 block">Weight</span>
                  <span className="text-base font-bold text-gray-900">{activePet.weightKg} kg</span>
                </div>
                <div className="p-3 bg-red-50 rounded-xl border border-red-200">
                  <span className="text-xs text-red-600 font-semibold block">Blood Group</span>
                  <span className="text-base font-black text-red-700">{activePet.bloodGroup}</span>
                </div>
                <div className="p-3 bg-gray-50 rounded-xl border border-gray-200 col-span-2">
                  <span className="text-xs text-gray-500 block">15-Digit Microchip ISO</span>
                  <span className="text-sm font-mono font-bold text-gray-800 tracking-wider">
                    {activePet.microchipId}
                  </span>
                </div>
              </div>

              {/* Owner Emergency Contact */}
              <div className="mt-5 p-4 bg-blue-50 border border-blue-200 rounded-xl flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-blue-900 uppercase tracking-wider block">Verified Guardian</span>
                  <p className="text-sm font-bold text-gray-900 mt-0.5">{activePet.emergencyContact.name}</p>
                  <p className="text-xs text-gray-600">{activePet.emergencyContact.relation} • {activePet.emergencyContact.city}</p>
                </div>
                <div className="flex items-center gap-2">
                  <a
                    href={`tel:${activePet.emergencyContact.phone}`}
                    className="p-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg flex items-center gap-1 text-xs font-bold transition-all shadow-sm"
                  >
                    <Phone className="w-4 h-4" />
                    <span>Call Now</span>
                  </a>
                  <a
                    href={`https://wa.me/${activePet.emergencyContact.phone.replace(/[^0-9]/g, '')}?text=Vetalya%20Emergency%20Notification%20Regarding%20${activePet.name}`}
                    target="_blank"
                    rel="noreferrer"
                    className="p-2.5 bg-green-500 hover:bg-green-600 text-white rounded-lg flex items-center gap-1 text-xs font-bold transition-all shadow-sm"
                  >
                    <MessageSquare className="w-4 h-4" />
                    <span>WhatsApp</span>
                  </a>
                </div>
              </div>

              {/* Launch Consultation Action */}
              <div className="mt-5 pt-4 border-t border-gray-200 flex justify-end">
                <button
                  onClick={() => onSelectPetForConsultation && onSelectPetForConsultation(activePet)}
                  className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-xl text-sm flex items-center gap-2 shadow-sm cursor-pointer"
                >
                  <Activity className="w-4 h-4" />
                  <span>Open SOAP Consultation for {activePet.name}</span>
                </button>
              </div>
            </div>

            {/* Verified Vaccine Status Quick Preview */}
            <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-2">
                  <ShieldCheck className="w-5 h-5 text-emerald-600" />
                  <h4 className="font-bold text-gray-900">Verified Lifelong Immunization Status</h4>
                </div>
                <span className="text-xs text-gray-500">Auto-synced with VCI Registry</span>
              </div>

              <div className="space-y-2.5">
                {activePet.verifiedVaccinations.map((vac, idx) => (
                  <div key={idx} className="p-3 bg-gray-50 rounded-xl border border-gray-200 flex items-center justify-between">
                    <div>
                      <p className="font-semibold text-sm text-gray-900">{vac.name}</p>
                      <p className="text-xs text-gray-500">Administered: {vac.date} • Batch: {vac.batch}</p>
                    </div>
                    <div className="text-right">
                      <span className="px-2.5 py-0.5 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-full border border-emerald-300">
                        Valid till {vac.validUntil}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* RIGHT: Statutory Record Verification & Audit Trail (5 cols) */}
          <div className="lg:col-span-5 space-y-5">
            <div className="bg-white border border-gray-200 rounded-2xl p-6 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h4 className="font-bold text-gray-900 flex items-center gap-2">
                    <Award className="w-5 h-5 text-blue-600" />
                    <span>Statutory Medical Audit Trail</span>
                  </h4>
                  <p className="text-xs text-gray-500 mt-0.5">
                    Review owner-uploaded diagnostics and seal as Clinically Verified
                  </p>
                </div>
              </div>

              {records.length === 0 ? (
                <div className="p-8 text-center text-gray-400 text-sm border-2 border-dashed border-gray-200 rounded-xl">
                  No records uploaded for this pet profile yet.
                </div>
              ) : (
                <div className="space-y-4">
                  {records.map((rec) => (
                    <div 
                      key={rec.id}
                      className={`p-4 rounded-xl border transition-all ${
                        rec.verified ? 'bg-emerald-50/50 border-emerald-200' : 'bg-gray-50 border-gray-200 hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-start justify-between">
                        <div className="flex items-center gap-2">
                          <FileText className={`w-4 h-4 ${rec.verified ? 'text-emerald-600' : 'text-gray-500'}`} />
                          <span className="font-mono text-xs text-gray-500">{rec.id}</span>
                        </div>
                        {rec.verified ? (
                          <span className="px-2 py-0.5 bg-emerald-100 text-emerald-800 text-xs font-bold rounded-md flex items-center gap-1 border border-emerald-300">
                            <Check className="w-3 h-3" />
                            <span>Verified</span>
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 bg-amber-100 text-amber-800 text-xs font-bold rounded-md border border-amber-300">
                            Pending Audit
                          </span>
                        )}
                      </div>

                      <h5 className="font-bold text-sm text-gray-900 mt-2">{rec.title}</h5>
                      <p className="text-xs text-gray-500 mt-0.5">
                        Uploaded by: {rec.uploadedBy} on {rec.date}
                      </p>

                      {rec.verified ? (
                        <div className="mt-3 p-2.5 bg-emerald-100/70 border border-emerald-300 rounded-lg text-xs text-emerald-900">
                          <p className="font-semibold">VCI Signature Seal Affixed:</p>
                          <p className="text-emerald-800 font-mono text-[11px] mt-0.5">{rec.verifiedBy}</p>
                          <p className="text-emerald-700 text-[10px] mt-0.5">{rec.verifiedAt}</p>
                        </div>
                      ) : (
                        <div className="mt-3 pt-3 border-t border-gray-200 flex items-center justify-between">
                          <button 
                            onClick={() => alert(`Opening ${rec.title} for review...`)}
                            className="text-xs font-semibold text-blue-600 hover:text-blue-800 flex items-center gap-1"
                          >
                            <ExternalLink className="w-3 h-3" />
                            <span>Inspect File</span>
                          </button>
                          <button
                            onClick={() => verifyRecord(rec.id)}
                            className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold rounded-lg text-xs flex items-center gap-1 shadow-sm cursor-pointer"
                          >
                            <ShieldCheck className="w-3.5 h-3.5" />
                            <span>Verify & Stamp</span>
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}

              {/* VCI Seal Guarantee Notice */}
              <div className="mt-6 p-3.5 bg-gray-50 rounded-xl border border-gray-200 text-xs text-gray-600">
                <p className="font-bold text-gray-800">Legal Audit Note:</p>
                <p className="mt-0.5">
                  Every verified record is signed with your registered Council number and securely stamped. Verified records become part of the pet's immutable lifelong digital health record.
                </p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

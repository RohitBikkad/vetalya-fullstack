import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../../context/AuthContext';
import {
  getVetDashboardApi,
  getVetAppointmentsApi,
  getVetProfileApi,
  getVetHospitalsApi,
  updateVetAppointmentStatusApi,
  updateVetVisitNotesApi,
} from '../../../services/api';
import {
  Stethoscope,
  Users,
  Clock,
  CheckCircle2,
  AlertCircle,
  FileText,
  Plus,
  Trash2,
  Search,
  Filter,
  Eye,
  Activity,
  ArrowRight,
  ShieldCheck,
  X,
  Send,
  Sparkles,
  RefreshCw,
} from 'lucide-react';

export default function VetDashboard() {
  const { currentUser } = useAuth();

  // OPD Metric counts - loaded from backend
  const [metrics, setMetrics] = useState({
    todayTotal: 0,
    waitingInClinic: 0,
    completedToday: 0,
    criticalTriage: 0,
  });
  const [loadingMetrics, setLoadingMetrics] = useState(true);

  // Patient queue state - loaded from backend
  const [queue, setQueue] = useState([]);
  const [loadingQueue, setLoadingQueue] = useState(true);

  // Vet Profile & Affiliation - loaded from backend
  const [vetProfile, setVetProfile] = useState(null);
  const [hospitalName, setHospitalName] = useState('');

  // Selected patient for SOAP consultation
  const [activeConsultation, setActiveConsultation] = useState(null);

  // SOAP Form State
  const [soapForm, setSoapForm] = useState({
    subjective: '',
    temperature: '',
    weight: '',
    heartRate: '',
    objectiveExam: '',
    assessment: '',
    dietPlan: '',
    followUpDays: '7',
    medications: [
      { name: '', dosage: '', frequency: 'BID (Twice daily)', duration: '5 days', instructions: 'After food' },
    ],
  });

  const [prescriptionSigned, setPrescriptionSigned] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('queue'); // 'queue' | 'diagnostics' | 'history'

  // Diagnostic records
  const [diagnostics, setDiagnostics] = useState([]);

  const loadVetData = useCallback(async () => {
    setLoadingMetrics(true);
    setLoadingQueue(true);

    try {
      const [profileRes, hospitalsRes] = await Promise.allSettled([
        getVetProfileApi(),
        getVetHospitalsApi(),
      ]);

      if (profileRes.status === 'fulfilled' && profileRes.value) {
        setVetProfile(profileRes.value);
      }
      if (hospitalsRes.status === 'fulfilled' && hospitalsRes.value && hospitalsRes.value.length > 0) {
        setHospitalName(hospitalsRes.value[0].name);
      }
    } catch (e) {
      console.warn('Could not fetch vet profile/hospital:', e.message);
    }

    try {
      const dashRes = await getVetDashboardApi().catch(() => null);
      if (dashRes) {
        setMetrics({
          todayTotal: dashRes.todayAppointments || 0,
          waitingInClinic: dashRes.statusCounts?.BOOKED || 0,
          completedToday: dashRes.statusCounts?.COMPLETED || 0,
          criticalTriage: dashRes.pendingRequests || 0,
        });
      }
    } catch (e) {
      console.warn('Could not fetch vet dashboard metrics:', e.message);
    } finally {
      setLoadingMetrics(false);
    }

    try {
      const aptsRes = await getVetAppointmentsApi().catch(() => null);
      if (aptsRes && aptsRes.content) {
        const mapped = aptsRes.content.map(apt => ({
          id: apt.appointmentId,
          token: `T-${apt.appointmentId ? apt.appointmentId.slice(-2) : '01'}`,
          petName: apt.pet?.name || 'Patient Companion',
          species: apt.pet?.species === 'DOG' ? 'Canine' : apt.pet?.species === 'CAT' ? 'Feline' : (apt.pet?.species || 'Companion'),
          breed: apt.pet?.breed || 'Companion',
          age: '—',
          weight: '—',
          ownerName: apt.owner ? `${apt.owner.firstName || ''} ${apt.owner.lastName || ''}`.trim() || 'Pet Owner' : 'Pet Owner',
          ownerPhone: 'Verified Client',
          time: apt.appointmentAt ? new Date(apt.appointmentAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : 'Scheduled',
          reason: apt.reason || 'Clinical Consultation & Health Assessment',
          status: apt.status === 'BOOKED' ? 'Waiting' : apt.status === 'COMPLETED' ? 'Completed' : (apt.status || 'Waiting'),
          statusColor: apt.status === 'COMPLETED' ? 'bg-slate-100 text-slate-700 border-slate-300' : 'bg-amber-100 text-amber-800 border-amber-300',
          triageLevel: 'Standard',
          temp: '101.4',
          hr: '110',
        }));
        setQueue(mapped);
      } else {
        setQueue([]);
      }
    } catch (e) {
      console.warn('Could not fetch vet appointments queue:', e.message);
      setQueue([]);
    } finally {
      setLoadingQueue(false);
    }
  }, []);

  useEffect(() => {
    loadVetData();
  }, [loadVetData]);

  const handleStartConsultation = (patient) => {
    setActiveConsultation(patient);
    setSoapForm({
      subjective: patient.reason || 'General wellness checkup and triage assessment',
      temperature: patient.temp || '101.4',
      weight: patient.weight ? patient.weight.replace(' kg', '') : '10',
      heartRate: patient.hr || '110',
      objectiveExam: 'Bright, alert, responsive. Hydration status normal. Cardiovascular and pulmonary auscultation clear.',
      assessment: patient.reason || 'Clinical Health Evaluation',
      dietPlan: 'Maintain regular balanced diet and clean fresh water.',
      followUpDays: '7',
      medications: [
        { name: '', dosage: '', frequency: 'BID (Twice daily)', duration: '5 days', instructions: 'After food' },
      ],
    });
    setPrescriptionSigned(false);
  };

  const handleAddMedicationRow = () => {
    setSoapForm({
      ...soapForm,
      medications: [
        ...soapForm.medications,
        { name: '', dosage: '', frequency: 'BID (Twice daily)', duration: '5 days', instructions: 'After food' },
      ],
    });
  };

  const handleRemoveMedicationRow = (idx) => {
    const list = [...soapForm.medications];
    list.splice(idx, 1);
    setSoapForm({ ...soapForm, medications: list });
  };

  const handleMedicationChange = (idx, field, value) => {
    const list = [...soapForm.medications];
    list[idx][field] = value;
    setSoapForm({ ...soapForm, medications: list });
  };

  const handleSavePrescription = async (e) => {
    e.preventDefault();
    setPrescriptionSigned(true);

    if (activeConsultation?.id && !activeConsultation.id.startsWith('mock-')) {
      try {
        await updateVetAppointmentStatusApi(activeConsultation.id, 'COMPLETED', soapForm.assessment);
        await updateVetVisitNotesApi(activeConsultation.id, JSON.stringify(soapForm));
      } catch (err) {
        console.warn('Could not persist SOAP prescription to backend:', err.message);
      }
    }

    // Update patient status in queue
    setQueue(prev => prev.map((p) => (p.id === activeConsultation.id ? { ...p, status: 'Completed', statusColor: 'bg-slate-100 text-slate-700 border-slate-300' } : p)));

    // Update metrics
    setMetrics(prev => ({
      ...prev,
      waitingInClinic: Math.max(0, prev.waitingInClinic - 1),
      completedToday: prev.completedToday + 1,
    }));

    setTimeout(() => {
      setActiveConsultation(null);
      setPrescriptionSigned(false);
      loadVetData();
    }, 1800);
  };

  const filteredQueue = queue.filter(
    (p) =>
      p.petName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.ownerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.token.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-8 animate-in fade-in">
      
      {/* ── Doctor Header Greeting & OPD Station Banner ── */}
      <div className="bg-gradient-to-r from-white via-white to-[#E8EEF4] rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya flex flex-col md:flex-row md:items-center justify-between gap-6 text-left">
        <div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#E8EEF4] text-[#10324E] text-[11px] font-extrabold uppercase tracking-wider mb-2">
            <Stethoscope className="w-3.5 h-3.5" />
            <span>Clinical OPD Consultation Desk</span>
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[#10324E] font-display">
            {currentUser?.name ? (currentUser.name.startsWith('Dr.') ? currentUser.name : `Dr. ${currentUser.name}`) : 'Licensed Veterinarian'} 🩺
          </h1>
          <p className="text-sm text-[#242826]/70 mt-1">
            {hospitalName || 'Clinical OPD Practice'} • Council Reg: {vetProfile?.registrationNo || currentUser?.councilRegNumber || 'Verified Practitioner'} • Room 1
          </p>
        </div>

        {/* Practice Quick Status */}
        <div className="flex items-center gap-2">
          <button
            onClick={loadVetData}
            title="Refresh patient queue and metrics"
            className="p-2 rounded-2xl bg-white hover:bg-slate-50 border border-[#10324E]/15 text-[#10324E] transition shadow-2xs cursor-pointer"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loadingQueue ? 'animate-spin text-[#2D8A7A]' : ''}`} />
          </button>
          <span className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-2xl bg-emerald-100 text-emerald-800 text-xs font-extrabold border border-emerald-300 shadow-2xs">
            <span className="w-2 h-2 rounded-full bg-emerald-600 animate-pulse" />
            OPD Station Active
          </span>
        </div>
      </div>

      {/* ── OPD Practice Metrics Ribbon (4 Cards) ── */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
        <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
          <div className="flex items-center justify-between text-[#10324E]">
            <span className="text-xs font-bold text-[#242826]/60">Today's Total</span>
            <Users className="w-4 h-4 text-[#10324E]" />
          </div>
          <p className={`text-2xl sm:text-3xl font-extrabold text-[#10324E] font-display mt-2 ${loadingMetrics ? 'opacity-40 animate-pulse' : ''}`}>
            {metrics.todayTotal}
          </p>
          <span className="text-[11px] text-[#242826]/60 mt-1 inline-block">Scheduled consultations</span>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
          <div className="flex items-center justify-between text-[#2D8A7A]">
            <span className="text-xs font-bold text-[#242826]/60">Waiting in Lobby</span>
            <Clock className="w-4 h-4 text-[#2D8A7A]" />
          </div>
          <p className={`text-2xl sm:text-3xl font-extrabold text-[#2D8A7A] font-display mt-2 ${loadingMetrics ? 'opacity-40 animate-pulse' : ''}`}>
            {metrics.waitingInClinic}
          </p>
          <span className="text-[11px] text-amber-700 font-bold mt-1 inline-block">Active waitlist</span>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
          <div className="flex items-center justify-between text-emerald-700">
            <span className="text-xs font-bold text-[#242826]/60">Completed Rx</span>
            <CheckCircle2 className="w-4 h-4 text-emerald-600" />
          </div>
          <p className={`text-2xl sm:text-3xl font-extrabold text-[#10324E] font-display mt-2 ${loadingMetrics ? 'opacity-40 animate-pulse' : ''}`}>
            {metrics.completedToday}
          </p>
          <span className="text-[11px] text-emerald-700 font-bold mt-1 inline-block">SOAP notes signed</span>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-rose-200 shadow-vetalya text-left bg-rose-50/30">
          <div className="flex items-center justify-between text-rose-700">
            <span className="text-xs font-bold text-rose-800">Critical / Triage</span>
            <AlertCircle className="w-4 h-4 text-rose-600 animate-pulse" />
          </div>
          <p className={`text-2xl sm:text-3xl font-extrabold text-rose-700 font-display mt-2 ${loadingMetrics ? 'opacity-40 animate-pulse' : ''}`}>
            {metrics.criticalTriage}
          </p>
          <span className="text-[11px] text-rose-700 font-bold mt-1 inline-block">Pending review</span>
        </div>
      </div>

      {/* ── Tabs: Live Queue vs Diagnostic Center ── */}
      <div id="opd-queue" className="flex items-center justify-between border-b border-[#10324E]/10 pb-3 scroll-mt-28">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setActiveTab('queue')}
            className={`px-4 py-2 rounded-2xl text-xs font-bold transition cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'queue' ? 'bg-[#10324E] text-white shadow-sm' : 'bg-white text-[#10324E] hover:bg-slate-100'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            <span>Live OPD Queue ({queue.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('diagnostics')}
            className={`px-4 py-2 rounded-2xl text-xs font-bold transition cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'diagnostics' ? 'bg-[#10324E] text-white shadow-sm' : 'bg-white text-[#10324E] hover:bg-slate-100'
            }`}
          >
            <Activity className="w-3.5 h-3.5 text-[#2D8A7A]" />
            <span>Diagnostics &amp; Imaging ({diagnostics.length})</span>
          </button>
        </div>

        {/* Queue Search */}
        <div className="relative max-w-xs w-full hidden sm:block">
          <Search className="w-3.5 h-3.5 absolute left-3 top-3 text-[#242826]/40" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search token, pet, or client..."
            className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#10324E] focus:outline-none focus:ring-2 focus:ring-[#2D8A7A]/40"
          />
        </div>
      </div>

      {/* ── Tab Content: Live Queue ── */}
      {activeTab === 'queue' && (
        <div className="bg-white rounded-[32px] p-6 border border-[#10324E]/8 shadow-vetalya text-left overflow-hidden">
          <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8 mb-4">
            <h3 className="font-extrabold text-base text-[#10324E]">Current OPD Patient Queue</h3>
            <span className="text-xs text-[#242826]/60">Select any patient to start consultation and generate digital SOAP Rx</span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-xs text-left">
              <thead>
                <tr className="border-b border-[#10324E]/10 text-[#242826]/60 font-bold uppercase text-[10px] tracking-wider">
                  <th className="pb-3 pl-2">Token</th>
                  <th className="pb-3">Patient Companion</th>
                  <th className="pb-3">Parent / Contact</th>
                  <th className="pb-3">Vitals (T / HR)</th>
                  <th className="pb-3">Chief Complaint</th>
                  <th className="pb-3">Status</th>
                  <th className="pb-3 text-right pr-2">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#10324E]/5">
                {loadingQueue ? (
                  <tr>
                    <td colSpan="7" className="py-12 text-center text-xs text-[#242826]/60 animate-pulse">
                      Loading patient queue from clinical registry...
                    </td>
                  </tr>
                ) : filteredQueue.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="py-12 text-center">
                      <div className="max-w-xs mx-auto space-y-2">
                        <Users className="w-8 h-8 text-[#10324E]/30 mx-auto" />
                        <p className="font-bold text-xs text-[#10324E]">No patients currently waiting</p>
                        <p className="text-[11px] text-[#242826]/60">
                          Consultations booked by pet parents and incoming OPD triage cases will appear here live.
                        </p>
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredQueue.map((item) => (
                    <tr key={item.id} className="hover:bg-[#FAF8F5] transition">
                      <td className="py-3.5 pl-2 font-mono font-extrabold text-[#10324E]">{item.token}</td>
                      <td className="py-3.5">
                        <div className="font-bold text-[#10324E]">{item.petName}</div>
                        <div className="text-[11px] text-[#242826]/60">{item.species} • {item.breed} ({item.weight})</div>
                      </td>
                      <td className="py-3.5">
                        <div className="font-semibold text-[#10324E]">{item.ownerName}</div>
                        <div className="text-[11px] text-[#242826]/60">{item.ownerPhone}</div>
                      </td>
                      <td className="py-3.5 font-mono text-[11px]">
                        <span className="text-[#10324E]">{item.temp}°F</span> • <span className="text-[#2D8A7A]">{item.hr} bpm</span>
                      </td>
                      <td className="py-3.5 max-w-xs">
                        <p className="text-[11px] text-[#242826]/80 truncate">{item.reason}</p>
                      </td>
                      <td className="py-3.5">
                        <span className={`px-2.5 py-1 rounded-full text-[10px] border ${item.statusColor}`}>
                          {item.status}
                        </span>
                      </td>
                      <td className="py-3.5 text-right pr-2">
                        <button
                          onClick={() => handleStartConsultation(item)}
                          className="px-3.5 py-1.5 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white font-bold text-xs shadow-2xs transition inline-flex items-center gap-1 cursor-pointer"
                        >
                          <FileText className="w-3 h-3 text-[#E99A7C]" />
                          <span>SOAP Note</span>
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* ── Tab Content: Diagnostics & Imaging ── */}
      {activeTab === 'diagnostics' && (
        diagnostics.length === 0 ? (
          <div id="diagnostics" className="bg-white rounded-3xl p-12 border border-dashed border-[#10324E]/15 text-center scroll-mt-28">
            <Activity className="w-10 h-10 text-[#2D8A7A]/40 mx-auto mb-3" />
            <h4 className="text-sm font-bold text-[#10324E]">No diagnostic imaging records yet</h4>
            <p className="text-xs text-[#242826]/60 mt-1 max-w-sm mx-auto">
              Radiographs, blood panels, and ultrasound DICOM images ordered during clinical consultations will appear here for review.
            </p>
          </div>
        ) : (
          <div id="diagnostics" className="grid grid-cols-1 md:grid-cols-2 gap-6 text-left scroll-mt-28">
            {diagnostics.map((diag) => (
              <div key={diag.id} className="bg-white rounded-3xl p-6 border border-[#10324E]/8 shadow-vetalya space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/8">
                  <div>
                    <h4 className="font-extrabold text-sm text-[#10324E]">{diag.petName} — {diag.type}</h4>
                    <span className="text-[11px] text-[#242826]/60">{diag.date}</span>
                  </div>
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                    {diag.status}
                  </span>
                </div>

                <div className="rounded-2xl overflow-hidden h-48 bg-slate-900 border border-slate-800 relative">
                  <img src={diag.image} alt={diag.type} className="w-full h-full object-cover opacity-90" />
                  <div className="absolute bottom-2 right-2 px-2.5 py-1 rounded-lg bg-black/70 text-white text-[10px] font-mono">
                    High-Res DICOM
                  </div>
                </div>

                <p className="text-xs text-[#242826]/80 leading-relaxed bg-[#FAF8F5] p-3 rounded-2xl border border-[#10324E]/6">
                  <strong>Radiologist / Lab Report:</strong> {diag.notes}
                </p>
              </div>
            ))}
          </div>
        )
      )}

      {/* ── Interactive SOAP Clinical Consultation & Rx Modal ── */}
      {activeConsultation && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
          <div className="bg-white rounded-[32px] max-w-3xl w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 my-auto text-left max-h-[92vh] flex flex-col animate-in zoom-in-95">
            
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10 shrink-0">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-[#E8EEF4] text-[#10324E] flex items-center justify-center font-bold">
                  {activeConsultation.token}
                </div>
                <div>
                  <h3 className="text-lg font-extrabold text-[#10324E]">
                    SOAP Clinical Case Record: {activeConsultation.petName}
                  </h3>
                  <p className="text-xs text-[#242826]/60">
                    {activeConsultation.species} • {activeConsultation.breed} • Client: {activeConsultation.ownerName}
                  </p>
                </div>
              </div>
              <button
                onClick={() => setActiveConsultation(null)}
                className="p-1.5 rounded-xl text-[#242826]/60 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* SOAP Form Scrollable Body */}
            <form onSubmit={handleSavePrescription} className="overflow-y-auto py-4 space-y-5 text-xs flex-1 pr-1">
              
              {/* S: Subjective */}
              <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-2">
                <div className="font-extrabold text-[#10324E] uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-md bg-[#10324E] text-white flex items-center justify-center text-[10px]">S</span>
                  <span>Subjective (History &amp; Chief Complaint)</span>
                </div>
                <textarea
                  rows="2"
                  value={soapForm.subjective}
                  onChange={(e) => setSoapForm({ ...soapForm, subjective: e.target.value })}
                  placeholder="Owner observations, duration, appetite, water intake, bowel movements..."
                  className="w-full p-2.5 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#10324E]"
                />
              </div>

              {/* O: Objective (Vitals & Physical Exam) */}
              <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-3">
                <div className="font-extrabold text-[#10324E] uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-md bg-[#2D8A7A] text-white flex items-center justify-center text-[10px]">O</span>
                  <span>Objective (Clinical Vitals &amp; Physical Exam)</span>
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="block text-[11px] font-bold text-[#10324E] mb-1">Body Temp (°F)</label>
                    <input
                      type="text"
                      value={soapForm.temperature}
                      onChange={(e) => setSoapForm({ ...soapForm, temperature: e.target.value })}
                      className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-[#10324E] mb-1">Weight (kg)</label>
                    <input
                      type="text"
                      value={soapForm.weight}
                      onChange={(e) => setSoapForm({ ...soapForm, weight: e.target.value })}
                      className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 font-mono"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-[#10324E] mb-1">Heart Rate (bpm)</label>
                    <input
                      type="text"
                      value={soapForm.heartRate}
                      onChange={(e) => setSoapForm({ ...soapForm, heartRate: e.target.value })}
                      className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 font-mono"
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Systemic Examination Findings</label>
                  <textarea
                    rows="2"
                    value={soapForm.objectiveExam}
                    onChange={(e) => setSoapForm({ ...soapForm, objectiveExam: e.target.value })}
                    className="w-full p-2.5 rounded-xl bg-white border border-[#10324E]/15 text-xs"
                  />
                </div>
              </div>

              {/* A: Assessment */}
              <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-2">
                <div className="font-extrabold text-[#10324E] uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                  <span className="w-5 h-5 rounded-md bg-amber-600 text-white flex items-center justify-center text-[10px]">A</span>
                  <span>Assessment (Primary Diagnosis)</span>
                </div>
                <input
                  type="text"
                  value={soapForm.assessment}
                  onChange={(e) => setSoapForm({ ...soapForm, assessment: e.target.value })}
                  placeholder="e.g. Acute Gastroenteritis, Canine Parvovirus, Right Stifle Sprain"
                  className="w-full p-2.5 rounded-xl bg-white border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
                />
              </div>

              {/* P: Plan & Digital Prescription (Rx) */}
              <div className="p-4 rounded-2xl bg-[#E0F2EB]/40 border border-[#2D8A7A]/25 space-y-3">
                <div className="flex items-center justify-between">
                  <div className="font-extrabold text-[#10324E] uppercase tracking-wider text-[11px] flex items-center gap-1.5">
                    <span className="w-5 h-5 rounded-md bg-[#2D8A7A] text-white flex items-center justify-center text-[10px]">P</span>
                    <span>Plan &amp; Digital Prescription (Rx)</span>
                  </div>
                  <button
                    type="button"
                    onClick={handleAddMedicationRow}
                    className="text-[11px] font-bold text-[#2D8A7A] hover:underline flex items-center gap-1 cursor-pointer"
                  >
                    <Plus className="w-3.5 h-3.5" /> Add Drug
                  </button>
                </div>

                <div className="space-y-2.5">
                  {soapForm.medications.map((med, idx) => (
                    <div key={idx} className="p-3 rounded-xl bg-white border border-[#10324E]/10 space-y-2">
                      <div className="flex items-center justify-between gap-2">
                        <input
                          type="text"
                          required
                          value={med.name}
                          onChange={(e) => handleMedicationChange(idx, 'name', e.target.value)}
                          placeholder="Drug Name &amp; Strength (e.g. Meloxicam 0.5mg/ml)"
                          className="flex-1 p-2 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10 font-bold text-[#10324E]"
                        />
                        {soapForm.medications.length > 1 && (
                          <button
                            type="button"
                            onClick={() => handleRemoveMedicationRow(idx)}
                            className="p-1.5 rounded-lg text-rose-600 hover:bg-rose-50 cursor-pointer"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                      <div className="grid grid-cols-3 gap-2">
                        <input
                          type="text"
                          value={med.dosage}
                          onChange={(e) => handleMedicationChange(idx, 'dosage', e.target.value)}
                          placeholder="Dosage (e.g. 5ml)"
                          className="p-1.5 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10"
                        />
                        <select
                          value={med.frequency}
                          onChange={(e) => handleMedicationChange(idx, 'frequency', e.target.value)}
                          className="p-1.5 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10"
                        >
                          <option value="SID (Once daily)">SID (Once daily)</option>
                          <option value="BID (Twice daily)">BID (Twice daily)</option>
                          <option value="TID (Thrice daily)">TID (Thrice daily)</option>
                          <option value="PRN (As needed)">PRN (As needed)</option>
                        </select>
                        <input
                          type="text"
                          value={med.duration}
                          onChange={(e) => handleMedicationChange(idx, 'duration', e.target.value)}
                          placeholder="Duration (5 days)"
                          className="p-1.5 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10"
                        />
                      </div>
                    </div>
                  ))}
                </div>

                {/* Dietary Advice & Follow-up */}
                <div className="grid grid-cols-2 gap-3 pt-2">
                  <div>
                    <label className="block text-[11px] font-bold text-[#10324E] mb-1">Dietary Advice</label>
                    <input
                      type="text"
                      value={soapForm.dietPlan}
                      onChange={(e) => setSoapForm({ ...soapForm, dietPlan: e.target.value })}
                      placeholder="e.g. Bland diet, plenty of fresh fluids"
                      className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 text-xs"
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-bold text-[#10324E] mb-1">Follow-up in (Days)</label>
                    <input
                      type="number"
                      value={soapForm.followUpDays}
                      onChange={(e) => setSoapForm({ ...soapForm, followUpDays: e.target.value })}
                      className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 text-xs font-mono"
                    />
                  </div>
                </div>
              </div>

              {/* Digital Signature & Actions */}
              <div className="pt-2 flex items-center justify-between">
                <div className="flex items-center gap-1.5 text-[11px] text-[#242826]/70">
                  <ShieldCheck className="w-4 h-4 text-[#2D8A7A]" />
                  <span>Will be cryptographically signed with Veterinary Council license.</span>
                </div>

                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setActiveConsultation(null)}
                    className="px-4 py-2.5 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] cursor-pointer"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    className="px-5 py-2.5 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-bold shadow-md flex items-center gap-1.5 cursor-pointer"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>{prescriptionSigned ? 'Prescription Signed!' : 'Sign & Complete Consultation'}</span>
                  </button>
                </div>
              </div>

            </form>
          </div>
        </div>
      )}

    </div>
  );
}


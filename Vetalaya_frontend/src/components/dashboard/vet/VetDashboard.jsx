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
  Search,
  Activity,
  ShieldCheck,
  RefreshCw,
  SlidersHorizontal,
  QrCode,
  ShieldAlert,
  BarChart3,
  Calendar,
  Building2,
  Plus
} from 'lucide-react';

// Specialized Clinical Platform Subcomponents
import LiveQueueSection from './LiveQueueSection';
import SoapConsultationModal from './SoapConsultationModal';
import PrescriptionBuilder from './PrescriptionBuilder';
import VaccinationPassSection from './VaccinationPassSection';
import EmergencyQrScannerSection from './EmergencyQrScannerSection';
import AnalyticsSection from './AnalyticsSection';
import ClinicSettingsModal from './ClinicSettingsModal';

export default function VetDashboard() {
  const { currentUser } = useAuth();

  // Active navigation tab
  // 'queue' | 'rx-builder' | 'vaccines' | 'emergency-qr' | 'analytics'
  const [activeTab, setActiveTab] = useState('queue');

  // Modals state
  const [activeConsultationPet, setActiveConsultationPet] = useState(null);
  const [activePrescriptionPet, setActivePrescriptionPet] = useState(null);
  const [showSettingsModal, setShowSettingsModal] = useState(false);

  // Doctor & Clinic Profile State
  const [vetProfile, setVetProfile] = useState(null);
  const [hospitalName, setHospitalName] = useState('Vetalya Advanced Veterinary Hospital');
  const [clinicConfig, setClinicConfig] = useState({
    doctorName: currentUser?.name ? (currentUser.name.startsWith('Dr.') ? currentUser.name : `Dr. ${currentUser.name}`) : 'Dr. Vivek Deshmukh',
    vciNumber: currentUser?.councilRegNumber || 'VCI/MH/2016/0491',
    stateCouncil: 'Maharashtra State Veterinary Council',
    degrees: 'BVSc & AH, MVSc (Surgery)',
    clinicName: 'Vetalya Advanced Veterinary Hospital & Critical Care',
    city: 'Pune, Maharashtra',
    firstVisitFee: 600,
    followUpFee: 350,
  });

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
        if (profileRes.value.name || profileRes.value.registrationNo) {
          setClinicConfig(prev => ({
            ...prev,
            doctorName: profileRes.value.name ? (profileRes.value.name.startsWith('Dr.') ? profileRes.value.name : `Dr. ${profileRes.value.name}`) : prev.doctorName,
            vciNumber: profileRes.value.registrationNo || prev.vciNumber
          }));
        }
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
        const mapped = aptsRes.content.map(apt => {
          let parsedNotes = null;
          if (apt.visitNotes) {
            try {
              parsedNotes = typeof apt.visitNotes === 'string' ? JSON.parse(apt.visitNotes) : apt.visitNotes;
            } catch (e) {
              // Plain text
            }
          }
          return {
            id: apt.appointmentId,
            token: `T-${apt.appointmentId ? apt.appointmentId.slice(-2) : '01'}`,
            petName: apt.pet?.name || 'Patient Companion',
            petId: apt.pet?.petId || (apt.pet?.name ? `PET-${apt.pet.name.toUpperCase().slice(0, 3)}` : 'PET-01M71N6A7E'),
            species: apt.pet?.species === 'DOG' ? 'Canine' : apt.pet?.species === 'CAT' ? 'Feline' : (apt.pet?.species || 'Canine'),
            breed: apt.pet?.breed || 'Crossbreed',
            age: '2 yrs',
            weight: parsedNotes?.weight ? `${parsedNotes.weight} kg` : '18 kg',
            ownerName: apt.owner ? `${apt.owner.firstName || ''} ${apt.owner.lastName || ''}`.trim() || 'Pet Guardian' : 'Pet Guardian',
            ownerPhone: apt.owner?.phone || '+91 9921128469',
            time: apt.appointmentAt ? new Date(apt.appointmentAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : 'Scheduled',
            reason: apt.reason || 'Clinical OPD Assessment',
            status: apt.status === 'COMPLETED' ? 'Completed' : (apt.status === 'BOOKED' ? 'Waiting' : apt.status),
            statusColor: apt.status === 'COMPLETED' ? 'bg-slate-100 text-slate-700 border-slate-300' : 'bg-amber-100 text-amber-800 border-amber-300',
            triageLevel: 'Standard',
            temp: parsedNotes?.temperature || '101.4',
            hr: parsedNotes?.heartRate || '110',
            visitNotes: apt.visitNotes,
          };
        });
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

  // Handle consultation completion
  const handleSaveSoapRecord = async (soapData) => {
    if (activeConsultationPet?.id) {
      try {
        await updateVetAppointmentStatusApi(activeConsultationPet.id, 'COMPLETED', soapData.assessment);
        await updateVetVisitNotesApi(activeConsultationPet.id, JSON.stringify(soapData));
      } catch (err) {
        console.warn('Failed to update appointment in backend:', err.message);
      }
    }
    // Update local queue status
    setQueue(prev => prev.map(p => {
      if (p.id === activeConsultationPet?.id) {
        return {
          ...p,
          status: 'Completed',
          statusColor: 'bg-slate-100 text-slate-700 border-slate-300',
          visitNotes: soapData
        };
      }
      return p;
    }));
    await loadVetData();
  };

  // Switch to Rx builder from completed consultation
  const handleOpenRxFromSoap = (soapData, pet) => {
    setActiveConsultationPet(null);
    setActivePrescriptionPet({
      ...pet,
      diagnosis: soapData.assessment,
      vitals: {
        weight: soapData.weight,
        temp: soapData.temperature,
        hr: soapData.heartRate
      },
      prefilledMeds: soapData.medications
    });
    setActiveTab('rx-builder');
  };

  return (
    <div className="space-y-6 animate-in fade-in text-left">
      
      {/* ── Top Clinical Desk Banner & Statutory Header ── */}
      <div className="bg-gradient-to-r from-gray-900 via-slate-900 to-emerald-950 rounded-3xl p-6 sm:p-8 text-white shadow-xl border border-gray-800">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div>
            <div className="flex items-center gap-2 flex-wrap mb-2">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 text-xs font-bold border border-emerald-500/30">
                <Stethoscope className="w-3.5 h-3.5" />
                <span>Veterinary Clinical Desk</span>
              </span>
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-white/10 text-gray-200 text-xs font-mono border border-white/10">
                <ShieldCheck className="w-3.5 h-3.5 text-emerald-400" />
                <span>VCI Reg: {clinicConfig.vciNumber}</span>
              </span>
              <span className="text-xs text-gray-300">
                {clinicConfig.stateCouncil}
              </span>
            </div>

            <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white">
              {clinicConfig.doctorName}
            </h1>
            <p className="text-sm text-gray-300 mt-1 flex items-center gap-2 flex-wrap">
              <span className="font-semibold">{hospitalName || clinicConfig.clinicName}</span>
              <span>•</span>
              <span className="text-gray-400">{clinicConfig.city}</span>
              <span>•</span>
              <span className="text-emerald-400 font-mono text-xs">OPD Slot: 15 mins</span>
            </p>
          </div>

          {/* Quick Action Ribbon */}
          <div className="flex items-center gap-2.5 flex-wrap">
            <button
              onClick={() => setShowSettingsModal(true)}
              className="px-4 py-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white font-bold text-xs flex items-center gap-2 border border-white/20 transition cursor-pointer"
            >
              <SlidersHorizontal className="w-4 h-4 text-emerald-400" />
              <span>Practice &amp; VCI Settings</span>
            </button>

            <button
              onClick={loadVetData}
              title="Refresh queue and backend data"
              className="p-2.5 rounded-xl bg-white/10 hover:bg-white/20 text-white transition cursor-pointer border border-white/20"
            >
              <RefreshCw className={`w-4 h-4 ${loadingQueue ? 'animate-spin text-emerald-400' : ''}`} />
            </button>

            <span className="inline-flex items-center gap-1.5 px-3 py-2 rounded-xl bg-emerald-500/20 text-emerald-300 text-xs font-bold border border-emerald-400/30">
              <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
              <span>OPD Active</span>
            </span>
          </div>
        </div>
      </div>

      {/* ── Main Clinical Platform Navigation Tabs ── */}
      <div className="bg-white rounded-2xl p-2 border border-gray-200 shadow-sm flex items-center gap-1.5 overflow-x-auto">
        <button
          onClick={() => setActiveTab('queue')}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'queue'
              ? 'bg-emerald-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Live OPD Queue ({queue.length})</span>
        </button>

        <button
          onClick={() => setActiveTab('rx-builder')}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'rx-builder'
              ? 'bg-emerald-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
          }`}
        >
          <FileText className="w-4 h-4" />
          <span>Digital Prescription (Rx)</span>
        </button>

        <button
          onClick={() => setActiveTab('vaccines')}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'vaccines'
              ? 'bg-emerald-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
          }`}
        >
          <ShieldAlert className="w-4 h-4" />
          <span>Vaccination &amp; Rabies Pass</span>
        </button>

        <button
          onClick={() => setActiveTab('emergency-qr')}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'emergency-qr'
              ? 'bg-emerald-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
          }`}
        >
          <QrCode className="w-4 h-4" />
          <span>Emergency QR &amp; Record Audit</span>
        </button>

        <button
          onClick={() => setActiveTab('analytics')}
          className={`px-4 py-2.5 rounded-xl text-xs font-bold transition flex items-center gap-2 cursor-pointer whitespace-nowrap ${
            activeTab === 'analytics'
              ? 'bg-emerald-600 text-white shadow-sm'
              : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
          }`}
        >
          <BarChart3 className="w-4 h-4" />
          <span>Practice Analytics &amp; Reminders</span>
        </button>
      </div>

      {/* ── Active Tab Display ── */}

      {/* 1. OPD Queue Section */}
      {activeTab === 'queue' && (
        <div className="space-y-6">
          <LiveQueueSection 
            queue={queue}
            backendQueue={queue} 
            loading={loadingQueue}
            loadingQueue={loadingQueue}
            onStartConsultation={(patient) => setActiveConsultationPet(patient)}
            onUpdateStatus={(id, newStatus) => {
              setQueue(prev => prev.map(p => p.id === id ? { ...p, status: newStatus } : p));
            }}
            onAddWalkInPatient={(newPatient) => {
              setQueue(prev => [newPatient, ...prev]);
            }}
          />
        </div>
      )}

      {/* 2. Digital Prescription (Rx) Builder */}
      {activeTab === 'rx-builder' && (
        <div className="space-y-6">
          <PrescriptionBuilder 
            patient={activePrescriptionPet || (queue.length > 0 ? queue[0] : null)}
            doctorProfile={clinicConfig}
            embedded={true}
            onClose={() => setActiveTab('queue')}
          />
        </div>
      )}

      {/* 3. Lifelong Vaccination & Rabies Certificate Pass */}
      {activeTab === 'vaccines' && (
        <div className="space-y-6">
          <VaccinationPassSection 
            doctorProfile={clinicConfig}
            patient={activeConsultationPet || (queue.length > 0 ? queue[0] : null)}
          />
        </div>
      )}

      {/* 4. Emergency QR Scanner & Audit Trail */}
      {activeTab === 'emergency-qr' && (
        <div className="space-y-6">
          <EmergencyQrScannerSection 
            doctorProfile={clinicConfig}
            onSelectPetForConsultation={(pet) => {
              setActiveConsultationPet(pet);
              setActiveTab('queue');
            }}
          />
        </div>
      )}

      {/* 5. Practice Analytics & Reminders */}
      {activeTab === 'analytics' && (
        <div className="space-y-6">
          <AnalyticsSection />
        </div>
      )}

      {/* ── SOAP Clinical Consultation Modal ── */}
      {activeConsultationPet && (
        <SoapConsultationModal
          patient={activeConsultationPet}
          onClose={() => setActiveConsultationPet(null)}
          onSave={handleSaveSoapRecord}
          onOpenRxBuilder={(soapData) => handleOpenRxFromSoap(soapData, activeConsultationPet)}
        />
      )}

      {/* ── Practice & VCI Settings Modal ── */}
      {showSettingsModal && (
        <ClinicSettingsModal
          initialData={clinicConfig}
          onClose={() => setShowSettingsModal(false)}
          onSave={(updatedConfig) => {
            setClinicConfig(updatedConfig);
          }}
        />
      )}

    </div>
  );
}

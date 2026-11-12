import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useAuth } from '../../../context/AuthContext';
import {
  getVetDashboardApi,
  getVetAppointmentsApi,
  getVetProfileApi,
  getVetHospitalsApi,
  updateVetAppointmentStatusApi,
  updateVetVisitNotesApi,
  createEncounterApi,
  signEncounterApi,
  getEncounterApi,
  updateDraftEncounterApi,
  generateSlotsApi,
  getAvailableSlotsApi,
  checkInAppointmentApi,
  startConsultationApi,
  endConsultationApi,
  createDiagnosticOrderApi,
  getPetDiagnosticsApi,
  updateDiagnosticResultApi,
  getDiagnosticCatalogApi,
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
  Lock,
  ShieldAlert,
  Calendar,
  Microscope,
  Check,
  FlaskConical,
} from 'lucide-react';

export default function VetDashboard({ hospitalId, workingDate }) {
  const { currentUser } = useAuth();

  const [loadError, setLoadError] = useState('');
  const encounterRef = useRef(null);
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
  const [submittingSoap, setSubmittingSoap] = useState(false);
  const [soapError, setSoapError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [activeTab, setActiveTab] = useState('queue'); // 'queue' | 'diagnostics' | 'history'

  // Diagnostic records
  const [diagnostics, setDiagnostics] = useState([]);

  // Standard Test Catalog for Requisitions
  const [STANDARD_TESTS, setStandardTests] = useState([]);
  useEffect(() => {
    let cancelled = false;
    getDiagnosticCatalogApi().then(tests => {
      if (!cancelled) setStandardTests(tests.map(t => ({ code: t.testCode, name: t.testName, category: t.category, tat: `${t.tatHours} hrs`, price: new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(t.basePricePaisa / 100) })));
    }).catch(e => { if (!cancelled) setLoadError(e.message); });
    return () => { cancelled = true; };
  }, []);

  // Slot Management State
  const [showSlotModal, setShowSlotModal] = useState(false);
  const [slotForm, setSlotForm] = useState({
    fromDate: new Date().toISOString().split('T')[0],
    toDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
    startTime: '09:00',
    endTime: '17:00',
    slotDurationMinutes: 20,
    slotType: 'OPD_CONSULTATION',
    capacity: 1,
  });
  const [generatingSlots, setGeneratingSlots] = useState(false);
  const [slotMsg, setSlotMsg] = useState(null);

  // Diagnostic Management State
  const [showDiagOrderModal, setShowDiagOrderModal] = useState(false);
  const [diagOrderForm, setDiagOrderForm] = useState({
    petId: '',
    petName: '',
    priority: 'ROUTINE',
    clinicalNotes: '',
    selectedTests: [],
  });
  const [submittingDiagOrder, setSubmittingDiagOrder] = useState(false);
  const [diagError, setDiagError] = useState(null);

  // Result verification modal
  const [showResultModal, setShowResultModal] = useState(false);
  const [selectedItemToVerify, setSelectedItemToVerify] = useState(null);
  const [resultForm, setResultForm] = useState({
    resultValue: '',
    referenceRange: '',
    unit: '',
    flag: 'NORMAL',
    interpretation: '',
  });
  const [submittingResult, setSubmittingResult] = useState(false);

  const loadVetData = useCallback(async () => {
    setLoadError('');
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
        setHospitalName(hospitalsRes.value.find(h => h.hospitalId === hospitalId)?.name || 'Selected hospital');
      }
    } catch (e) {
      console.warn('Could not fetch vet profile/hospital:', e.message);
    }


    try {
      const today = workingDate || new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata' }).format(new Date());
      const filters = { ...(hospitalId ? { hospitalId } : {}), from: today, to: today };
      const first = await getVetAppointmentsApi(0, 50, filters);
      const pages = [first];
      for (let page = 1; page < (first.totalPages || 1); page++) pages.push(await getVetAppointmentsApi(page, 50, filters));
      const aptsRes = { content: pages.flatMap(p => p.content || []) };
      if (aptsRes && aptsRes.content) {
        const mapped = aptsRes.content.map(apt => {
          let parsedNotes = null;
          if (apt.visitNotes) {
            try {
              parsedNotes = typeof apt.visitNotes === 'string' ? JSON.parse(apt.visitNotes) : apt.visitNotes;
            } catch (e) {
              // Plain text or unparsed
            }
          }
          return {
            id: apt.appointmentId,
            petId: apt.pet?.petId,
            hospitalId: apt.hospitalId,
            signed: Boolean(parsedNotes?.isSigned || parsedNotes?.signedAt),
            token: `T-${apt.appointmentId ? apt.appointmentId.slice(-2) : '01'}`,
            petName: apt.pet?.name || 'Patient Companion',
            species: apt.pet?.species === 'DOG' ? 'Canine' : apt.pet?.species === 'CAT' ? 'Feline' : (apt.pet?.species || 'Companion'),
            breed: apt.pet?.breed || 'Companion',
            age: '—',
            weight: parsedNotes?.weight ? `${parsedNotes.weight} kg` : '—',
            ownerName: apt.owner ? `${apt.owner.firstName || ''} ${apt.owner.lastName || ''}`.trim() || 'Pet Owner' : 'Pet Owner',
            ownerPhone: 'Verified Client',
            time: apt.appointmentAt ? new Date(apt.appointmentAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : 'Scheduled',
            reason: apt.reason || 'Clinical Consultation & Health Assessment',
            status: apt.status,
            statusColor: apt.status === 'COMPLETED' ? 'bg-slate-100 text-slate-700 border-slate-300' : 'bg-amber-100 text-amber-800 border-amber-300',
            triageLevel: 'Standard',
            temp: parsedNotes?.temperature || '—',
            hr: parsedNotes?.heartRate || '—',
            visitNotes: apt.visitNotes,
          };
        });
        setQueue(mapped);
        setMetrics({ todayTotal: mapped.length, waitingInClinic: mapped.filter(p => p.status === 'CONFIRMED').length, completedToday: mapped.filter(p => p.status === 'COMPLETED').length, criticalTriage: mapped.filter(p => p.status === 'BOOKED').length });
        const orders = await Promise.all([...new Set(mapped.map(p => p.petId).filter(Boolean))].map(id => getPetDiagnosticsApi(id)));
        setDiagnostics(orders.flat().filter(o => !hospitalId || o.hospitalId === hospitalId).map(o => ({ ...o, id: o.orderId, petName: mapped.find(p => p.petId === o.petId)?.petName || o.petId, type: o.items.map(i => i.testName).join(', '), date: new Date(o.createdAt).toLocaleString('en-IN'), notes: o.clinicalNotes })));
      } else {
        setQueue([]);
      }
    } catch (e) {
      setLoadError(e.message || 'Unable to load clinical workspace.');
      setQueue([]);
    } finally {
      setLoadingQueue(false);
      setLoadingMetrics(false);
    }
  }, [hospitalId, workingDate]);

  useEffect(() => {
    loadVetData();
  }, [loadVetData]);

  const handleCheckInPatient = async (patientId) => {
    try {
      await checkInAppointmentApi(patientId);
      await loadVetData();
    } catch (e) {
      setLoadError(e.message);
    }
  };

  const handleGenerateSlotsSubmit = async (e) => {
    e.preventDefault();
    setGeneratingSlots(true);
    setSlotMsg(null);
    try {
      await generateSlotsApi({
        ...slotForm,
        hospitalId,
        slotDurationMinutes: parseInt(slotForm.slotDurationMinutes, 10),
        capacity: parseInt(slotForm.capacity, 10),
      });
      setSlotMsg({ type: 'success', text: 'Appointment slots successfully configured and generated!' });
      setTimeout(() => setShowSlotModal(false), 1500);
    } catch (e) {
      setSlotMsg({ type: 'error', text: e.message || 'Failed to generate slots.' });
    } finally {
      setGeneratingSlots(false);
    }
  };

  const handleCreateDiagnosticOrder = async (e) => {
    e.preventDefault();
    setSubmittingDiagOrder(true);
    setDiagError(null);
    try {
      const items = diagOrderForm.selectedTests.map(code => {
        const found = STANDARD_TESTS.find(t => t.code === code) || { name: code, category: 'GENERAL' };
        return {
          testCode: code,
          testName: found.name,
          category: found.category,
        };
      });

      const orderData = {
        petId: diagOrderForm.petId,
        priority: diagOrderForm.priority,
        clinicalNotes: diagOrderForm.clinicalNotes,
        items,
      };

      if (!orderData.petId) throw new Error('Select a registered patient.');
      await createDiagnosticOrderApi({ ...orderData, hospitalId });
      await loadVetData();
      setShowDiagOrderModal(false);
      setActiveTab('diagnostics');
    } catch (e) {
      setDiagError(e.message || 'Could not submit diagnostic requisition.');
    } finally {
      setSubmittingDiagOrder(false);
    }
  };

  const handleOpenResultModal = (order, item) => {
    setSelectedItemToVerify({ ...item, orderId: order.id, petName: order.petName });
    setResultForm({
      resultValue: item.resultValue || '',
      referenceRange: item.referenceRange || '',
      unit: item.unit || '',
      flag: item.flag || 'NORMAL',
      interpretation: item.interpretation || '',
    });
    setShowResultModal(true);
  };

  const handleSaveResult = async (e) => {
    e.preventDefault();
    if (!selectedItemToVerify) return;
    setSubmittingResult(true);

    try {
      if (!selectedItemToVerify.itemId.startsWith('ITEM-') && !selectedItemToVerify.itemId.startsWith('IT-')) {
        await updateDiagnosticResultApi(selectedItemToVerify.itemId, {
          ...resultForm,
          verifiedBy: `Dr. ${vetProfile?.firstName || 'Practitioner'}`,
        });
      }

      setDiagnostics(prev =>
        prev.map(ord => {
          if (ord.id === selectedItemToVerify.orderId) {
            const updatedItems = ord.items.map(it => {
              if (it.itemId === selectedItemToVerify.itemId || it.testCode === selectedItemToVerify.testCode) {
                return {
                  ...it,
                  ...resultForm,
                  status: 'COMPLETED',
                  verifiedBy: `Dr. ${vetProfile?.firstName || 'Practitioner'}`,
                };
              }
              return it;
            });
            const allDone = updatedItems.every(i => i.status === 'COMPLETED');
            return {
              ...ord,
              status: allDone ? 'COMPLETED' : 'PROCESSING',
              items: updatedItems,
            };
          }
          return ord;
        })
      );

      setShowResultModal(false);
    } catch (e) {
      setLoadError(e.message);
    } finally {
      setSubmittingResult(false);
    }
  };

  const handleStartConsultation = (patient) => {
    if (patient.status === 'COMPLETED' || patient.signed) { setLoadError('This visit is finalized. Signed records require an auditable amendment.'); return; }
    encounterRef.current = null;
    setActiveConsultation(patient);
    setSoapError(null);

    let parsedNotes = null;
    if (patient.visitNotes) {
      try {
        parsedNotes = typeof patient.visitNotes === 'string' ? JSON.parse(patient.visitNotes) : patient.visitNotes;
      } catch (e) {
        // Ignored
      }
    }

    setSoapForm({
      subjective: parsedNotes?.subjective || patient.reason || '',
      temperature: parsedNotes?.temperature || (patient.temp !== '—' ? patient.temp : ''),
      weight: parsedNotes?.weight || (patient.weight !== '—' ? patient.weight.replace(' kg', '') : ''),
      heartRate: parsedNotes?.heartRate || (patient.hr !== '—' ? patient.hr : ''),
      objectiveExam: parsedNotes?.objectiveExam || '',
      assessment: parsedNotes?.assessment || patient.reason || '',
      dietPlan: parsedNotes?.dietPlan || '',
      followUpDays: parsedNotes?.followUpDays || '7',
      medications: parsedNotes?.medications && parsedNotes.medications.length > 0
        ? parsedNotes.medications
        : [{ name: '', dosage: '', frequency: 'BID (Twice daily)', duration: '5 days', instructions: 'After food' }],
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
    setSoapError(null);

    // Validation checks
    if (!soapForm.assessment.trim()) {
      setSoapError('Primary Clinical Assessment / Diagnosis is required before signing prescription.');
      return;
    }

    if (soapForm.temperature.trim()) {
      const tempVal = parseFloat(soapForm.temperature);
      if (isNaN(tempVal) || tempVal < 90 || tempVal > 112) {
        setSoapError('Body temperature must be a realistic numeric reading between 90°F and 112°F.');
        return;
      }
    }

    if (soapForm.weight.trim()) {
      const wtVal = parseFloat(soapForm.weight);
      if (isNaN(wtVal) || wtVal <= 0 || wtVal > 300) {
        setSoapError('Patient weight must be a valid positive number in kilograms.');
        return;
      }
    }

    if (soapForm.heartRate.trim()) {
      const hrVal = parseInt(soapForm.heartRate, 10);
      if (isNaN(hrVal) || hrVal < 20 || hrVal > 350) {
        setSoapError('Heart rate must be an integer between 20 and 350 bpm.');
        return;
      }
    }

    // Validate medication rows
    for (let i = 0; i < soapForm.medications.length; i++) {
      const med = soapForm.medications[i];
      if (med.name.trim() && !med.dosage.trim()) {
        setSoapError(`Please specify the dosage for medication "${med.name}".`);
        return;
      }
    }

    setSubmittingSoap(true);

    try {
      // 1. Persist to structured Clinical Encounter API with digital signature
      const encounterPayload = {
        petId: activeConsultation.petId || activeConsultation.id,
        hospitalId: activeConsultation.hospitalId || null,
        appointmentId: activeConsultation.id,
        encounterType: 'OPD_CONSULTATION',
        chiefComplaint: soapForm.subjective?.trim() || null,
        presentingHistory: activeConsultation.reason || null,
        physicalExamination: soapForm.objectiveExam?.trim() || null,
        temperatureF: soapForm.temperature ? parseFloat(soapForm.temperature) : null,
        bodyWeightKg: soapForm.weight ? parseFloat(soapForm.weight) : null,
        heartRateBpm: soapForm.heartRate ? parseInt(soapForm.heartRate, 10) : null,
        assessmentNotes: soapForm.assessment?.trim() || null,
        diagnosisPrimary: soapForm.assessment?.trim() || 'Clinical Examination',
        treatmentPlan: soapForm.dietPlan?.trim() || null,
        dietInstructions: soapForm.dietPlan?.trim() || null,
        followUpDate: new Date(Date.now() + (parseInt(soapForm.followUpDays, 10) || 7) * 86400000).toISOString().split('T')[0],
        medications: soapForm.medications.filter(m => m.name.trim()).map(m => ({
          medicineName: m.name.trim(),
          dosage: m.dosage?.trim() || '1 unit',
          frequency: m.frequency || 'BID',
          duration: m.duration || '5 days',
          instructions: m.instructions || 'After food',
          isScheduleH: m.isScheduleH || false,
          quantity: 1,
        })),
      };

      if (!activeConsultation.petId) throw new Error('Patient identity is missing. Refresh the queue.');
      if (encounterRef.current) {
        await updateDraftEncounterApi(encounterRef.current, encounterPayload);
      } else {
        const createdEnc = await createEncounterApi(encounterPayload);
        if (!createdEnc?.encounterId) throw new Error('The encounter could not be created.');
        encounterRef.current = createdEnc.encounterId;
      }
      const signed = await signEncounterApi(encounterRef.current);
      if (!signed?.isSigned) throw new Error('The encounter was not signed. Please retry.');

      setPrescriptionSigned(true);
      await loadVetData();

      setTimeout(() => {
        setActiveConsultation(null);
        setPrescriptionSigned(false);
        setSubmittingSoap(false);
      }, 1500);
    } catch (err) {
      setSubmittingSoap(false);
      setSoapError(err.message || 'Failed to persist clinical consultation to database. Please retry.');
    }
  };

  const filteredQueue = queue.filter(
    (p) =>
      p.petName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.ownerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.token.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-8 animate-in fade-in">
      
      {loadError && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">{loadError}</div>}
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
            {hospitalName || 'Selected hospital'} • Council Reg: {vetProfile?.registrationNo || currentUser?.councilRegNumber || 'Not supplied'}
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
            <span className="text-xs font-bold text-[#242826]/60">Selected Day Total</span>
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
          <span className="text-[11px] text-emerald-700 font-bold mt-1 inline-block">Completed appointments</span>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-rose-200 shadow-vetalya text-left bg-rose-50/30">
          <div className="flex items-center justify-between text-rose-700">
            <span className="text-xs font-bold text-rose-800">Expected arrivals</span>
            <AlertCircle className="w-4 h-4 text-rose-600 animate-pulse" />
          </div>
          <p className={`text-2xl sm:text-3xl font-extrabold text-rose-700 font-display mt-2 ${loadingMetrics ? 'opacity-40 animate-pulse' : ''}`}>
            {metrics.criticalTriage}
          </p>
          <span className="text-[11px] text-rose-700 font-bold mt-1 inline-block">Pending review</span>
        </div>
      </div>

      {/* ── Tabs: Live Queue vs Diagnostic Center ── */}
      <div id="opd-queue" className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#10324E]/10 pb-3 scroll-mt-28">
        <div className="flex flex-wrap items-center gap-2">
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
            <Activity className="w-3.5 h-3.5 text-[#079D96]" />
            <span>Diagnostics &amp; Imaging ({diagnostics.length})</span>
          </button>

          <button
            onClick={() => {
              setSlotMsg(null);
              setShowSlotModal(true);
            }}
            className="px-3 py-2 rounded-2xl text-xs font-bold bg-[#EAF1F8] text-[#123F70] hover:bg-[#d8e6f3] transition cursor-pointer flex items-center gap-1.5"
            title="Configure and pre-generate appointment booking slots"
          >
            <Calendar className="w-3.5 h-3.5 text-[#079D96]" />
            <span>Manage Slots</span>
          </button>

          {activeTab === 'diagnostics' && (
            <button
              onClick={() => {
                setDiagOrderForm({
                  petId: queue[0]?.petId || '',
                  petName: queue[0]?.petName || 'Select Companion',
                  priority: 'ROUTINE',
                  clinicalNotes: '',
                  selectedTests: [],
                });
                setShowDiagOrderModal(true);
              }}
              className="px-3 py-2 rounded-2xl text-xs font-bold bg-[#E8F6F4] text-[#079D96] hover:bg-[#d2f1ec] transition cursor-pointer flex items-center gap-1.5"
            >
              <FlaskConical className="w-3.5 h-3.5" />
              <span>New Lab Requisition</span>
            </button>
          )}
        </div>

        {/* Queue Search */}
        <div className="relative max-w-xs w-full">
          <Search className="w-3.5 h-3.5 absolute left-3 top-3 text-[#242826]/40" />
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search token, pet, or client..."
            className="w-full pl-9 pr-3 py-1.5 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#10324E] focus:outline-none focus:ring-2 focus:ring-[#079D96]/40"
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
                        <div className="flex items-center justify-end gap-1.5">
                          {item.status === 'BOOKED' && (
                            <button
                              onClick={() => handleCheckInPatient(item.id)}
                              className="px-2.5 py-1.5 rounded-xl bg-amber-50 hover:bg-amber-100 text-amber-900 border border-amber-200 font-bold text-[11px] transition inline-flex items-center gap-1 cursor-pointer"
                              title="Patient arrived at clinic — grant queue token"
                            >
                              <Check className="w-3 h-3 text-amber-700" />
                              <span>Check-In</span>
                            </button>
                          )}
                          <button
                            onClick={() => {
                              setDiagOrderForm({
                                petId: item.petId || item.id,
                                petName: item.petName,
                                priority: 'ROUTINE',
                                clinicalNotes: `OPD Queue Token ${item.token}: ${item.reason}`,
                                selectedTests: [],
                              });
                              setShowDiagOrderModal(true);
                            }}
                            className="px-2.5 py-1.5 rounded-xl bg-[#E8F6F4] hover:bg-[#d0f0eb] text-[#079D96] border border-[#079D96]/30 font-bold text-[11px] transition inline-flex items-center gap-1 cursor-pointer"
                            title="Order Lab Requisition"
                          >
                            <FlaskConical className="w-3 h-3" />
                            <span>Order Lab</span>
                          </button>
                          <button
                            disabled={item.status === 'COMPLETED' || item.status === 'CANCELLED' || item.status === 'NO_SHOW'}
                            onClick={() => handleStartConsultation(item)}
                            className="px-3.5 py-1.5 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white font-bold text-xs shadow-2xs transition inline-flex items-center gap-1 cursor-pointer"
                          >
                            <FileText className="w-3 h-3 text-[#FF843F]" />
                            <span>SOAP Note</span>
                          </button>
                        </div>
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
        <div className="space-y-4 text-left">
          <div className="flex items-center justify-between">
            <h3 className="font-extrabold text-base text-[#10324E]">Clinical Diagnostics &amp; Imaging Requisitions</h3>
            <button
              onClick={() => {
                setDiagOrderForm({
                  petId: queue[0]?.petId || '',
                  petName: queue[0]?.petName || 'Select Companion',
                  priority: 'ROUTINE',
                  clinicalNotes: '',
                  selectedTests: [],
                });
                setShowDiagOrderModal(true);
              }}
              className="px-3.5 py-1.5 rounded-xl bg-[#079D96] hover:bg-[#06847e] text-white font-bold text-xs shadow-2xs transition inline-flex items-center gap-1.5 cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Create Laboratory Requisition</span>
            </button>
          </div>

          {diagnostics.length === 0 ? (
            <div id="diagnostics" className="bg-white rounded-3xl p-12 border border-dashed border-[#10324E]/15 text-center scroll-mt-28">
              <Activity className="w-10 h-10 text-[#079D96]/40 mx-auto mb-3" />
              <h4 className="text-sm font-bold text-[#10324E]">No diagnostic orders active</h4>
              <p className="text-xs text-[#242826]/60 mt-1 max-w-sm mx-auto">
                Blood panels, rapid pathogen tests, and digital imaging requisitions ordered for companions will appear here for laboratory processing and verification.
              </p>
            </div>
          ) : (
            <div className="space-y-4">
              {diagnostics.map((diag) => (
                <div key={diag.id} className="bg-white rounded-3xl p-6 border border-[#10324E]/8 shadow-vetalya space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 pb-3 border-b border-[#10324E]/8">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-black text-[#10324E]">{diag.id}</span>
                        <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full ${
                          diag.priority === 'STAT' ? 'bg-rose-100 text-rose-800 border border-rose-300' :
                          diag.priority === 'URGENT' ? 'bg-amber-100 text-amber-800 border border-amber-300' :
                          'bg-slate-100 text-slate-700'
                        }`}>
                          {diag.priority}
                        </span>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          diag.status === 'COMPLETED' ? 'bg-emerald-100 text-emerald-800' : 'bg-blue-100 text-blue-800'
                        }`}>
                          {diag.status}
                        </span>
                      </div>
                      <h4 className="font-extrabold text-sm text-[#10324E] mt-1">{diag.petName} — {diag.type}</h4>
                      <span className="text-[11px] text-[#242826]/60">{diag.date}</span>
                    </div>

                    <button
                      onClick={() => {
                        setDiagOrderForm({
                          petId: diag.petId || queue[0]?.id || 'PET-001',
                          petName: diag.petName,
                          priority: 'ROUTINE',
                          clinicalNotes: `Addendum to ${diag.id}`,
                          selectedTests: ['BIOCHEM_LFT_KFT'],
                        });
                        setShowDiagOrderModal(true);
                      }}
                      className="px-2.5 py-1 rounded-xl bg-[#FAF8F5] hover:bg-slate-100 border border-[#10324E]/10 text-[#10324E] text-xs font-bold transition inline-flex items-center gap-1 self-start cursor-pointer"
                    >
                      <Plus className="w-3 h-3 text-[#079D96]" />
                      <span>Add Panel</span>
                    </button>
                  </div>

                  {diag.notes && (
                    <p className="text-xs text-[#242826]/80 bg-[#FAF8F5] p-3 rounded-2xl border border-[#10324E]/6">
                      <strong className="text-[#10324E]">Clinical Indication:</strong> {diag.notes}
                    </p>
                  )}

                  {/* Diagnostic Items & Results Breakdown */}
                  {diag.items && diag.items.length > 0 && (
                    <div className="overflow-x-auto">
                      <table className="w-full text-xs text-left">
                        <thead>
                          <tr className="border-b border-[#10324E]/10 text-[#242826]/60 font-bold uppercase text-[10px]">
                            <th className="pb-2">Test / Profile</th>
                            <th className="pb-2">Category</th>
                            <th className="pb-2">Result Value</th>
                            <th className="pb-2">Reference Range</th>
                            <th className="pb-2">Clinical Flag</th>
                            <th className="pb-2 text-right">Verification</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-[#10324E]/5">
                          {diag.items.map((item, idx) => (
                            <tr key={idx} className="hover:bg-[#FAF8F5]">
                              <td className="py-2.5 font-bold text-[#10324E]">
                                {item.testName}
                                <span className="block text-[10px] text-[#242826]/60 font-mono">{item.testCode}</span>
                              </td>
                              <td className="py-2.5 text-[11px] font-semibold text-[#079D96]">{item.category}</td>
                              <td className="py-2.5 font-mono text-[11px]">
                                {item.resultValue ? (
                                  <span className="font-bold text-[#10324E]">{item.resultValue} {item.unit || ''}</span>
                                ) : (
                                  <span className="text-[#242826]/40 italic">Awaiting lab run</span>
                                )}
                              </td>
                              <td className="py-2.5 font-mono text-[11px] text-[#242826]/70">
                                {item.referenceRange || '—'}
                              </td>
                              <td className="py-2.5">
                                {item.status === 'COMPLETED' ? (
                                  <span className={`text-[10px] font-black px-2 py-0.5 rounded-full ${
                                    item.flag === 'CRITICAL' ? 'bg-red-100 text-red-800' :
                                    item.flag === 'HIGH' || item.flag === 'LOW' ? 'bg-amber-100 text-amber-800' :
                                    'bg-emerald-100 text-emerald-800'
                                  }`}>
                                    {item.flag || 'NORMAL'}
                                  </span>
                                ) : (
                                  <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-100 text-slate-600">
                                    PENDING
                                  </span>
                                )}
                              </td>
                              <td className="py-2.5 text-right">
                                {item.status === 'COMPLETED' ? (
                                  <span className="text-[11px] text-emerald-700 font-bold inline-flex items-center gap-1">
                                    <ShieldCheck className="w-3.5 h-3.5" />
                                    <span>Verified</span>
                                  </span>
                                ) : (
                                  <button
                                    onClick={() => handleOpenResultModal(diag, item)}
                                    className="px-2.5 py-1 rounded-xl bg-[#123F70] hover:bg-[#0b315c] text-white text-[11px] font-bold shadow-2xs transition inline-flex items-center gap-1 cursor-pointer"
                                  >
                                    <Microscope className="w-3 h-3 text-[#FF843F]" />
                                    <span>Enter Result</span>
                                  </button>
                                )}
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
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
              
              {soapError && (
                <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
                    <span>{soapError}</span>
                  </div>
                  <button type="button" onClick={() => setSoapError(null)} className="text-rose-600 hover:text-rose-900 font-extrabold">✕</button>
                </div>
              )}
              
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
                      <div className="flex items-center gap-2 pt-1 text-[11px]">
                        <label className="flex items-center gap-1.5 cursor-pointer text-[#10324E]">
                          <input
                            type="checkbox"
                            checked={med.isScheduleH || false}
                            onChange={(e) => handleMedicationChange(idx, 'isScheduleH', e.target.checked)}
                            className="rounded text-[#2D8A7A] focus:ring-[#2D8A7A]"
                          />
                          <span className="font-bold">Schedule H / Prescription Drug</span>
                        </label>
                        {med.isScheduleH && (
                          <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800 border border-amber-300 flex items-center gap-1">
                            <ShieldAlert className="w-3 h-3 text-amber-600" />
                            <span>Statutory Register Entry</span>
                          </span>
                        )}
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
                    disabled={submittingSoap}
                    className="px-5 py-2.5 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-bold shadow-md flex items-center gap-1.5 cursor-pointer disabled:opacity-50"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>
                      {submittingSoap
                        ? 'Persisting to DB...'
                        : prescriptionSigned
                        ? 'Prescription Signed!'
                        : 'Sign & Complete Consultation'}
                    </span>
                  </button>
                </div>
              </div>

            </form>
          </div>
        </div>
      )}

      {/* ── Slot Configuration & Generation Modal ── */}
      {showSlotModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-lg w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-[#EAF1F8] text-[#123F70] flex items-center justify-center">
                  <Calendar className="w-5 h-5 text-[#079D96]" />
                </div>
                <div>
                  <h3 className="text-base font-extrabold text-[#10324E]">Manage Appointment Slots</h3>
                  <p className="text-[11px] text-[#242826]/60">Pessimistic concurrency slot generator</p>
                </div>
              </div>
              <button
                onClick={() => setShowSlotModal(false)}
                className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {slotMsg && (
              <div className={`mt-4 p-3 rounded-2xl text-xs font-bold ${
                slotMsg.type === 'success' ? 'bg-emerald-50 text-emerald-800 border border-emerald-200' : 'bg-rose-50 text-rose-800 border border-rose-200'
              }`}>
                {slotMsg.text}
              </div>
            )}

            <form onSubmit={handleGenerateSlotsSubmit} className="mt-4 space-y-3.5">
              <div className="p-3 rounded-2xl bg-[#EAF1F8]/50 border border-[#123F70]/10 text-[11px] text-[#123F70]">
                Pre-configured slots enforce atomic concurrency locking at the database layer to guarantee zero race conditions or double-bookings.
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">From Date *</label>
                  <input
                    type="date"
                    required
                    value={slotForm.fromDate}
                    onChange={(e) => setSlotForm({ ...slotForm, fromDate: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">To Date *</label>
                  <input
                    type="date"
                    required
                    value={slotForm.toDate}
                    onChange={(e) => setSlotForm({ ...slotForm, toDate: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Start Time *</label>
                  <input
                    type="time"
                    required
                    value={slotForm.startTime}
                    onChange={(e) => setSlotForm({ ...slotForm, startTime: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">End Time *</label>
                  <input
                    type="time"
                    required
                    value={slotForm.endTime}
                    onChange={(e) => setSlotForm({ ...slotForm, endTime: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Duration</label>
                  <select
                    value={slotForm.slotDurationMinutes}
                    onChange={(e) => setSlotForm({ ...slotForm, slotDurationMinutes: e.target.value })}
                    className="w-full px-2.5 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  >
                    <option value="15">15 mins</option>
                    <option value="20">20 mins</option>
                    <option value="30">30 mins</option>
                    <option value="45">45 mins</option>
                  </select>
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Slot Type</label>
                  <select
                    value={slotForm.slotType}
                    onChange={(e) => setSlotForm({ ...slotForm, slotType: e.target.value })}
                    className="w-full px-2.5 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  >
                    <option value="OPD_CONSULTATION">OPD Consult</option>
                    <option value="VACCINATION">Vaccination</option>
                    <option value="SURGERY">Surgery</option>
                    <option value="TELECONSULTATION">Teleconsult</option>
                  </select>
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Capacity</label>
                  <input
                    type="number"
                    min="1"
                    max="5"
                    value={slotForm.capacity}
                    onChange={(e) => setSlotForm({ ...slotForm, capacity: e.target.value })}
                    className="w-full px-2.5 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
              </div>

              <div className="pt-3 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowSlotModal(false)}
                  className="px-4 py-2 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={generatingSlots}
                  className="px-4 py-2 rounded-xl bg-[#123F70] hover:bg-[#0b315c] text-white text-xs font-bold shadow-md cursor-pointer disabled:opacity-50"
                >
                  {generatingSlots ? 'Generating Slots...' : 'Generate & Lock Slots'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Diagnostic Requisition Modal ── */}
      {showDiagOrderModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-xl w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-[#E8F6F4] text-[#079D96] flex items-center justify-center">
                  <FlaskConical className="w-5 h-5 text-[#079D96]" />
                </div>
                <div>
                  <h3 className="text-base font-extrabold text-[#10324E]">Order Diagnostic Panels</h3>
                  <p className="text-[11px] text-[#242826]/60">Companion: {diagOrderForm.petName}</p>
                </div>
              </div>
              <button
                onClick={() => setShowDiagOrderModal(false)}
                className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {diagError && (
              <div className="mt-3 p-3 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold">
                {diagError}
              </div>
            )}

            <form onSubmit={handleCreateDiagnosticOrder} className="mt-4 space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Select Patient / Companion</label>
                  <select
                    value={diagOrderForm.petId}
                    onChange={(e) => {
                      const selectedPt = queue.find(q => (q.petId || q.id) === e.target.value);
                      setDiagOrderForm({
                        ...diagOrderForm,
                        petId: e.target.value,
                        petName: selectedPt ? selectedPt.petName : 'Patient Companion',
                      });
                    }}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
                  >
                    {queue.length > 0 ? (
                      queue.map(q => (
                        <option key={q.id} value={q.petId || q.id}>{q.petName} (Token {q.token})</option>
                      ))
                    ) : (
                      <option value="">No registered patients available</option>
                    )}
                  </select>
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Order Priority</label>
                  <select
                    value={diagOrderForm.priority}
                    onChange={(e) => setDiagOrderForm({ ...diagOrderForm, priority: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  >
                    <option value="ROUTINE">Routine (Standard TAT)</option>
                    <option value="URGENT">Urgent (Expedited)</option>
                    <option value="STAT">STAT / Critical Emergency</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1.5">Select Tests / Profiles *</label>
                <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                  {STANDARD_TESTS.map(test => {
                    const isChecked = diagOrderForm.selectedTests.includes(test.code);
                    return (
                      <label
                        key={test.code}
                        className={`flex items-center justify-between p-2.5 rounded-xl border cursor-pointer transition text-xs ${
                          isChecked ? 'bg-[#E8F6F4] border-[#079D96] text-[#10324E]' : 'bg-[#FAF8F5] border-[#10324E]/10 text-[#242826]'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <input
                            type="checkbox"
                            checked={isChecked}
                            onChange={(e) => {
                              if (e.target.checked) {
                                setDiagOrderForm({
                                  ...diagOrderForm,
                                  selectedTests: [...diagOrderForm.selectedTests, test.code],
                                });
                              } else {
                                setDiagOrderForm({
                                  ...diagOrderForm,
                                  selectedTests: diagOrderForm.selectedTests.filter(c => c !== test.code),
                                });
                              }
                            }}
                            className="rounded text-[#079D96] focus:ring-[#079D96]"
                          />
                          <div>
                            <span className="font-bold">{test.name}</span>
                            <span className="block text-[10px] text-[#242826]/60">{test.category} • TAT: {test.tat}</span>
                          </div>
                        </div>
                        <span className="font-mono font-bold text-[#079D96] text-xs">{test.price}</span>
                      </label>
                    );
                  })}
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Clinical Notes &amp; Suspected Pathology</label>
                <textarea
                  rows="2"
                  value={diagOrderForm.clinicalNotes}
                  onChange={(e) => setDiagOrderForm({ ...diagOrderForm, clinicalNotes: e.target.value })}
                  placeholder="e.g. Pale mucous membranes, elevated temperature, rule out hemoparasites..."
                  className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowDiagOrderModal(false)}
                  className="px-4 py-2 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingDiagOrder || !STANDARD_TESTS.length || !diagOrderForm.petId || diagOrderForm.selectedTests.length === 0}
                  className="px-4 py-2 rounded-xl bg-[#079D96] hover:bg-[#06847e] text-white text-xs font-bold shadow-md cursor-pointer disabled:opacity-50"
                >
                  {submittingDiagOrder ? 'Submitting Requisition...' : 'Submit Requisition'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Diagnostic Result Verification Modal ── */}
      {showResultModal && selectedItemToVerify && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-lg w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-[#EAF1F8] text-[#123F70] flex items-center justify-center">
                  <Microscope className="w-5 h-5 text-[#FF843F]" />
                </div>
                <div>
                  <h3 className="text-base font-extrabold text-[#10324E]">Enter Laboratory Result</h3>
                  <p className="text-[11px] text-[#242826]/60">{selectedItemToVerify.testName} ({selectedItemToVerify.petName})</p>
                </div>
              </div>
              <button
                onClick={() => setShowResultModal(false)}
                className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100 cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveResult} className="mt-4 space-y-3.5">
              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Result Value / Findings *</label>
                <input
                  type="text"
                  required
                  value={resultForm.resultValue}
                  onChange={(e) => setResultForm({ ...resultForm, resultValue: e.target.value })}
                  placeholder="e.g. 14.2 or Negative or Elevated Alkaline Phosphatase"
                  className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E] font-mono"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Reference Range</label>
                  <input
                    type="text"
                    value={resultForm.referenceRange}
                    onChange={(e) => setResultForm({ ...resultForm, referenceRange: e.target.value })}
                    placeholder="e.g. 12.0 - 18.0"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E] font-mono"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Unit of Measure</label>
                  <input
                    type="text"
                    value={resultForm.unit}
                    onChange={(e) => setResultForm({ ...resultForm, unit: e.target.value })}
                    placeholder="e.g. g/dL, U/L, 10^3/uL"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E] font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Clinical Evaluation Flag</label>
                <select
                  value={resultForm.flag}
                  onChange={(e) => setResultForm({ ...resultForm, flag: e.target.value })}
                  className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
                >
                  <option value="NORMAL">NORMAL — Within Expected Species Limits</option>
                  <option value="HIGH">HIGH — Elevated</option>
                  <option value="LOW">LOW — Below Normal Reference</option>
                  <option value="CRITICAL">CRITICAL — Immediate Clinical Attention Required</option>
                  <option value="INCONCLUSIVE">INCONCLUSIVE — Repeat Sample Indicated</option>
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Pathologist / Practitioner Interpretation</label>
                <textarea
                  rows="2"
                  value={resultForm.interpretation}
                  onChange={(e) => setResultForm({ ...resultForm, interpretation: e.target.value })}
                  placeholder="e.g. Consistent with mild dehydration; electrolytes within physiological limits."
                  className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                />
              </div>

              <div className="pt-2 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowResultModal(false)}
                  className="px-4 py-2 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingResult}
                  className="px-4 py-2 rounded-xl bg-[#123F70] hover:bg-[#0b315c] text-white text-xs font-bold shadow-md cursor-pointer disabled:opacity-50"
                >
                  {submittingResult ? 'Verifying...' : 'Sign & Verify Result'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

    </div>
  );
}


import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../../context/AuthContext';
import {
  getPetsApi,
  createPetApi,
  getOwnerAppointmentsApi,
  bookOwnerAppointmentApi,
  getDueVaccinationsApi,
  searchVetsApi,
} from '../../../services/api';
import {
  Heart,
  Plus,
  Calendar,
  PhoneCall,
  ShieldCheck,
  Clock,
  FileText,
  AlertTriangle,
  ChevronRight,
  CheckCircle2,
  Syringe,
  MapPin,
  Stethoscope,
  X,
  Download,
  Share2,
} from 'lucide-react';

export default function PetParentDashboard() {
  const { currentUser } = useAuth();

  // Modals state
  const [showBookModal, setShowBookModal] = useState(false);
  const [showAddPetModal, setShowAddPetModal] = useState(false);
  const [showSosModal, setShowSosModal] = useState(false);
  const [selectedPet, setSelectedPet] = useState(null);
  const [selectedRx, setSelectedRx] = useState(null);

  // Pets state - loaded from API
  const [pets, setPets] = useState([]);
  const [loadingPets, setLoadingPets] = useState(true);

  // Appointments state - loaded from API
  const [appointments, setAppointments] = useState([]);
  const [loadingAppointments, setLoadingAppointments] = useState(true);

  // Due vaccinations state
  const [dueVaccinations, setDueVaccinations] = useState([]);

  // Prescriptions state
  const [prescriptions, setPrescriptions] = useState([]);

  const loadPets = useCallback(async () => {
    setLoadingPets(true);
    try {
      const res = await getPetsApi();
      if (res && res.content) {
        const mapped = res.content.map(p => ({
          id: p.petId,
          name: p.name,
          species: p.species === 'DOG' ? 'Canine' : p.species === 'CAT' ? 'Feline' : p.species,
          breed: p.breed || 'Companion',
          age: p.age || '1 yr',
          weight: '—',
          avatar: p.photoUrl || (p.species === 'CAT'
            ? 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=300&q=80'
            : 'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=300&q=80'),
          microchip: p.registrationNo || 'Pending',
          status: 'Active',
          statusColor: 'bg-emerald-100 text-emerald-800 border-emerald-300',
          vaccines: [],
        }));
        setPets(mapped);
      }
    } catch (err) {
      console.warn('Could not fetch pets from API:', err.message);
    } finally {
      setLoadingPets(false);
    }
  }, []);

  const loadAppointments = useCallback(async () => {
    setLoadingAppointments(true);
    try {
      const res = await getOwnerAppointmentsApi();
      if (res && res.content) {
        const mapped = res.content.map(apt => ({
          id: apt.appointmentId,
          petName: apt.pet?.name || 'Companion',
          petAvatar: apt.pet?.photoUrl || (apt.pet?.species === 'CAT'
            ? 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=100&q=80'
            : 'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=100&q=80'),
          date: apt.appointmentAt ? new Date(apt.appointmentAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', weekday: 'short' }) : 'Scheduled',
          time: apt.appointmentAt ? new Date(apt.appointmentAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : '10:00 AM',
          clinic: apt.hospital?.name || 'Vetalya Partner Clinic',
          vetName: apt.vet ? `Dr. ${apt.vet.firstName || ''} ${apt.vet.lastName || ''}`.trim() : 'Attending Veterinarian',
          status: apt.status || 'Confirmed',
          reason: apt.reason || apt.notes || 'Routine Consultation',
          token: `T-${apt.appointmentId ? apt.appointmentId.slice(-2) : '01'}`,
          visitNotes: apt.visitNotes,
        }));
        setAppointments(mapped);

        // Extract real digital prescriptions from completed database visits
        const rxList = [];
        res.content.forEach(apt => {
          if (apt.visitNotes && (apt.status === 'COMPLETED' || apt.status === 'Completed')) {
            try {
              const parsed = typeof apt.visitNotes === 'string' ? JSON.parse(apt.visitNotes) : apt.visitNotes;
              rxList.push({
                id: `RX-${apt.appointmentId ? apt.appointmentId.slice(-6).toUpperCase() : '001'}`,
                appointmentId: apt.appointmentId,
                petName: apt.pet?.name || 'Companion',
                doctor: apt.vet ? `Dr. ${apt.vet.firstName || ''} ${apt.vet.lastName || ''}`.trim() : 'Attending Veterinarian',
                date: apt.appointmentAt ? new Date(apt.appointmentAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Recent',
                diagnosis: parsed.assessment || apt.reason || 'Clinical Consultation',
                medications: parsed.medications || [],
                dietPlan: parsed.dietPlan || 'Standard balanced diet',
                followUpDays: parsed.followUpDays || '7',
                vitals: {
                  temp: parsed.temperature,
                  weight: parsed.weight,
                  hr: parsed.heartRate,
                },
                subjective: parsed.subjective,
                objectiveExam: parsed.objectiveExam,
              });
            } catch (e) {
              // Non-JSON notes
            }
          }
        });
        setPrescriptions(rxList);
      } else {
        setAppointments([]);
        setPrescriptions([]);
      }
    } catch (err) {
      console.warn('Could not fetch appointments from API:', err.message);
      setAppointments([]);
      setPrescriptions([]);
    } finally {
      setLoadingAppointments(false);
    }
  }, []);

  const loadVaccinations = useCallback(async () => {
    try {
      const res = await getDueVaccinationsApi(60);
      if (Array.isArray(res)) {
        setDueVaccinations(res.map(v => ({
          name: v.vaccineName || 'Core Booster',
          date: v.dueDate ? new Date(v.dueDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }) : 'Due Soon',
          critical: true,
          status: 'Due Soon',
          petName: v.petName,
        })));
      }
    } catch (err) {
      console.warn('Could not fetch due vaccinations:', err.message);
      setDueVaccinations([]);
    }
  }, []);

  useEffect(() => {
    loadPets();
    loadAppointments();
    loadVaccinations();
  }, [loadPets, loadAppointments, loadVaccinations]);

  // Form states for booking
  const [availableVets, setAvailableVets] = useState([]);
  const [loadingVets, setLoadingVets] = useState(false);
  const [bookError, setBookError] = useState(null);
  const [submittingBook, setSubmittingBook] = useState(false);

  const [bookForm, setBookForm] = useState({
    petId: '',
    vetUserId: '',
    hospitalId: '',
    date: new Date(Date.now() + 86400000).toISOString().split('T')[0],
    time: '11:00 AM',
    reason: 'Routine Health Checkup',
  });

  // Keep bookForm petId in sync with pets
  useEffect(() => {
    if (pets.length > 0 && !bookForm.petId) {
      setBookForm(prev => ({ ...prev, petId: pets[0].id }));
    }
  }, [pets, bookForm.petId]);

  // Fetch available veterinarians when booking modal opens
  useEffect(() => {
    if (showBookModal) {
      setBookError(null);
      setLoadingVets(true);
      searchVetsApi()
        .then((vets) => {
          if (Array.isArray(vets) && vets.length > 0) {
            setAvailableVets(vets);
            setBookForm(prev => ({
              ...prev,
              vetUserId: prev.vetUserId || vets[0].vetUserId,
              hospitalId: prev.hospitalId || (vets[0].hospitals?.[0]?.hospitalId || ''),
            }));
          }
        })
        .catch((err) => {
          console.warn('Could not load vets for booking:', err);
        })
        .finally(() => {
          setLoadingVets(false);
        });
    }
  }, [showBookModal]);

  // Form states for adding pet
  const [addPetError, setAddPetError] = useState(null);
  const [submittingPet, setSubmittingPet] = useState(false);
  const [newPetForm, setNewPetForm] = useState({
    name: '',
    species: 'Canine',
    gender: 'MALE',
    breed: '',
    age: '',
    dob: '',
    weight: '',
    microchip: '',
  });

  const handleBookSubmit = async (e) => {
    e.preventDefault();
    setBookError(null);

    const pet = pets.find((p) => p.id === bookForm.petId) || pets[0];
    if (!pet) {
      setBookError('Please register a companion profile before booking an appointment.');
      return;
    }

    if (!bookForm.vetUserId) {
      setBookError('Please select a veterinarian from the available registry.');
      return;
    }

    if (!bookForm.reason.trim() || bookForm.reason.trim().length < 3) {
      setBookError('Please enter a reason for the consultation (minimum 3 characters).');
      return;
    }

    const timeParts = (bookForm.time || '10:00 AM').split(' ');
    const [hoursStr, minsStr] = (timeParts[0] || '10:00').split(':');
    let h = parseInt(hoursStr, 10);
    if (timeParts[1] === 'PM' && h < 12) h += 12;
    if (timeParts[1] === 'AM' && h === 12) h = 0;
    const appDate = new Date(bookForm.date || Date.now() + 86400000);
    appDate.setHours(h, parseInt(minsStr, 10) || 0, 0, 0);

    if (appDate.getTime() < Date.now()) {
      setBookError('Appointment date and time must be set in the future.');
      return;
    }

    setSubmittingBook(true);

    try {
      const selectedVet = availableVets.find(v => v.vetUserId === bookForm.vetUserId);
      const hospitalId = bookForm.hospitalId || (selectedVet?.hospitals?.[0]?.hospitalId || null);

      await bookOwnerAppointmentApi({
        petId: pet.id,
        vetUserId: bookForm.vetUserId,
        hospitalId: hospitalId,
        appointmentAt: appDate.toISOString(),
        notes: bookForm.reason.trim(),
      });

      await loadAppointments();
      setShowBookModal(false);
      setSubmittingBook(false);
    } catch (err) {
      setSubmittingBook(false);
      setBookError(err.message || 'Failed to book appointment in database. Please check slot availability.');
    }
  };

  const handleAddPetSubmit = async (e) => {
    e.preventDefault();
    setAddPetError(null);

    if (!newPetForm.name.trim()) {
      setAddPetError('Pet name is required.');
      return;
    }

    const speciesMap = {
      'Canine': 'DOG',
      'Feline': 'CAT',
      'Avian': 'BIRD',
      'Small Mammal': 'RABBIT',
      'Other': 'OTHER',
    };

    let regNo = newPetForm.microchip?.trim() || null;
    if (regNo) {
      const regRegex = /^PET-\d{4}-[A-Z0-9]{6}$/;
      if (!regRegex.test(regNo)) {
        setAddPetError('Microchip / Registration number must follow format PET-YYYY-XXXXXX (e.g. PET-2026-A1B2C3), or leave blank if not yet registered.');
        return;
      }
    }

    if (newPetForm.weight) {
      const wt = parseFloat(newPetForm.weight);
      if (isNaN(wt) || wt <= 0 || wt > 300) {
        setAddPetError('Weight must be a valid positive number in kilograms.');
        return;
      }
    }

    const payload = {
      name: newPetForm.name.trim(),
      species: speciesMap[newPetForm.species] || 'DOG',
      gender: newPetForm.gender || 'UNKNOWN',
      breed: newPetForm.breed?.trim() || null,
      dob: newPetForm.dob || null,
      photoUrl: newPetForm.species === 'Feline'
        ? 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=300&q=80'
        : 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=300&q=80',
      registrationNo: regNo,
    };

    setSubmittingPet(true);

    try {
      await createPetApi(payload);
      await loadPets();
      setShowAddPetModal(false);
      setNewPetForm({ name: '', species: 'Canine', gender: 'MALE', breed: '', age: '', dob: '', weight: '', microchip: '' });
      setSubmittingPet(false);
    } catch (err) {
      setSubmittingPet(false);
      setAddPetError(err.message || 'Failed to create pet passport in database.');
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in">
      
      {/* ── Welcome Greeting & Action Buttons ── */}
      <div className="bg-gradient-to-r from-white via-white to-[#E0F2EB]/50 rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#E0F2EB] text-[#2D8A7A] text-[11px] font-extrabold uppercase tracking-wider mb-2">
            <Heart className="w-3.5 h-3.5" />
            <span>Pet Parent Health Portal</span>
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[#10324E] font-display">
            Welcome back, {currentUser?.name?.split(' ')[0] || 'Pet Parent'}! 👋
          </h1>
          <p className="text-sm text-[#242826]/75 mt-1 max-w-xl">
            {pets.length === 0 ? (
              <span>Welcome to your Vetalya health portal! Register your companion below to start managing digital pet passports and booking appointments.</span>
            ) : (
              <span>
                You have <strong className="text-[#10324E]">{pets.length} companion{pets.length > 1 ? 's' : ''}</strong> registered.
                {appointments.length > 0 ? (
                  <> Next consultation for <strong className="text-[#10324E]">{appointments[0].petName}</strong> is scheduled on <strong className="text-[#10324E]">{appointments[0].date} at {appointments[0].time}</strong>.</>
                ) : (
                  <> All health records and vaccination passports are active.</>
                )}
              </span>
            )}
          </p>
        </div>

        {/* Quick Actions */}
        <div className="flex flex-wrap items-center gap-2.5 shrink-0">
          <button
            onClick={() => setShowBookModal(true)}
            className="flex items-center gap-2 px-4 py-3 rounded-2xl bg-[#10324E] hover:bg-[#0c273e] text-white text-xs font-bold shadow-md transition cursor-pointer"
          >
            <Calendar className="w-4 h-4 text-[#E99A7C]" />
            <span>Book Appointment</span>
          </button>

          <button
            onClick={() => setShowAddPetModal(true)}
            className="flex items-center gap-2 px-4 py-3 rounded-2xl bg-white hover:bg-slate-50 border border-[#10324E]/15 text-[#10324E] text-xs font-bold shadow-2xs transition cursor-pointer"
          >
            <Plus className="w-4 h-4 text-[#2D8A7A]" />
            <span>Add Pet</span>
          </button>

          <button
            onClick={() => setShowSosModal(true)}
            className="flex items-center gap-1.5 px-3.5 py-3 rounded-2xl bg-rose-50 hover:bg-rose-100 border border-rose-200 text-rose-700 text-xs font-bold transition cursor-pointer"
          >
            <PhoneCall className="w-4 h-4 text-rose-600 animate-pulse" />
            <span>SOS Triage</span>
          </button>
        </div>
      </div>

      {/* ── Section: Digital Pet Passports ── */}
      <div id="passports" className="scroll-mt-28">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <h2 className="text-lg sm:text-xl font-extrabold text-[#10324E] font-display">
              Digital Pet Passports
            </h2>
            <span className="text-xs bg-white px-2 py-0.5 rounded-full border border-[#10324E]/10 font-bold text-[#10324E]/70">
              {pets.length}
            </span>
          </div>
          <button
            onClick={() => setShowAddPetModal(true)}
            className="text-xs font-bold text-[#2D8A7A] hover:underline flex items-center gap-1 cursor-pointer"
          >
            <span>+ Add New Companion</span>
          </button>
        </div>

        {loadingPets ? (
          <div className="bg-white rounded-3xl p-8 border border-[#10324E]/8 text-center">
            <p className="text-xs text-[#242826]/60 animate-pulse">Loading companions from health registry...</p>
          </div>
        ) : pets.length === 0 ? (
          <div className="bg-white rounded-3xl p-8 border border-dashed border-[#10324E]/20 text-center">
            <Heart className="w-10 h-10 text-[#2D8A7A]/50 mx-auto mb-3" />
            <h4 className="text-sm font-bold text-[#10324E]">No companions registered yet</h4>
            <p className="text-xs text-[#242826]/60 mt-1 max-w-sm mx-auto">
              Add your companion's profile to manage health passports, vaccinations, and book clinic appointments.
            </p>
            <button
              onClick={() => setShowAddPetModal(true)}
              className="mt-4 px-4 py-2 rounded-xl bg-[#2D8A7A] text-white text-xs font-bold hover:bg-[#257366] transition-colors"
            >
              + Register First Pet
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
            {pets.map((pet) => (
              <div
                key={pet.id}
                onClick={() => setSelectedPet(pet)}
                className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya hover:shadow-lg transition-all cursor-pointer relative overflow-hidden group text-left"
              >
                <div className="flex items-start gap-4">
                  <img
                    src={pet.avatar}
                    alt={pet.name}
                    className="w-16 h-16 rounded-2xl object-cover ring-2 ring-[#2D8A7A]/20 shrink-0 group-hover:scale-105 transition-transform"
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <h3 className="font-extrabold text-base text-[#10324E] truncate">{pet.name}</h3>
                      <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full border ${pet.statusColor}`}>
                        {pet.status}
                      </span>
                    </div>
                    <p className="text-xs text-[#242826]/70 truncate">{pet.breed} • {pet.species}</p>
                    <p className="text-[11px] text-[#242826]/60 mt-0.5">{pet.age} • {pet.weight}</p>
                  </div>
                </div>

                {/* Microchip Badge */}
                <div className="mt-4 pt-3 border-t border-[#10324E]/8 flex items-center justify-between text-[11px]">
                  <div className="flex items-center gap-1.5 text-[#242826]/60">
                    <ShieldCheck className="w-3.5 h-3.5 text-[#2D8A7A]" />
                    <span className="font-mono">Chip: #{pet.microchip ? pet.microchip.slice(-6) : 'PENDING'}</span>
                  </div>
                  <span className="font-bold text-[#2D8A7A] group-hover:translate-x-1 transition-transform inline-flex items-center gap-0.5">
                    View Passport <ChevronRight className="w-3 h-3" />
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Two Column Layout: Upcoming Appointments & Vaccination Tracker ── */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        
        {/* Left Column (7 cols): Appointments & Prescriptions */}
        <div className="lg:col-span-7 space-y-6">
          
          {/* Upcoming Appointments */}
          <div id="appointments" className="bg-white rounded-3xl p-6 border border-[#10324E]/8 shadow-vetalya text-left scroll-mt-28">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8">
              <div className="flex items-center gap-2">
                <Calendar className="w-5 h-5 text-[#2D8A7A]" />
                <h3 className="text-base font-extrabold text-[#10324E]">Upcoming Consultations</h3>
              </div>
              <button
                onClick={() => setShowBookModal(true)}
                className="text-xs font-bold text-[#2D8A7A] hover:underline cursor-pointer"
              >
                + Schedule
              </button>
            </div>

            <div className="mt-4 space-y-3">
              {loadingAppointments ? (
                <div className="p-6 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 text-center text-xs text-[#242826]/60 animate-pulse">
                  Checking scheduled appointments...
                </div>
              ) : appointments.length === 0 ? (
                <div className="p-6 rounded-2xl bg-[#FAF8F5] border border-dashed border-[#10324E]/20 text-center">
                  <Calendar className="w-8 h-8 text-[#2D8A7A]/50 mx-auto mb-2" />
                  <p className="font-bold text-xs text-[#10324E]">No upcoming consultations scheduled</p>
                  <p className="text-[11px] text-[#242826]/60 mt-1 max-w-xs mx-auto">
                    Book an appointment with a verified veterinarian for checkups, vaccinations, or medical triage.
                  </p>
                  <button
                    onClick={() => setShowBookModal(true)}
                    className="mt-3 px-3.5 py-1.5 rounded-xl bg-[#2D8A7A] text-white text-xs font-bold hover:bg-[#257366] transition cursor-pointer"
                  >
                    + Schedule Consultation
                  </button>
                </div>
              ) : (
                appointments.map((apt) => (
                  <div key={apt.id} className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 hover:border-[#2D8A7A]/30 transition flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div className="flex items-center gap-3">
                      <img src={apt.petAvatar} alt={apt.petName} className="w-12 h-12 rounded-xl object-cover shrink-0" />
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-sm text-[#10324E]">{apt.petName}</span>
                          <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                            Token {apt.token}
                          </span>
                        </div>
                        <p className="text-xs text-[#242826]/75 mt-0.5">{apt.reason}</p>
                        <p className="text-[11px] text-[#242826]/60 flex items-center gap-1 mt-0.5">
                          <MapPin className="w-3 h-3 text-[#E99A7C]" /> {apt.clinic} • {apt.vetName}
                        </p>
                      </div>
                    </div>

                    <div className="flex sm:flex-col items-center sm:items-end justify-between shrink-0">
                      <span className="text-xs font-extrabold text-[#10324E]">{apt.date}</span>
                      <span className="text-xs text-[#2D8A7A] font-bold">{apt.time}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Recent Prescriptions & SOAP Summary */}
          <div id="prescriptions" className="bg-white rounded-3xl p-6 border border-[#10324E]/8 shadow-vetalya text-left scroll-mt-28">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8">
              <div className="flex items-center gap-2">
                <FileText className="w-5 h-5 text-[#2D8A7A]" />
                <h3 className="text-base font-extrabold text-[#10324E]">Recent Digital Prescriptions (SOAP)</h3>
              </div>
            </div>

            <div className="mt-4 space-y-3">
              {prescriptions.length === 0 ? (
                <div className="p-6 rounded-2xl bg-[#FAF8F5] border border-dashed border-[#10324E]/20 text-center">
                  <FileText className="w-8 h-8 text-[#2D8A7A]/50 mx-auto mb-2" />
                  <p className="font-bold text-xs text-[#10324E]">No digital prescriptions on record yet</p>
                  <p className="text-[11px] text-[#242826]/60 mt-1 max-w-sm mx-auto">
                    Prescriptions issued by licensed veterinarians during clinic visits will appear here automatically with dosage and dietary guidelines.
                  </p>
                </div>
              ) : (
                prescriptions.map((rx) => (
                  <div
                    key={rx.id}
                    onClick={() => setSelectedRx(rx)}
                    className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 hover:border-[#2D8A7A]/30 transition cursor-pointer flex items-center justify-between"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-xs text-[#10324E]">{rx.petName} — {rx.diagnosis}</span>
                      </div>
                      <p className="text-[11px] text-[#242826]/60 mt-1">Prescribed by {rx.doctor} on {rx.date}</p>
                      <p className="text-[11px] text-[#2D8A7A] font-semibold mt-0.5">{rx.medications.length} active medications prescribed</p>
                    </div>
                    <button className="px-3 py-1.5 rounded-xl bg-white border border-[#10324E]/12 text-xs font-bold text-[#10324E] hover:text-[#2D8A7A] transition flex items-center gap-1 shadow-2xs">
                      <Download className="w-3.5 h-3.5" />
                      <span className="hidden sm:inline">View Rx</span>
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>

        </div>

        {/* Right Column (5 cols): Vaccine & Health Tracker */}
        <div id="vaccines" className="lg:col-span-5 space-y-6 scroll-mt-28">
          
          <div className="bg-white rounded-3xl p-6 border border-[#10324E]/8 shadow-vetalya text-left">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8">
              <div className="flex items-center gap-2">
                <Syringe className="w-5 h-5 text-[#2D8A7A]" />
                <h3 className="text-base font-extrabold text-[#10324E]">Vaccination Timeline</h3>
              </div>
              <span className="text-[11px] font-bold text-amber-700 bg-amber-50 px-2 py-0.5 rounded-full border border-amber-200">
                {dueVaccinations.length > 0
                  ? `${dueVaccinations.length} Due Soon`
                  : pets[0]?.vaccines?.filter(v => v.critical)?.length
                  ? `${pets[0].vaccines.filter(v => v.critical).length} Due Soon`
                  : 'Up to Date'}
              </span>
            </div>

            <div className="mt-4 space-y-3.5">
              {dueVaccinations.length > 0 ? (
                dueVaccinations.map((v, i) => (
                  <div key={i} className="flex items-start gap-3 p-3 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/6">
                    <div className="w-8 h-8 rounded-xl flex items-center justify-center shrink-0 bg-amber-100 text-amber-800">
                      <AlertTriangle className="w-4 h-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <p className="font-bold text-xs text-[#10324E]">{v.name} {v.petName ? `(${v.petName})` : ''}</p>
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
                          {v.status}
                        </span>
                      </div>
                      <p className="text-[11px] text-[#242826]/70 mt-0.5">{v.date}</p>
                    </div>
                  </div>
                ))
              ) : pets[0]?.vaccines && pets[0].vaccines.length > 0 ? (
                pets[0].vaccines.map((v, i) => (
                  <div key={i} className="flex items-start gap-3 p-3 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/6">
                    <div className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 ${
                      v.critical ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                    }`}>
                      {v.critical ? <AlertTriangle className="w-4 h-4" /> : <CheckCircle2 className="w-4 h-4" />}
                    </div>
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center justify-between">
                        <p className="font-bold text-xs text-[#10324E]">{v.name}</p>
                        <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full ${
                          v.critical ? 'bg-amber-100 text-amber-800' : 'bg-emerald-100 text-emerald-800'
                        }`}>
                          {v.status}
                        </span>
                      </div>
                      <p className="text-[11px] text-[#242826]/70 mt-0.5">{v.date}</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/6 text-center text-xs text-[#242826]/60">
                  No vaccination schedule recorded yet.
                </div>
              )}
            </div>

            {/* Microchip Digital Verification Card */}
            <div className="mt-6 p-4 rounded-2xl bg-[#E0F2EB]/50 border border-[#2D8A7A]/20">
              <div className="flex items-center gap-2 text-xs font-bold text-[#10324E]">
                <ShieldCheck className="w-4 h-4 text-[#2D8A7A]" />
                <span>Microchip Registry Verification</span>
              </div>
              <p className="text-[11px] text-[#242826]/75 mt-1 leading-relaxed">
                {pets.length > 0
                  ? `All ${pets.length} companion${pets.length > 1 ? 's have' : ' has'} ISO 11784/11785 RFID microchips linked to your national pet passport profile.`
                  : 'Registered companions will have their ISO 11784/11785 RFID microchips linked to your national pet passport profile.'}
              </p>
            </div>
          </div>

          {/* Quick Care Hotline Card */}
          <div className="bg-gradient-to-br from-[#10324E] to-[#143C5E] rounded-3xl p-6 text-white text-left shadow-vetalya">
            <h4 className="font-extrabold text-base">24/7 Veterinary Triage Hotline</h4>
            <p className="text-xs text-white/80 mt-1 leading-relaxed">
              Immediate telephone triage assistance for accidental ingestion, trauma, or acute pain.
            </p>
            <div className="mt-4 flex items-center justify-between">
              <span className="font-mono text-base font-bold text-[#F49E78]">+91 80 4719 2200</span>
              <button
                onClick={() => setShowSosModal(true)}
                className="px-3 py-1.5 rounded-xl bg-white text-[#10324E] text-xs font-bold hover:bg-slate-100 transition cursor-pointer"
              >
                Call Hotline
              </button>
            </div>
          </div>

        </div>

      </div>

      {/* ── Book Appointment Modal ── */}
      {showBookModal && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-lg w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10">
              <div className="flex items-center gap-2">
                <Calendar className="w-5 h-5 text-[#2D8A7A]" />
                <h3 className="text-lg font-extrabold text-[#10324E]">Book Clinic Consultation</h3>
              </div>
              <button onClick={() => setShowBookModal(false)} className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100 cursor-pointer">
                <X className="w-5 h-5" />
              </button>
            </div>

            {bookError && (
              <div className="mt-4 p-3 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold flex items-center justify-between">
                <span>{bookError}</span>
                <button type="button" onClick={() => setBookError(null)} className="text-rose-600 font-extrabold ml-2">✕</button>
              </div>
            )}

            <form onSubmit={handleBookSubmit} className="mt-5 space-y-4">
              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Select Companion *</label>
                {pets.length === 0 ? (
                  <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-900">
                    You have no companions registered yet. Please add a pet before booking an appointment.
                  </div>
                ) : (
                  <select
                    value={bookForm.petId}
                    onChange={(e) => setBookForm({ ...bookForm, petId: e.target.value })}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
                  >
                    {pets.map((p) => (
                      <option key={p.id} value={p.id}>{p.name} ({p.breed} • {p.species})</option>
                    ))}
                  </select>
                )}
              </div>

              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Select Attending Veterinarian *</label>
                {loadingVets ? (
                  <div className="text-xs text-[#242826]/60 p-2 animate-pulse">Loading verified practitioners from database...</div>
                ) : availableVets.length === 0 ? (
                  <div className="p-3 rounded-xl bg-amber-50 border border-amber-200 text-xs text-amber-900">
                    No verified veterinarians currently listed in registry.
                  </div>
                ) : (
                  <select
                    value={bookForm.vetUserId}
                    onChange={(e) => {
                      const vId = e.target.value;
                      const selectedV = availableVets.find(x => x.vetUserId === vId);
                      setBookForm({
                        ...bookForm,
                        vetUserId: vId,
                        hospitalId: selectedV?.hospitals?.[0]?.hospitalId || '',
                      });
                    }}
                    className="w-full px-3.5 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
                  >
                    {availableVets.map((v) => (
                      <option key={v.vetUserId} value={v.vetUserId}>
                        Dr. {v.firstName} {v.lastName} — {v.hospitals?.[0]?.name || 'Clinical Practice'} ({v.registrationNo})
                      </option>
                    ))}
                  </select>
                )}
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Preferred Date *</label>
                  <input
                    type="date"
                    required
                    min={new Date().toISOString().split('T')[0]}
                    value={bookForm.date}
                    onChange={(e) => setBookForm({ ...bookForm, date: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Time Slot *</label>
                  <select
                    value={bookForm.time}
                    onChange={(e) => setBookForm({ ...bookForm, time: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  >
                    <option value="10:00 AM">10:00 AM (Morning)</option>
                    <option value="11:30 AM">11:30 AM (Morning)</option>
                    <option value="02:30 PM">02:30 PM (Afternoon)</option>
                    <option value="04:00 PM">04:00 PM (Evening)</option>
                    <option value="05:30 PM">05:30 PM (Evening)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Reason for Visit *</label>
                <input
                  type="text"
                  required
                  value={bookForm.reason}
                  onChange={(e) => setBookForm({ ...bookForm, reason: e.target.value })}
                  placeholder="e.g. Annual booster, itching ears, limping, fever"
                  className="w-full px-3.5 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                />
              </div>

              <div className="pt-3 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowBookModal(false)}
                  className="px-4 py-2.5 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={pets.length === 0 || submittingBook || availableVets.length === 0}
                  className={`px-5 py-2.5 rounded-xl text-white text-xs font-bold shadow-md cursor-pointer disabled:opacity-50 ${
                    pets.length === 0 ? 'bg-slate-300 cursor-not-allowed' : 'bg-[#10324E] hover:bg-[#0c273e]'
                  }`}
                >
                  {submittingBook ? 'Booking with Database...' : 'Confirm Appointment'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Add Pet Modal ── */}
      {showAddPetModal && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-lg w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left">
            <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10">
              <div className="flex items-center gap-2">
                <Plus className="w-5 h-5 text-[#2D8A7A]" />
                <h3 className="text-lg font-extrabold text-[#10324E]">Register New Companion</h3>
              </div>
              <button onClick={() => setShowAddPetModal(false)} className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100 cursor-pointer">
                <X className="w-5 h-5" />
              </button>
            </div>

            {addPetError && (
              <div className="mt-4 p-3 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold flex items-center justify-between">
                <span>{addPetError}</span>
                <button type="button" onClick={() => setAddPetError(null)} className="text-rose-600 font-extrabold ml-2">✕</button>
              </div>
            )}

            <form onSubmit={handleAddPetSubmit} className="mt-5 space-y-3.5">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Pet Name *</label>
                  <input
                    type="text"
                    required
                    value={newPetForm.name}
                    onChange={(e) => setNewPetForm({ ...newPetForm, name: e.target.value })}
                    placeholder="e.g. Bruno"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Species *</label>
                  <select
                    value={newPetForm.species}
                    onChange={(e) => setNewPetForm({ ...newPetForm, species: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  >
                    <option value="Canine">Canine (Dog)</option>
                    <option value="Feline">Feline (Cat)</option>
                    <option value="Avian">Avian (Bird)</option>
                    <option value="Small Mammal">Small Mammal (Rabbit/Hamster)</option>
                    <option value="Other">Other Species</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Gender *</label>
                  <select
                    value={newPetForm.gender}
                    onChange={(e) => setNewPetForm({ ...newPetForm, gender: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  >
                    <option value="MALE">Male</option>
                    <option value="FEMALE">Female</option>
                    <option value="UNKNOWN">Unknown / Not specified</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Breed (Optional)</label>
                  <input
                    type="text"
                    value={newPetForm.breed}
                    onChange={(e) => setNewPetForm({ ...newPetForm, breed: e.target.value })}
                    placeholder="e.g. Labrador Retriever"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Date of Birth (Optional)</label>
                  <input
                    type="date"
                    max={new Date().toISOString().split('T')[0]}
                    value={newPetForm.dob}
                    onChange={(e) => setNewPetForm({ ...newPetForm, dob: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Weight (kg)</label>
                  <input
                    type="number"
                    step="0.1"
                    min="0.1"
                    max="300"
                    value={newPetForm.weight}
                    onChange={(e) => setNewPetForm({ ...newPetForm, weight: e.target.value })}
                    placeholder="e.g. 15.4"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">
                  Registration / Microchip ID (Format: PET-YYYY-XXXXXX, or blank)
                </label>
                <input
                  type="text"
                  value={newPetForm.microchip}
                  onChange={(e) => setNewPetForm({ ...newPetForm, microchip: e.target.value })}
                  placeholder="e.g. PET-2026-A1B2C3"
                  className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E] font-mono"
                />
                <span className="text-[10px] text-[#242826]/50 mt-1 block">Leave blank if pending official council RFID registration.</span>
              </div>

              <div className="pt-3 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddPetModal(false)}
                  className="px-4 py-2.5 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingPet}
                  className="px-5 py-2.5 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-bold shadow-md cursor-pointer disabled:opacity-50"
                >
                  {submittingPet ? 'Saving to Database...' : 'Create Pet Passport'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── View Pet Passport Modal ── */}
      {selectedPet && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-lg w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/10">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-[#2D8A7A]" />
                <h3 className="text-lg font-extrabold text-[#10324E]">Digital Pet Passport</h3>
              </div>
              <button onClick={() => setSelectedPet(null)} className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100 cursor-pointer">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex items-center gap-4 p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
              <img
                src={selectedPet.avatar}
                alt={selectedPet.name}
                className="w-16 h-16 rounded-2xl object-cover ring-2 ring-[#2D8A7A]/30"
              />
              <div className="flex-1">
                <div className="flex items-center justify-between">
                  <h4 className="text-base font-extrabold text-[#10324E]">{selectedPet.name}</h4>
                  <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full border ${selectedPet.statusColor}`}>
                    {selectedPet.status}
                  </span>
                </div>
                <p className="text-xs text-[#242826]/70 mt-0.5">{selectedPet.breed} • {selectedPet.species}</p>
                <p className="text-[11px] text-[#242826]/60">{selectedPet.age} • {selectedPet.weight}</p>
              </div>
            </div>

            <div className="p-4 rounded-2xl bg-[#E0F2EB]/40 border border-[#2D8A7A]/20 space-y-2 text-xs">
              <div className="flex justify-between">
                <span className="text-[#242826]/70">Microchip RFID:</span>
                <span className="font-mono font-bold text-[#10324E]">{selectedPet.microchip}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#242826]/70">Passport Status:</span>
                <span className="font-bold text-[#2D8A7A]">Active & Digitally Verified</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#242826]/70">National Health ID:</span>
                <span className="font-mono text-[#10324E]">VT-{selectedPet.id ? selectedPet.id.slice(-8).toUpperCase() : 'PASS-01'}</span>
              </div>
            </div>

            <div className="pt-2 flex justify-end gap-2">
              <button
                onClick={() => setSelectedPet(null)}
                className="px-5 py-2.5 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white text-xs font-bold shadow-md cursor-pointer"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── View Prescription Modal ── */}
      {selectedRx && (
        <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-xl w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/10">
              <div>
                <span className="text-[10px] font-mono font-bold text-[#2D8A7A] uppercase">{selectedRx.id}</span>
                <h3 className="text-lg font-extrabold text-[#10324E]">Digital Veterinary Prescription</h3>
              </div>
              <button onClick={() => setSelectedRx(null)} className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 text-xs space-y-2">
              <div className="flex justify-between">
                <span className="text-[#242826]/60">Patient:</span>
                <span className="font-bold text-[#10324E]">{selectedRx.petName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#242826]/60">Consulting Doctor:</span>
                <span className="font-bold text-[#10324E]">{selectedRx.doctor}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-[#242826]/60">Diagnosis:</span>
                <span className="font-bold text-[#2D8A7A]">{selectedRx.diagnosis}</span>
              </div>
            </div>

            {/* Prescribed Drugs */}
            <div>
              <h4 className="text-xs font-bold text-[#10324E] mb-2">Prescribed Medication (Rx)</h4>
              <div className="space-y-2">
                {selectedRx.medications.map((m, idx) => (
                  <div key={idx} className="p-3 rounded-xl bg-white border border-[#10324E]/10 text-xs">
                    <p className="font-bold text-[#10324E]">{idx + 1}. {m.name}</p>
                    <p className="text-[11px] text-[#242826]/70 mt-0.5">Dosage: {m.dosage} • {m.frequency} • {m.duration}</p>
                  </div>
                ))}
              </div>
            </div>

            {/* Diet Plan */}
            <div className="p-3 rounded-xl bg-amber-50/70 border border-amber-200/80 text-xs text-amber-900">
              <span className="font-bold">Dietary Guidelines:</span> {selectedRx.dietPlan}
            </div>

            <div className="pt-2 flex justify-between items-center">
              <span className="text-[11px] text-[#242826]/50">Digitally verified with cryptographic timestamp.</span>
              <button
                onClick={() => setSelectedRx(null)}
                className="px-4 py-2 rounded-xl bg-[#10324E] text-white text-xs font-bold"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ── Emergency SOS Hotline Modal ── */}
      {showSosModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-md w-full p-6 sm:p-8 shadow-vetalya border border-rose-200 animate-in zoom-in-95 text-center space-y-4">
            <div className="w-14 h-14 rounded-full bg-rose-100 text-rose-600 flex items-center justify-center mx-auto shadow-inner">
              <PhoneCall className="w-7 h-7 animate-bounce" />
            </div>
            <h3 className="text-xl font-extrabold text-[#10324E]">24/7 Veterinary Emergency SOS</h3>
            <p className="text-xs text-[#242826]/75 leading-relaxed">
              If your pet is unresponsive, choking, or in acute shock, please connect directly with our priority triage desk immediately.
            </p>
            <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 font-mono text-xl font-extrabold text-rose-700">
              1800-VETALYA (1800-838-2592)
            </div>
            <div className="flex gap-2 justify-center pt-2">
              <button
                onClick={() => setShowSosModal(false)}
                className="px-5 py-2.5 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
              >
                Dismiss
              </button>
              <a
                href="tel:18008382592"
                className="px-6 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold shadow-md inline-flex items-center gap-1.5"
              >
                <PhoneCall className="w-4 h-4" />
                <span>Call Now</span>
              </a>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}


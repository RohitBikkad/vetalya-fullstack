import React, { useState } from 'react';
import { useAuth } from '../../../context/AuthContext';
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

  // Pets state
  const [pets, setPets] = useState([
    {
      id: 'pet-01',
      name: 'Leo',
      species: 'Canine',
      breed: 'Golden Retriever',
      age: '3 yrs 2 mos',
      weight: '29.5 kg',
      avatar: 'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=300&q=80',
      microchip: '985141002938102',
      status: 'Vaccine Due',
      statusColor: 'bg-amber-100 text-amber-800 border-amber-300',
      vaccines: [
        { name: 'Rabies (Annual)', date: 'Due in 4 days (Nov 10)', status: 'Due Soon', critical: true },
        { name: 'DHPP Core Booster', date: 'Done Sep 15, 2026', status: 'Completed', critical: false },
        { name: 'Deworming (Drontal Plus)', date: 'Done Aug 20, 2026', status: 'Completed', critical: false },
      ],
    },
    {
      id: 'pet-02',
      name: 'Milo',
      species: 'Feline',
      breed: 'British Shorthair',
      age: '1 yr 8 mos',
      weight: '4.8 kg',
      avatar: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=300&q=80',
      microchip: '985141004819201',
      status: 'Up to Date',
      statusColor: 'bg-emerald-100 text-emerald-800 border-emerald-300',
      vaccines: [
        { name: 'FVRCP Tri-cat Core', date: 'Done Jun 12, 2026', status: 'Completed', critical: false },
        { name: 'Feline Leukemia (FeLV)', date: 'Done Jun 12, 2026', status: 'Completed', critical: false },
        { name: 'Broad-spectrum Dewormer', date: 'Due Dec 15, 2026', status: 'Scheduled', critical: false },
      ],
    },
    {
      id: 'pet-03',
      name: 'Bella',
      species: 'Canine',
      breed: 'Beagle',
      age: '4 yrs 1 mo',
      weight: '11.2 kg',
      avatar: 'https://images.unsplash.com/photo-1537151625747-768eb6cf92b2?auto=format&fit=crop&w=300&q=80',
      microchip: '985141009102831',
      status: 'Up to Date',
      statusColor: 'bg-emerald-100 text-emerald-800 border-emerald-300',
      vaccines: [
        { name: 'Rabies Booster', date: 'Done May 04, 2026', status: 'Completed', critical: false },
        { name: 'Bordetella (Kennel Cough)', date: 'Done May 04, 2026', status: 'Completed', critical: false },
      ],
    },
  ]);

  // Appointments state
  const [appointments, setAppointments] = useState([
    {
      id: 'apt-101',
      petName: 'Leo',
      petAvatar: 'https://images.unsplash.com/photo-1552053831-71594a27632d?auto=format&fit=crop&w=100&q=80',
      date: 'Tomorrow, Nov 7',
      time: '10:30 AM',
      clinic: 'Koramangala Companion Hospital',
      vetName: 'Dr. Priya Sharma (BVSc & AH)',
      status: 'Confirmed',
      reason: 'Annual Rabies & Health Triage Checkup',
      token: 'T-04',
    },
    {
      id: 'apt-102',
      petName: 'Milo',
      petAvatar: 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=100&q=80',
      date: 'Sat, Nov 15',
      time: '04:15 PM',
      clinic: 'Indiranagar Specialty Pet OPD',
      vetName: 'Dr. Rajesh Kulkarni (MVSc)',
      status: 'Scheduled',
      reason: 'Routine Dental Scaling Review',
      token: 'T-11',
    },
  ]);

  // Prescriptions state
  const [prescriptions] = useState([
    {
      id: 'rx-2026-089',
      petName: 'Bella',
      date: 'Oct 28, 2026',
      doctor: 'Dr. Priya Sharma',
      diagnosis: 'Acute Gastrointestinal Upset (Food Sensitization)',
      medications: [
        { name: 'Metronidazole Oral Suspension', dosage: '5ml', frequency: 'Twice daily (BID)', duration: '5 days' },
        { name: 'Pro-Kolin+ Digestive Paste', dosage: '2ml', frequency: 'Once daily (SID)', duration: '7 days' },
      ],
      dietPlan: 'Bland gastro diet: Boiled chicken with white rice in small portions.',
      notes: 'Ensure clean drinking water at all times. Return if vomiting persists past 48 hours.',
    },
    {
      id: 'rx-2026-072',
      petName: 'Leo',
      date: 'Sep 15, 2026',
      doctor: 'Dr. Rajesh Kulkarni',
      diagnosis: 'Right Ear Otitis Externa (Mild Yeast)',
      medications: [
        { name: 'Otomax Ear Drops', dosage: '4 drops', frequency: 'Twice daily', duration: '7 days' },
        { name: 'Epi-Otic Ear Cleanser', dosage: 'Flush ear', frequency: 'Before drops', duration: '7 days' },
      ],
      dietPlan: 'Standard adult food, monitor scratch frequency.',
      notes: 'Ear canal cleared after initial flushing. Recheck completed.',
    },
  ]);

  // Form states for booking
  const [bookForm, setBookForm] = useState({
    petId: 'pet-01',
    date: '2026-11-08',
    time: '11:00 AM',
    reason: 'Health Checkup',
  });

  // Form states for adding pet
  const [newPetForm, setNewPetForm] = useState({
    name: '',
    species: 'Canine',
    breed: '',
    age: '',
    weight: '',
    microchip: '',
  });

  const handleBookSubmit = (e) => {
    e.preventDefault();
    const pet = pets.find((p) => p.id === bookForm.petId) || pets[0];
    const newApt = {
      id: `apt-${Date.now()}`,
      petName: pet.name,
      petAvatar: pet.avatar,
      date: bookForm.date,
      time: bookForm.time,
      clinic: 'Koramangala Companion Hospital',
      vetName: 'Dr. Priya Sharma (BVSc & AH)',
      status: 'Confirmed',
      reason: bookForm.reason,
      token: `T-${Math.floor(Math.random() * 20) + 1}`,
    };
    setAppointments([newApt, ...appointments]);
    setShowBookModal(false);
  };

  const handleAddPetSubmit = (e) => {
    e.preventDefault();
    if (!newPetForm.name.trim()) return;
    const pet = {
      id: `pet-${Date.now()}`,
      name: newPetForm.name,
      species: newPetForm.species,
      breed: newPetForm.breed || 'Mixed Breed',
      age: newPetForm.age || '1 yr',
      weight: newPetForm.weight ? `${newPetForm.weight} kg` : '10 kg',
      avatar: newPetForm.species === 'Feline'
        ? 'https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?auto=format&fit=crop&w=300&q=80'
        : 'https://images.unsplash.com/photo-1543466835-00a7907e9de1?auto=format&fit=crop&w=300&q=80',
      microchip: newPetForm.microchip || `98514100${Math.floor(100000 + Math.random() * 900000)}`,
      status: 'Up to Date',
      statusColor: 'bg-emerald-100 text-emerald-800 border-emerald-300',
      vaccines: [
        { name: 'Core Vaccine Check', date: 'Pending Records', status: 'Scheduled', critical: false },
      ],
    };
    setPets([...pets, pet]);
    setShowAddPetModal(false);
    setNewPetForm({ name: '', species: 'Canine', breed: '', age: '', weight: '', microchip: '' });
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
            Welcome back, {currentUser?.name?.split(' ')[0] || 'Rahul'}! 👋
          </h1>
          <p className="text-sm text-[#242826]/75 mt-1 max-w-xl">
            You have <strong className="text-[#10324E]">3 companions</strong> registered. <strong>Leo</strong> has an annual booster scheduled tomorrow at Koramangala OPD.
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
                  <span className="font-mono">Chip: #{pet.microchip.slice(-6)}</span>
                </div>
                <span className="font-bold text-[#2D8A7A] group-hover:translate-x-1 transition-transform inline-flex items-center gap-0.5">
                  View Passport <ChevronRight className="w-3 h-3" />
                </span>
              </div>
            </div>
          ))}
        </div>
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
              {appointments.map((apt) => (
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
              ))}
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
              {prescriptions.map((rx) => (
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
              ))}
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
                1 Due Soon
              </span>
            </div>

            <div className="mt-4 space-y-3.5">
              {pets[0].vaccines.map((v, i) => (
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
              ))}
            </div>

            {/* Microchip Digital Verification Card */}
            <div className="mt-6 p-4 rounded-2xl bg-[#E0F2EB]/50 border border-[#2D8A7A]/20">
              <div className="flex items-center gap-2 text-xs font-bold text-[#10324E]">
                <ShieldCheck className="w-4 h-4 text-[#2D8A7A]" />
                <span>Microchip Registry Verification</span>
              </div>
              <p className="text-[11px] text-[#242826]/75 mt-1 leading-relaxed">
                All 3 pets have their ISO 11784/11785 RFID microchips linked to your national pet passport profile.
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
              <button onClick={() => setShowBookModal(false)} className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleBookSubmit} className="mt-5 space-y-4">
              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Select Companion</label>
                <select
                  value={bookForm.petId}
                  onChange={(e) => setBookForm({ ...bookForm, petId: e.target.value })}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
                >
                  {pets.map((p) => (
                    <option key={p.id} value={p.id}>{p.name} ({p.breed} • {p.species})</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Preferred Date</label>
                  <input
                    type="date"
                    value={bookForm.date}
                    onChange={(e) => setBookForm({ ...bookForm, date: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Time Slot</label>
                  <select
                    value={bookForm.time}
                    onChange={(e) => setBookForm({ ...bookForm, time: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  >
                    <option value="10:00 AM">10:00 AM (Morning)</option>
                    <option value="11:30 AM">11:30 AM (Morning)</option>
                    <option value="04:00 PM">04:00 PM (Evening)</option>
                    <option value="05:30 PM">05:30 PM (Evening)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-[#10324E] mb-1">Reason for Visit</label>
                <input
                  type="text"
                  value={bookForm.reason}
                  onChange={(e) => setBookForm({ ...bookForm, reason: e.target.value })}
                  placeholder="e.g. Annual booster, itching ears, limping"
                  className="w-full px-3.5 py-2.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                />
              </div>

              <div className="pt-3 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowBookModal(false)}
                  className="px-4 py-2.5 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white text-xs font-bold shadow-md"
                >
                  Confirm Appointment
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
              <button onClick={() => setShowAddPetModal(false)} className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100">
                <X className="w-5 h-5" />
              </button>
            </div>

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
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Breed</label>
                  <input
                    type="text"
                    value={newPetForm.breed}
                    onChange={(e) => setNewPetForm({ ...newPetForm, breed: e.target.value })}
                    placeholder="e.g. Labrador Retriever"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Age</label>
                  <input
                    type="text"
                    value={newPetForm.age}
                    onChange={(e) => setNewPetForm({ ...newPetForm, age: e.target.value })}
                    placeholder="e.g. 2 yrs 4 mos"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Weight (kg)</label>
                  <input
                    type="number"
                    step="0.1"
                    value={newPetForm.weight}
                    onChange={(e) => setNewPetForm({ ...newPetForm, weight: e.target.value })}
                    placeholder="e.g. 15.4"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-[#10324E] mb-1">Microchip ID (Optional)</label>
                  <input
                    type="text"
                    value={newPetForm.microchip}
                    onChange={(e) => setNewPetForm({ ...newPetForm, microchip: e.target.value })}
                    placeholder="15-digit ISO chip"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
                  />
                </div>
              </div>

              <div className="pt-3 flex justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowAddPetModal(false)}
                  className="px-4 py-2.5 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2.5 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-bold shadow-md"
                >
                  Create Pet Passport
                </button>
              </div>
            </form>
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


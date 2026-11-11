import React, { useState } from 'react';
import {
  Users,
  Clock,
  CheckCircle2,
  AlertCircle,
  FileText,
  Search,
  Filter,
  UserPlus,
  ArrowRight,
  ShieldAlert,
  ChevronDown,
  Phone,
  Sparkles
} from 'lucide-react';

export default function LiveQueueSection({
  queue,
  loadingQueue,
  onStartConsultation,
  onUpdateStatus,
  onAddWalkInPatient,
  onOpenQuickPrescription
}) {
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL'); // 'ALL' | 'WAITING' | 'IN_CONSULTATION' | 'COMPLETED'
  const [speciesFilter, setSpeciesFilter] = useState('ALL');
  const [showWalkInModal, setShowWalkInModal] = useState(false);

  // Walk-in form state
  const [walkInForm, setWalkInForm] = useState({
    petName: '',
    species: 'Canine',
    breed: 'Indie / Mixed',
    ownerName: '',
    ownerPhone: '',
    reason: 'Routine Consultation / Checkup',
    triageLevel: 'Standard', // 'Standard' | 'Urgent' | 'Emergency'
  });

  const handleWalkInSubmit = (e) => {
    e.preventDefault();
    if (!walkInForm.petName.trim() || !walkInForm.ownerName.trim()) return;

    onAddWalkInPatient({
      id: `walkin-${Date.now()}`,
      token: `T-W${Math.floor(Math.random() * 90 + 10)}`,
      petName: walkInForm.petName.trim(),
      species: walkInForm.species,
      breed: walkInForm.breed.trim(),
      age: 'Walk-in',
      weight: '—',
      ownerName: walkInForm.ownerName.trim(),
      ownerPhone: walkInForm.ownerPhone.trim() || 'Direct OPD Desk Entry',
      time: new Date().toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
      reason: walkInForm.reason.trim(),
      status: 'Waiting',
      statusColor: 'bg-amber-100 text-amber-800 border-amber-300',
      triageLevel: walkInForm.triageLevel,
      temp: '—',
      hr: '—',
    });

    setWalkInForm({
      petName: '',
      species: 'Canine',
      breed: 'Indie / Mixed',
      ownerName: '',
      ownerPhone: '',
      reason: 'Routine Consultation / Checkup',
      triageLevel: 'Standard',
    });
    setShowWalkInModal(false);
  };

  const filteredQueue = queue.filter((p) => {
    const matchesSearch =
      p.petName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.ownerName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.token.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'WAITING' && p.status === 'Waiting') ||
      (statusFilter === 'IN_CONSULTATION' && p.status === 'In Consultation') ||
      (statusFilter === 'COMPLETED' && p.status === 'Completed');

    const matchesSpecies =
      speciesFilter === 'ALL' ||
      p.species.toLowerCase() === speciesFilter.toLowerCase();

    return matchesSearch && matchesStatus && matchesSpecies;
  });

  return (
    <div className="space-y-4">
      {/* ── Toolbar: Filters & Quick Walk-in Entry ── */}
      <div className="bg-white rounded-3xl p-4 sm:p-5 border border-[#10324E]/8 shadow-vetalya flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 text-left">
        <div className="flex flex-wrap items-center gap-2">
          {/* Status Pills */}
          {[
            { id: 'ALL', label: `All Patients (${queue.length})` },
            { id: 'WAITING', label: 'Waiting in Lobby' },
            { id: 'IN_CONSULTATION', label: 'In Consultation' },
            { id: 'COMPLETED', label: 'Completed' },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setStatusFilter(tab.id)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition cursor-pointer ${
                statusFilter === tab.id
                  ? 'bg-[#10324E] text-white shadow-2xs'
                  : 'bg-[#FAF8F5] text-[#10324E]/70 hover:bg-slate-100'
              }`}
            >
              {tab.label}
            </button>
          ))}

          {/* Species Dropdown */}
          <select
            value={speciesFilter}
            onChange={(e) => setSpeciesFilter(e.target.value)}
            className="px-2.5 py-1.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs font-bold text-[#10324E] focus:outline-none cursor-pointer"
          >
            <option value="ALL">All Species</option>
            <option value="Canine">Canine (Dogs)</option>
            <option value="Feline">Feline (Cats)</option>
            <option value="Exotic">Avian & Exotic</option>
          </select>
        </div>

        <div className="flex items-center gap-2">
          {/* Search Box */}
          <div className="relative flex-1 md:w-56">
            <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-[#242826]/40" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search token or pet..."
              className="w-full pl-8 pr-3 py-1.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs text-[#10324E] focus:outline-none"
            />
          </div>

          {/* Add Walk-in Entry Button */}
          <button
            onClick={() => setShowWalkInModal(true)}
            className="px-3.5 py-2 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-extrabold shadow-sm transition flex items-center gap-1.5 cursor-pointer shrink-0"
          >
            <UserPlus className="w-3.5 h-3.5" />
            <span>Walk-in Patient</span>
          </button>
        </div>
      </div>

      {/* ── Table: Live Patient Queue ── */}
      <div className="bg-white rounded-[32px] p-6 border border-[#10324E]/8 shadow-vetalya text-left overflow-hidden">
        <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8 mb-4">
          <div>
            <h3 className="font-extrabold text-base text-[#10324E]">Live Clinic Queue &amp; Token Station</h3>
            <p className="text-xs text-[#242826]/60">Click on any patient to start clinical examination, enter vital signs, and sign digital prescription.</p>
          </div>
          <span className="text-[11px] font-mono font-bold px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-800 border border-emerald-200">
            {filteredQueue.filter(p => p.status === 'Waiting').length} waiting
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead>
              <tr className="border-b border-[#10324E]/10 text-[#242826]/60 font-bold uppercase text-[10px] tracking-wider">
                <th className="pb-3 pl-2">Token</th>
                <th className="pb-3">Patient Companion</th>
                <th className="pb-3">Parent &amp; Contact</th>
                <th className="pb-3">Vitals (T / HR)</th>
                <th className="pb-3">Chief Complaint</th>
                <th className="pb-3">Triage</th>
                <th className="pb-3">Queue Status</th>
                <th className="pb-3 text-right pr-2">Clinical Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#10324E]/5">
              {loadingQueue ? (
                <tr>
                  <td colSpan="8" className="py-12 text-center text-xs text-[#242826]/60 animate-pulse">
                    Loading clinical queue from Vetalya registry...
                  </td>
                </tr>
              ) : filteredQueue.length === 0 ? (
                <tr>
                  <td colSpan="8" className="py-12 text-center">
                    <div className="max-w-xs mx-auto space-y-2">
                      <Users className="w-8 h-8 text-[#10324E]/30 mx-auto" />
                      <p className="font-bold text-xs text-[#10324E]">No patients matching filter</p>
                      <p className="text-[11px] text-[#242826]/60">
                        Patients booked online or registered at reception walk-in desk will appear here live.
                      </p>
                    </div>
                  </td>
                </tr>
              ) : (
                filteredQueue.map((item) => (
                  <tr key={item.id} className="hover:bg-[#FAF8F5] transition group">
                    <td className="py-3.5 pl-2 font-mono font-extrabold text-[#10324E]">
                      <span className="px-2 py-1 rounded-lg bg-[#E8EEF4] text-[#10324E]">
                        {item.token}
                      </span>
                    </td>
                    <td className="py-3.5">
                      <div className="font-bold text-[#10324E]">{item.petName}</div>
                      <div className="text-[11px] text-[#242826]/60">{item.species} • {item.breed} ({item.weight})</div>
                    </td>
                    <td className="py-3.5">
                      <div className="font-semibold text-[#10324E]">{item.ownerName}</div>
                      <div className="text-[10px] text-[#242826]/60 flex items-center gap-1">
                        <Phone className="w-2.5 h-2.5 text-[#2D8A7A]" />
                        <span>{item.ownerPhone}</span>
                      </div>
                    </td>
                    <td className="py-3.5 font-mono text-[11px]">
                      <span className="text-[#10324E]">{item.temp !== '—' ? `${item.temp}°F` : '—'}</span>
                      {' / '}
                      <span className="text-[#2D8A7A]">{item.hr !== '—' ? `${item.hr} bpm` : '—'}</span>
                    </td>
                    <td className="py-3.5 max-w-xs">
                      <p className="text-[11px] text-[#242826]/80 truncate" title={item.reason}>
                        {item.reason}
                      </p>
                    </td>
                    <td className="py-3.5">
                      {item.triageLevel === 'Emergency' ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-extrabold bg-rose-100 text-rose-800 border border-rose-300 animate-pulse">
                          CRITICAL
                        </span>
                      ) : item.triageLevel === 'Urgent' ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-amber-100 text-amber-800 border border-amber-300">
                          Urgent
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded-full text-[10px] text-slate-600 bg-slate-100">
                          Standard
                        </span>
                      )}
                    </td>
                    <td className="py-3.5">
                      <select
                        value={item.status}
                        onChange={(e) => onUpdateStatus(item.id, e.target.value)}
                        className={`text-[10px] font-bold px-2 py-1 rounded-lg border cursor-pointer focus:outline-none ${
                          item.status === 'Completed'
                            ? 'bg-slate-100 text-slate-700 border-slate-300'
                            : item.status === 'In Consultation'
                            ? 'bg-blue-100 text-blue-800 border-blue-300'
                            : 'bg-amber-100 text-amber-800 border-amber-300'
                        }`}
                      >
                        <option value="Waiting">Waiting</option>
                        <option value="In Consultation">In Consultation</option>
                        <option value="Completed">Completed</option>
                        <option value="No Show">No Show</option>
                      </select>
                    </td>
                    <td className="py-3.5 text-right pr-2">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => onStartConsultation(item)}
                          className="px-3 py-1.5 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white font-bold text-xs shadow-2xs transition inline-flex items-center gap-1 cursor-pointer"
                        >
                          <FileText className="w-3 h-3 text-[#F49E78]" />
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

      {/* ── Modal: Add Walk-in Patient Quick Entry ── */}
      {showWalkInModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-md w-full p-6 shadow-vetalya border border-[#10324E]/10 text-left animate-in zoom-in-95 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/8">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-[#E0F2EB] text-[#2D8A7A] flex items-center justify-center font-bold">
                  <UserPlus className="w-4 h-4" />
                </div>
                <div>
                  <h4 className="font-extrabold text-sm text-[#10324E]">Direct Walk-in Patient Entry</h4>
                  <p className="text-[11px] text-[#242826]/60">Generate instant clinic queue token for unannounced visit</p>
                </div>
              </div>
              <button
                onClick={() => setShowWalkInModal(false)}
                className="text-[#242826]/40 hover:text-[#10324E] font-extrabold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleWalkInSubmit} className="space-y-3 text-xs">
              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Patient Companion Name *</label>
                <input
                  type="text"
                  required
                  value={walkInForm.petName}
                  onChange={(e) => setWalkInForm({ ...walkInForm, petName: e.target.value })}
                  placeholder="e.g. Bruno, Bella, Simba"
                  className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Species</label>
                  <select
                    value={walkInForm.species}
                    onChange={(e) => setWalkInForm({ ...walkInForm, species: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                  >
                    <option value="Canine">Canine (Dog)</option>
                    <option value="Feline">Feline (Cat)</option>
                    <option value="Avian">Avian (Bird)</option>
                    <option value="Exotic">Exotic</option>
                  </select>
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Breed</label>
                  <input
                    type="text"
                    value={walkInForm.breed}
                    onChange={(e) => setWalkInForm({ ...walkInForm, breed: e.target.value })}
                    placeholder="e.g. Golden Retriever"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Parent Name *</label>
                  <input
                    type="text"
                    required
                    value={walkInForm.ownerName}
                    onChange={(e) => setWalkInForm({ ...walkInForm, ownerName: e.target.value })}
                    placeholder="e.g. Rajesh Kumar"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Contact Phone</label>
                  <input
                    type="tel"
                    value={walkInForm.ownerPhone}
                    onChange={(e) => setWalkInForm({ ...walkInForm, ownerPhone: e.target.value })}
                    placeholder="e.g. +91 9876543210"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Chief Complaint / Reason for Visit</label>
                <input
                  type="text"
                  value={walkInForm.reason}
                  onChange={(e) => setWalkInForm({ ...walkInForm, reason: e.target.value })}
                  placeholder="e.g. Vomiting since morning, limping, routine vaccine"
                  className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Triage Priority</label>
                <div className="grid grid-cols-3 gap-2">
                  {[
                    { id: 'Standard', label: 'Standard OPD', color: 'bg-slate-100 text-slate-800' },
                    { id: 'Urgent', label: 'Urgent Attention', color: 'bg-amber-100 text-amber-900' },
                    { id: 'Emergency', label: 'Emergency / Triage', color: 'bg-rose-100 text-rose-900 font-black' },
                  ].map((level) => (
                    <button
                      type="button"
                      key={level.id}
                      onClick={() => setWalkInForm({ ...walkInForm, triageLevel: level.id })}
                      className={`py-1.5 px-2 rounded-xl text-[10px] font-bold transition border cursor-pointer ${
                        walkInForm.triageLevel === level.id
                          ? 'border-[#10324E] ring-2 ring-[#10324E]/20'
                          : 'border-transparent opacity-60'
                      } ${level.color}`}
                    >
                      {level.label}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowWalkInModal(false)}
                  className="px-4 py-2 rounded-xl border border-[#10324E]/15 font-bold text-[#10324E] cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white font-extrabold shadow-sm transition cursor-pointer flex items-center gap-1.5"
                >
                  <span>Issue Token &amp; Add</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}


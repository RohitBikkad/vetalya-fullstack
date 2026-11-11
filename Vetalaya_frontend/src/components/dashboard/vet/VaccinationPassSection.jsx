import React, { useState } from 'react';
import {
  ShieldCheck,
  Calendar,
  Syringe,
  Printer,
  Plus,
  CheckCircle2,
  AlertCircle,
  FileCheck,
  Search,
  ArrowRight,
  Clock,
  Sparkles,
  Download
} from 'lucide-react';
import { VACCINE_CATALOG } from './data/vetFormulary';

export default function VaccinationPassSection({
  doctorProfile,
  hospitalDetails,
  onAdministerVaccine
}) {
  const [vaccinations, setVaccinations] = useState([
    {
      id: 'vac-1',
      petName: 'Leo',
      petId: 'PET-01M71N6A7E',
      species: 'Canine',
      breed: 'Golden Retriever',
      ownerName: 'Rahul Verma',
      vaccineName: 'Nobivac DHPPi + L (9-in-1)',
      manufacturer: 'MSD Animal Health',
      batchNo: 'NBV-2026-9921',
      dateGiven: new Date(Date.now() - 15 * 86400000).toISOString().split('T')[0],
      nextDueDate: new Date(Date.now() + 350 * 86400000).toISOString().split('T')[0],
      status: 'Up to Date',
      doctorVerified: true,
      isRabies: false,
    },
    {
      id: 'vac-2',
      petName: 'Leo',
      petId: 'PET-01M71N6A7E',
      species: 'Canine',
      breed: 'Golden Retriever',
      ownerName: 'Rahul Verma',
      vaccineName: 'Nobivac Rabies (Inactivated Anti-Rabies)',
      manufacturer: 'MSD Animal Health',
      batchNo: 'RAB-8812-701A',
      dateGiven: new Date(Date.now() - 40 * 86400000).toISOString().split('T')[0],
      nextDueDate: new Date(Date.now() + 325 * 86400000).toISOString().split('T')[0],
      status: 'Up to Date',
      doctorVerified: true,
      isRabies: true,
    },
    {
      id: 'vac-3',
      petName: 'Milo',
      petId: 'PET-92A88B31',
      species: 'Feline',
      breed: 'Persian Longhair',
      ownerName: 'Ananya Roy',
      vaccineName: 'Felocell 4 (Tricat + Chlamydia)',
      manufacturer: 'Zoetis',
      batchNo: 'FEL-4491-002',
      dateGiven: new Date(Date.now() - 5 * 86400000).toISOString().split('T')[0],
      nextDueDate: new Date(Date.now() + 16 * 86400000).toISOString().split('T')[0],
      status: 'Booster Due Soon',
      doctorVerified: true,
      isRabies: false,
    },
  ]);

  const [showLogModal, setShowLogModal] = useState(false);
  const [selectedRabiesCert, setSelectedRabiesCert] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Form State for new vaccination
  const [form, setForm] = useState({
    petName: '',
    petId: '',
    species: 'Canine',
    breed: '',
    ownerName: '',
    vaccineName: 'Nobivac DHPPi + L (7-in-1 / 9-in-1)',
    manufacturer: 'MSD Animal Health',
    batchNo: '',
    expiryDate: '2027-12-31',
    dateGiven: new Date().toISOString().split('T')[0],
    isBooster: false,
    isRabies: false,
    site: 'Subcutaneous (Right Scapula)',
  });

  const handleVaccineTypeChange = (vaccineName) => {
    const found = VACCINE_CATALOG.find((v) => v.name === vaccineName);
    const isRabies = vaccineName.toLowerCase().includes('rabies');
    setForm((prev) => ({
      ...prev,
      vaccineName,
      isRabies,
      manufacturer: vaccineName.includes('Nobivac') ? 'MSD Animal Health' : 'Zoetis / Indian Immunologicals',
    }));
  };

  const handleLogSubmit = (e) => {
    e.preventDefault();
    if (!form.petName.trim() || !form.batchNo.trim()) return;

    // Calculate next due date
    const given = new Date(form.dateGiven);
    const addDays = form.isBooster ? 21 : 365;
    const nextDue = new Date(given.getTime() + addDays * 86400000).toISOString().split('T')[0];

    const newRecord = {
      id: `vac-${Date.now()}`,
      petName: form.petName.trim(),
      petId: form.petId.trim() || `PET-${Math.random().toString(36).substring(2, 8).toUpperCase()}`,
      species: form.species,
      breed: form.breed.trim() || 'Companion',
      ownerName: form.ownerName.trim() || 'Verified Client',
      vaccineName: form.vaccineName,
      manufacturer: form.manufacturer,
      batchNo: form.batchNo.trim(),
      dateGiven: form.dateGiven,
      nextDueDate: nextDue,
      status: 'Up to Date',
      doctorVerified: true,
      isRabies: form.isRabies,
    };

    setVaccinations([newRecord, ...vaccinations]);
    setShowLogModal(false);
  };

  const filteredVaccinations = vaccinations.filter(
    (v) =>
      v.petName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      v.vaccineName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      v.petId.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6 text-left">
      {/* ── Top Action Banner ── */}
      <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        <div>
          <span className="text-[10px] font-extrabold uppercase tracking-wider px-2.5 py-0.5 rounded-full bg-[#E0F2EB] text-[#2D8A7A] inline-flex items-center gap-1 mb-1">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Digital Pet Passport Immunization Registry</span>
          </span>
          <h3 className="text-xl font-black text-[#10324E]">Lifelong Vaccination Records &amp; Rabies Certificates</h3>
          <p className="text-xs text-[#242826]/70 mt-0.5">
            Administer vaccines, calculate automated booster dates, and generate statutory Municipal Rabies Certificates.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="relative">
            <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-[#242826]/40" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search Pet ID or vaccine..."
              className="pl-8 pr-3 py-1.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 text-xs focus:outline-none w-48 sm:w-60"
            />
          </div>

          <button
            onClick={() => setShowLogModal(true)}
            className="px-4 py-2 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white text-xs font-extrabold flex items-center gap-1.5 shadow-sm transition cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Administer Vaccine</span>
          </button>
        </div>
      </div>

      {/* ── Vaccinations History Table ── */}
      <div className="bg-white rounded-[32px] p-6 border border-[#10324E]/8 shadow-vetalya overflow-hidden">
        <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8 mb-4">
          <h4 className="font-extrabold text-sm text-[#10324E]">Clinical Vaccination Registry ({filteredVaccinations.length} records)</h4>
          <span className="text-[10px] text-[#242826]/60 font-mono">Synced with Pet Parent Digital Passport</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead>
              <tr className="border-b border-[#10324E]/10 text-[#242826]/60 font-bold uppercase text-[10px] tracking-wider">
                <th className="pb-3 pl-2">Patient Companion</th>
                <th className="pb-3">Pet ID</th>
                <th className="pb-3">Vaccine Name &amp; Manufacturer</th>
                <th className="pb-3">Batch / Lot No.</th>
                <th className="pb-3">Date Given</th>
                <th className="pb-3">Next Due Date</th>
                <th className="pb-3">Status</th>
                <th className="pb-3 text-right pr-2">Certificate</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#10324E]/5">
              {filteredVaccinations.map((vac) => (
                <tr key={vac.id} className="hover:bg-[#FAF8F5] transition">
                  <td className="py-3.5 pl-2">
                    <div className="font-bold text-[#10324E]">{vac.petName}</div>
                    <div className="text-[11px] text-[#242826]/60">{vac.species} • {vac.breed}</div>
                  </td>
                  <td className="py-3.5 font-mono text-[11px] text-[#2D8A7A] font-bold">
                    {vac.petId}
                  </td>
                  <td className="py-3.5">
                    <div className="font-bold text-[#10324E]">{vac.vaccineName}</div>
                    <div className="text-[10px] text-[#242826]/50">{vac.manufacturer}</div>
                  </td>
                  <td className="py-3.5 font-mono text-[11px] text-slate-700">
                    {vac.batchNo}
                  </td>
                  <td className="py-3.5 font-mono text-slate-600">
                    {vac.dateGiven}
                  </td>
                  <td className="py-3.5 font-mono font-bold text-[#10324E]">
                    {vac.nextDueDate}
                  </td>
                  <td className="py-3.5">
                    <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
                      {vac.status}
                    </span>
                  </td>
                  <td className="py-3.5 text-right pr-2">
                    {vac.isRabies ? (
                      <button
                        onClick={() => setSelectedRabiesCert(vac)}
                        className="px-3 py-1 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-[11px] font-bold shadow-2xs transition inline-flex items-center gap-1 cursor-pointer"
                      >
                        <FileCheck className="w-3 h-3" />
                        <span>Rabies Cert</span>
                      </button>
                    ) : (
                      <span className="text-[10px] text-[#242826]/40">Passport Logged</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Modal: Administer & Log Vaccine ── */}
      {showLogModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4">
          <div className="bg-white rounded-[32px] max-w-lg w-full p-6 shadow-vetalya border border-[#10324E]/10 space-y-4 animate-in zoom-in-95">
            <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/8">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-[#E0F2EB] text-[#2D8A7A] flex items-center justify-center font-bold">
                  <Syringe className="w-4 h-4" />
                </div>
                <div>
                  <h4 className="font-extrabold text-sm text-[#10324E]">Log Vaccine Administration</h4>
                  <p className="text-[11px] text-[#242826]/60">Auto-updates patient digital health passport &amp; reminder schedule</p>
                </div>
              </div>
              <button onClick={() => setShowLogModal(false)} className="text-[#242826]/40 hover:text-[#10324E] font-bold">
                ✕
              </button>
            </div>

            <form onSubmit={handleLogSubmit} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Patient Name *</label>
                  <input
                    type="text"
                    required
                    value={form.petName}
                    onChange={(e) => setForm({ ...form, petName: e.target.value })}
                    placeholder="e.g. Leo"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Pet ID (Passport)</label>
                  <input
                    type="text"
                    value={form.petId}
                    onChange={(e) => setForm({ ...form, petId: e.target.value })}
                    placeholder="PET-XXXXX"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 font-mono focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Vaccine Formula *</label>
                <select
                  value={form.vaccineName}
                  onChange={(e) => handleVaccineTypeChange(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 font-bold text-[#10324E] focus:outline-none"
                >
                  {VACCINE_CATALOG.map((v, i) => (
                    <option key={i} value={v.name}>{v.name} ({v.species})</option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Manufacturer</label>
                  <input
                    type="text"
                    value={form.manufacturer}
                    onChange={(e) => setForm({ ...form, manufacturer: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Batch / Lot Number *</label>
                  <input
                    type="text"
                    required
                    value={form.batchNo}
                    onChange={(e) => setForm({ ...form, batchNo: e.target.value })}
                    placeholder="e.g. NBV-88219"
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 font-mono font-bold focus:outline-none"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Date Administered</label>
                  <input
                    type="date"
                    value={form.dateGiven}
                    onChange={(e) => setForm({ ...form, dateGiven: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 font-mono focus:outline-none"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-bold text-[#10324E] mb-1">Batch Expiry</label>
                  <input
                    type="date"
                    value={form.expiryDate}
                    onChange={(e) => setForm({ ...form, expiryDate: e.target.value })}
                    className="w-full px-3 py-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 font-mono focus:outline-none"
                  />
                </div>
              </div>

              <div className="p-3 rounded-xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-2">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={form.isBooster}
                    onChange={(e) => setForm({ ...form, isBooster: e.target.checked })}
                    className="rounded border-slate-300 text-[#10324E] focus:ring-0"
                  />
                  <span className="text-[11px] font-bold text-[#10324E]">
                    This is a primary puppy booster dose (Next booster in +21 days instead of annual +365 days)
                  </span>
                </label>
              </div>

              <div className="pt-2 flex items-center justify-end gap-2">
                <button
                  type="button"
                  onClick={() => setShowLogModal(false)}
                  className="px-4 py-2 rounded-xl border border-[#10324E]/15 font-bold text-[#10324E]"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white font-extrabold shadow-sm transition flex items-center gap-1.5 cursor-pointer"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  <span>Log &amp; Seal Vaccine</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* ── Modal: Official Rabies Certificate Generator ── */}
      {selectedRabiesCert && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
          <div className="bg-white rounded-[32px] max-w-2xl w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 my-auto text-left space-y-5 animate-in zoom-in-95">
            <div className="flex items-center justify-between pb-3 border-b border-slate-200">
              <span className="text-xs font-bold text-[#2D8A7A] uppercase tracking-wider flex items-center gap-1">
                <ShieldCheck className="w-4 h-4" />
                <span>Statutory Rabies Vaccination Certificate</span>
              </span>
              <button onClick={() => setSelectedRabiesCert(null)} className="text-slate-400 hover:text-slate-700 font-bold">
                ✕
              </button>
            </div>

            {/* Certificate Printable Sheet */}
            <div className="border-2 border-[#10324E] p-6 rounded-2xl bg-white text-slate-800 space-y-4 font-serif">
              <div className="text-center space-y-1 border-b border-slate-300 pb-3">
                <h3 className="text-lg font-black text-[#10324E] tracking-tight uppercase font-sans">
                  Official Anti-Rabies Vaccination Certificate
                </h3>
                <p className="text-[11px] text-slate-600 font-sans">
                  Compliant with Municipal Pet Licensing and Ministry of Fisheries, Animal Husbandry & Dairying
                </p>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs font-sans">
                <div>
                  <span className="text-slate-500 text-[10px] block">PATIENT NAME & SPECIES</span>
                  <span className="font-bold text-[#10324E]">{selectedRabiesCert.petName} ({selectedRabiesCert.species})</span>
                </div>
                <div>
                  <span className="text-slate-500 text-[10px] block">PET PASSPORT ID</span>
                  <span className="font-mono font-bold text-[#2D8A7A]">{selectedRabiesCert.petId}</span>
                </div>
                <div>
                  <span className="text-slate-500 text-[10px] block">BREED & IDENTIFICATION</span>
                  <span>{selectedRabiesCert.breed}</span>
                </div>
                <div>
                  <span className="text-slate-500 text-[10px] block">OWNER NAME</span>
                  <span className="font-semibold">{selectedRabiesCert.ownerName}</span>
                </div>
              </div>

              <div className="bg-[#FAF8F5] p-3 rounded-xl border border-slate-200 text-xs font-sans space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-slate-600">Vaccine Administered:</span>
                  <span className="font-bold text-[#10324E]">{selectedRabiesCert.vaccineName}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Manufacturer &amp; Batch No:</span>
                  <span className="font-mono">{selectedRabiesCert.manufacturer} • Batch #{selectedRabiesCert.batchNo}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Date of Vaccination:</span>
                  <span className="font-bold">{selectedRabiesCert.dateGiven}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-600">Valid Until (Annual Booster):</span>
                  <span className="font-bold text-emerald-800">{selectedRabiesCert.nextDueDate}</span>
                </div>
              </div>

              {/* Doctor Attestation */}
              <div className="pt-4 flex items-end justify-between border-t border-slate-300 text-xs font-sans">
                <div className="text-[10px] text-slate-400 max-w-xs">
                  I hereby certify that the animal described herein was inspected and vaccinated against Rabies with an authorized inactivated vaccine on the date stated.
                </div>
                <div className="text-right">
                  <div className="w-24 h-10 border border-dashed border-slate-300 rounded-lg flex items-center justify-center text-[10px] text-slate-400 font-sans mb-1 ml-auto">
                    [Doctor Stamp]
                  </div>
                  <div className="font-bold text-[#10324E]">Dr. {doctorProfile?.firstName || 'Priya'} {doctorProfile?.lastName || 'Sharma'}</div>
                  <div className="text-[10px] text-slate-500">Reg No: {doctorProfile?.registrationNo || 'MSVC-2024-8891'}</div>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between pt-2">
              <span className="text-[11px] text-slate-500">Valid for domestic flight travel and municipal tags.</span>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setSelectedRabiesCert(null)}
                  className="px-4 py-2 rounded-xl border border-slate-300 text-xs font-bold text-slate-700"
                >
                  Close
                </button>
                <button
                  type="button"
                  onClick={() => window.print()}
                  className="px-4 py-2 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white text-xs font-bold shadow-sm flex items-center gap-1.5 cursor-pointer"
                >
                  <Printer className="w-3.5 h-3.5" />
                  <span>Print Certificate</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

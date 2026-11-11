import React, { useState, useRef } from 'react';
import {
  FileText,
  Plus,
  Trash2,
  Printer,
  Share2,
  Send,
  X,
  CheckCircle2,
  ShieldCheck,
  Stethoscope,
  Sparkles,
  Phone,
  MessageCircle,
  Download,
  AlertCircle
} from 'lucide-react';
import { INDIAN_VET_FORMULARY } from './data/vetFormulary';

export default function PrescriptionBuilder({
  patient,
  soapData,
  doctorProfile,
  hospitalDetails,
  onClose,
  onFinalizePrescription
}) {
  const [medications, setMedications] = useState([
    {
      name: 'Clavam Vet (Amoxicillin + Clavulanic Acid)',
      category: 'Antibiotic',
      formulation: 'Tablet (375mg)',
      dosage: '1 tab',
      frequency: 'BID (Twice daily)',
      timing: 'After food',
      duration: '5 days',
      instructions: 'Administer with fresh food or peanut butter',
    },
    {
      name: 'Melonex (Meloxicam)',
      category: 'NSAID / Anti-inflammatory',
      formulation: 'Oral Suspension (1.5mg/ml)',
      dosage: '1.2 ml',
      frequency: 'SID (Once daily)',
      timing: 'Strictly after food',
      duration: '3 days',
      instructions: 'Ensure pet is well-hydrated',
    },
  ]);

  const [dietaryAdvice, setDietaryAdvice] = useState(
    soapData?.dietaryAdvice || 'Bland gastrointestinal diet (boiled chicken + rice or prescription veterinary GI kibble).'
  );
  const [followUpDays, setFollowUpDays] = useState(soapData?.followUpDays || '5');
  const [doctorNotes, setDoctorNotes] = useState(
    soapData?.doctorNotes || 'Maintain fresh drinking water. Complete full course of antibiotics even if symptoms resolve.'
  );

  const [viewMode, setViewMode] = useState('edit'); // 'edit' | 'preview'
  const [searchTerm, setSearchTerm] = useState('');
  const [successNotice, setSuccessNotice] = useState(null);
  const [isFinalizing, setIsFinalizing] = useState(false);

  const printRef = useRef(null);

  // Add drug from formulary
  const handleAddFromFormulary = (drug) => {
    setMedications((prev) => [
      ...prev,
      {
        name: drug.name,
        category: drug.category,
        formulation: drug.formulation,
        dosage: drug.defaultDosage.split(' ')[0] || '1 tab',
        frequency: drug.frequency,
        timing: 'After food',
        duration: drug.duration,
        instructions: drug.instructions,
      },
    ]);
  };

  // Add blank medication row
  const handleAddBlankRow = () => {
    setMedications((prev) => [
      ...prev,
      {
        name: '',
        category: 'General',
        formulation: 'Tablet',
        dosage: '1 tab',
        frequency: 'BID (Twice daily)',
        timing: 'After food',
        duration: '5 days',
        instructions: 'As directed',
      },
    ]);
  };

  const handleRemoveRow = (idx) => {
    setMedications((prev) => prev.filter((_, i) => i !== idx));
  };

  const handleMedChange = (idx, field, value) => {
    setMedications((prev) => {
      const copy = [...prev];
      copy[idx][field] = value;
      return copy;
    });
  };

  // Print Prescription
  const handlePrint = () => {
    window.print();
  };

  // WhatsApp Dispatch Simulation
  const handleSendWhatsApp = () => {
    const text = encodeURIComponent(
      `🐾 *Vetalya Prescription for ${patient?.petName || 'Patient'}*\n` +
      `Prescribed by Dr. ${doctorProfile?.firstName || 'Veterinarian'} ${doctorProfile?.lastName || ''} (VCI Reg: ${doctorProfile?.registrationNo || 'Verified'})\n` +
      `Primary Diagnosis: ${soapData?.primaryDiagnosis || 'Health Consultation'}\n\n` +
      `*Prescribed Medications:*\n` +
      medications.map((m, i) => `${i + 1}. *${m.name}* - ${m.dosage} (${m.frequency}) for ${m.duration} [${m.instructions}]`).join('\n') +
      `\n\n*Diet:* ${dietaryAdvice}\n*Follow-Up:* In ${followUpDays} days.\nView Digital Pass: https://www.vetalya.com/passport`
    );
    window.open(`https://api.whatsapp.com/send?text=${text}`, '_blank');
    setSuccessNotice('Prescription details copied to WhatsApp trigger!');
  };

  const handleComplete = async () => {
    setIsFinalizing(true);
    try {
      await onFinalizePrescription({
        medications,
        dietaryAdvice,
        followUpDays,
        doctorNotes,
        primaryDiagnosis: soapData?.primaryDiagnosis,
        vitals: {
          temperature: soapData?.temperature,
          weight: soapData?.weight,
          heartRate: soapData?.heartRate,
        },
      });
      setSuccessNotice('Prescription sealed and saved to patient EHR!');
      setTimeout(() => {
        onClose();
      }, 1200);
    } catch (e) {
      setIsFinalizing(false);
    }
  };

  const filteredFormulary = INDIAN_VET_FORMULARY.filter((d) =>
    d.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    d.category.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-2 sm:p-4 overflow-y-auto">
      <div className="bg-white rounded-[32px] max-w-4xl w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 my-auto text-left max-h-[94vh] flex flex-col animate-in zoom-in-95">
        
        {/* Modal Top Bar */}
        <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-[#E0F2EB] text-[#2D8A7A] flex items-center justify-center font-extrabold text-base">
              Rx
            </div>
            <div>
              <h3 className="text-lg font-black text-[#10324E]">
                Digital Prescription: {patient?.petName || 'Patient Companion'}
              </h3>
              <p className="text-xs text-[#242826]/70">
                Diagnosis: <strong>{soapData?.primaryDiagnosis || 'Acute Consultation'}</strong> • Weight: {soapData?.weight || '—'} kg
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <div className="flex p-0.5 bg-[#FAF8F5] rounded-xl border border-[#10324E]/10 text-xs">
              <button
                type="button"
                onClick={() => setViewMode('edit')}
                className={`px-3 py-1 rounded-lg font-bold transition cursor-pointer ${
                  viewMode === 'edit' ? 'bg-[#10324E] text-white' : 'text-[#10324E]/70 hover:text-[#10324E]'
                }`}
              >
                Drug Editor
              </button>
              <button
                type="button"
                onClick={() => setViewMode('preview')}
                className={`px-3 py-1 rounded-lg font-bold transition cursor-pointer ${
                  viewMode === 'preview' ? 'bg-[#10324E] text-white' : 'text-[#10324E]/70 hover:text-[#10324E]'
                }`}
              >
                Official Letterhead View
              </button>
            </div>

            <button
              onClick={onClose}
              className="p-1.5 rounded-xl text-[#242826]/40 hover:bg-slate-100 hover:text-[#10324E] cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {successNotice && (
          <div className="mt-3 p-3 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center gap-2 animate-in fade-in">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{successNotice}</span>
          </div>
        )}

        {/* ── View Mode: Drug Editor ── */}
        {viewMode === 'edit' ? (
          <div className="overflow-y-auto py-4 space-y-5 text-xs flex-1 pr-1">
            
            {/* Quick Drug Formulary Autocomplete Box */}
            <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-2.5">
              <div className="flex items-center justify-between">
                <span className="font-extrabold text-[#10324E] text-[11px] uppercase tracking-wider flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-[#F49E78]" />
                  <span>Indian Veterinary Drug Formulary (Quick Add)</span>
                </span>
                <span className="text-[10px] text-[#242826]/50">Click any medication to insert into prescription</span>
              </div>

              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Search generic or brand (e.g. Monocef, Melonex, Bravecto, Clavam, Baytril)..."
                className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 text-xs focus:outline-none"
              />

              <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto">
                {filteredFormulary.slice(0, 10).map((drug, i) => (
                  <button
                    key={i}
                    type="button"
                    onClick={() => handleAddFromFormulary(drug)}
                    className="px-2.5 py-1 rounded-lg bg-white border border-[#10324E]/10 hover:border-[#2D8A7A] hover:bg-[#E0F2EB] text-[10px] font-bold text-[#10324E] transition cursor-pointer text-left flex items-center gap-1"
                  >
                    <Plus className="w-3 h-3 text-[#2D8A7A]" />
                    <span>{drug.name}</span>
                    <span className="text-[#242826]/40 text-[9px]">({drug.category})</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Prescribed Medications Table */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <h4 className="font-extrabold text-[#10324E] text-xs uppercase tracking-wider">
                  Prescription Drug Schedule ({medications.length} items)
                </h4>
                <button
                  type="button"
                  onClick={handleAddBlankRow}
                  className="px-3 py-1 rounded-xl bg-[#10324E] text-white text-[11px] font-bold flex items-center gap-1 cursor-pointer"
                >
                  <Plus className="w-3 h-3" />
                  <span>Add Custom Drug</span>
                </button>
              </div>

              <div className="space-y-2.5">
                {medications.map((med, idx) => (
                  <div key={idx} className="p-3.5 rounded-2xl bg-white border border-[#10324E]/10 shadow-2xs space-y-2 text-left">
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2 flex-1">
                        <span className="w-5 h-5 rounded-full bg-[#10324E] text-white text-[10px] font-bold flex items-center justify-center shrink-0">
                          {idx + 1}
                        </span>
                        <input
                          type="text"
                          required
                          value={med.name}
                          onChange={(e) => handleMedChange(idx, 'name', e.target.value)}
                          placeholder="Drug Name & Strength (e.g. Melonex Oral Suspension 1.5mg/ml)"
                          className="w-full p-2 rounded-xl bg-[#FAF8F5] border border-[#10324E]/10 font-bold text-[#10324E] text-xs focus:outline-none"
                        />
                      </div>
                      <button
                        type="button"
                        onClick={() => handleRemoveRow(idx)}
                        className="p-1.5 rounded-xl text-rose-500 hover:bg-rose-50 cursor-pointer"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>

                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-1">
                      <div>
                        <label className="block text-[9px] font-bold text-[#242826]/60 mb-0.5">Dosage</label>
                        <input
                          type="text"
                          value={med.dosage}
                          onChange={(e) => handleMedChange(idx, 'dosage', e.target.value)}
                          placeholder="e.g. 1.5 ml or 1 tab"
                          className="w-full p-1.5 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10 font-mono text-[11px]"
                        />
                      </div>

                      <div>
                        <label className="block text-[9px] font-bold text-[#242826]/60 mb-0.5">Frequency</label>
                        <select
                          value={med.frequency}
                          onChange={(e) => handleMedChange(idx, 'frequency', e.target.value)}
                          className="w-full p-1.5 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10 font-bold text-[10px]"
                        >
                          <option value="SID (Once daily)">SID (Once daily)</option>
                          <option value="BID (Twice daily)">BID (Twice daily)</option>
                          <option value="TID (Thrice daily)">TID (Thrice daily)</option>
                          <option value="QID (4 times daily)">QID (4 times daily)</option>
                          <option value="PRN (As needed)">PRN (As needed)</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-[9px] font-bold text-[#242826]/60 mb-0.5">Timing</label>
                        <select
                          value={med.timing}
                          onChange={(e) => handleMedChange(idx, 'timing', e.target.value)}
                          className="w-full p-1.5 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10 text-[10px]"
                        >
                          <option value="After food">After food</option>
                          <option value="Before food">Before food (Empty stomach)</option>
                          <option value="With food">With food</option>
                          <option value="Topical application">Topical application</option>
                        </select>
                      </div>

                      <div>
                        <label className="block text-[9px] font-bold text-[#242826]/60 mb-0.5">Duration</label>
                        <input
                          type="text"
                          value={med.duration}
                          onChange={(e) => handleMedChange(idx, 'duration', e.target.value)}
                          placeholder="e.g. 5 days, 14 days"
                          className="w-full p-1.5 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10 font-mono text-[11px]"
                        />
                      </div>
                    </div>

                    <div>
                      <input
                        type="text"
                        value={med.instructions}
                        onChange={(e) => handleMedChange(idx, 'instructions', e.target.value)}
                        placeholder="Special Instructions (e.g. Store in refrigerator, do not crush tablet, use e-collar)"
                        className="w-full p-1.5 rounded-lg bg-[#FAF8F5] border border-[#10324E]/10 text-[10px] text-[#242826]/80"
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* General Advice & Follow-Up */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 text-left">
              <div className="sm:col-span-2 space-y-1">
                <label className="block text-[11px] font-bold text-[#10324E]">Diet &amp; Nursing Advice</label>
                <input
                  type="text"
                  value={dietaryAdvice}
                  onChange={(e) => setDietaryAdvice(e.target.value)}
                  className="w-full p-2 rounded-xl bg-white border border-[#10324E]/10 text-xs"
                />
              </div>

              <div className="space-y-1">
                <label className="block text-[11px] font-bold text-[#10324E]">Follow-Up Visit In</label>
                <input
                  type="text"
                  value={`${followUpDays} days`}
                  onChange={(e) => setFollowUpDays(e.target.value.replace(/\D/g, ''))}
                  className="w-full p-2 rounded-xl bg-white border border-[#10324E]/10 text-xs font-mono font-bold"
                />
              </div>
            </div>

          </div>
        ) : (
          /* ── View Mode: Official Letterhead Preview (Printable Rx) ── */
          <div className="overflow-y-auto py-4 flex-1 pr-1">
            <div
              ref={printRef}
              className="bg-white p-6 sm:p-8 rounded-2xl border border-slate-300 shadow-sm text-left font-serif space-y-5 print:p-0 print:border-none print:shadow-none"
            >
              {/* Clinical Header */}
              <div className="flex items-start justify-between border-b-2 border-[#10324E] pb-4">
                <div>
                  <h2 className="text-xl font-bold text-[#10324E]">
                    Dr. {doctorProfile?.firstName || 'Priya'} {doctorProfile?.lastName || 'Sharma'}
                  </h2>
                  <p className="text-xs text-slate-600 font-sans">
                    {doctorProfile?.qualification || 'BVSc & AH, MVSc (Surgery)'}
                  </p>
                  <p className="text-xs font-mono font-bold text-[#2D8A7A]">
                    State Veterinary Council Reg: {doctorProfile?.registrationNo || 'MSVC-2024-8891'}
                  </p>
                </div>

                <div className="text-right font-sans">
                  <h3 className="font-bold text-sm text-[#10324E]">{hospitalDetails?.name || 'Vetalya Animal Clinic & OPD'}</h3>
                  <p className="text-[11px] text-slate-500 max-w-xs">{hospitalDetails?.address1 || '124 Indiranagar 100ft Road, Bengaluru, Karnataka - 560038'}</p>
                  <p className="text-[11px] text-slate-500">Ph: {hospitalDetails?.mobile || '+91 98765 43210'}</p>
                </div>
              </div>

              {/* Patient Banner */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 bg-[#FAF8F5] p-3 rounded-xl border border-slate-200 text-xs font-sans">
                <div>
                  <span className="text-slate-500 text-[10px] block">PATIENT COMPANION</span>
                  <span className="font-bold text-[#10324E] text-sm">{patient?.petName || 'Patient'}</span>
                </div>
                <div>
                  <span className="text-slate-500 text-[10px] block">SPECIES / BREED</span>
                  <span className="font-semibold">{patient?.species || 'Canine'} ({patient?.breed || 'Companion'})</span>
                </div>
                <div>
                  <span className="text-slate-500 text-[10px] block">PARENT / CLIENT</span>
                  <span className="font-semibold">{patient?.ownerName || 'Pet Parent'}</span>
                </div>
                <div>
                  <span className="text-slate-500 text-[10px] block">DATE / VITALS</span>
                  <span className="font-semibold">{new Date().toLocaleDateString('en-IN')} (Wt: {soapData?.weight || '—'}kg)</span>
                </div>
              </div>

              {/* Clinical Assessment */}
              <div className="text-xs font-sans pb-1">
                <strong>CLINICAL DIAGNOSIS: </strong>
                <span className="font-bold text-[#10324E]">{soapData?.primaryDiagnosis || 'Acute Clinical Consultation'}</span>
              </div>

              {/* Rx Prescribed Medicines Table */}
              <div className="space-y-1">
                <div className="text-lg font-bold text-[#10324E] font-serif">℞ Prescribed Medicines</div>
                <table className="w-full text-xs font-sans border-collapse">
                  <thead>
                    <tr className="border-b border-slate-300 text-slate-600 text-[10px] uppercase font-bold text-left">
                      <th className="py-2">No.</th>
                      <th className="py-2">Drug Name &amp; Strength</th>
                      <th className="py-2">Dosage</th>
                      <th className="py-2">Frequency</th>
                      <th className="py-2">Timing</th>
                      <th className="py-2">Duration</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {medications.map((m, idx) => (
                      <tr key={idx} className="py-2">
                        <td className="py-2 font-mono text-slate-400">{idx + 1}.</td>
                        <td className="py-2 font-bold text-[#10324E]">
                          <div>{m.name}</div>
                          <div className="text-[10px] text-slate-500 font-normal italic">{m.instructions}</div>
                        </td>
                        <td className="py-2 font-mono">{m.dosage}</td>
                        <td className="py-2 font-semibold text-emerald-800">{m.frequency}</td>
                        <td className="py-2">{m.timing}</td>
                        <td className="py-2 font-mono">{m.duration}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              {/* Advice */}
              <div className="bg-slate-50 p-3 rounded-xl border border-slate-200 text-xs font-sans space-y-1">
                <p><strong>Dietary &amp; Nursing Care:</strong> {dietaryAdvice}</p>
                <p><strong>Follow-Up:</strong> Review in {followUpDays} days or immediately if condition deteriorates.</p>
              </div>

              {/* Doctor Stamp & Signature */}
              <div className="pt-6 flex items-end justify-between font-sans border-t border-slate-200 text-xs">
                <div className="text-[10px] text-slate-400 max-w-sm">
                  Generated via Vetalya Electronic Veterinary Health Network. Statutory compliance under Veterinary Council of India regulations.
                </div>
                <div className="text-right">
                  <div className="w-24 h-12 border border-dashed border-slate-300 rounded-lg flex items-center justify-center text-[10px] text-slate-400 font-sans mb-1 ml-auto">
                    [Digital Signature]
                  </div>
                  <div className="font-bold text-[#10324E]">Dr. {doctorProfile?.firstName || 'Priya'} {doctorProfile?.lastName || 'Sharma'}</div>
                  <div className="text-[10px] text-slate-500">Reg No: {doctorProfile?.registrationNo || 'MSVC-8891'}</div>
                </div>
              </div>

            </div>
          </div>
        )}

        {/* Modal Bottom Actions */}
        <div className="pt-3 border-t border-[#10324E]/10 shrink-0 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={handlePrint}
              className="px-3.5 py-2 rounded-xl bg-white border border-[#10324E]/15 text-[#10324E] text-xs font-bold hover:bg-slate-50 flex items-center gap-1.5 cursor-pointer shadow-2xs"
            >
              <Printer className="w-3.5 h-3.5 text-[#10324E]" />
              <span>Print Rx</span>
            </button>

            <button
              type="button"
              onClick={handleSendWhatsApp}
              className="px-3.5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold flex items-center gap-1.5 cursor-pointer shadow-2xs"
            >
              <MessageCircle className="w-3.5 h-3.5" />
              <span>Share via WhatsApp</span>
            </button>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 sm:flex-none px-4 py-2.5 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] cursor-pointer"
            >
              Close
            </button>

            <button
              type="button"
              disabled={isFinalizing}
              onClick={handleComplete}
              className="flex-1 sm:flex-none px-5 py-2.5 rounded-xl bg-[#10324E] hover:bg-[#0c273e] text-white text-xs font-extrabold shadow-md flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50"
            >
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>{isFinalizing ? 'Sealing Rx...' : 'Sign, Seal & Save to Pet EHR'}</span>
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}


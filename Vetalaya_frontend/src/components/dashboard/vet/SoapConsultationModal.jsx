import React, { useState } from 'react';
import {
  FileText,
  Activity,
  Heart,
  Thermometer,
  Weight,
  Sparkles,
  AlertTriangle,
  Plus,
  Trash2,
  Send,
  X,
  Calendar,
  CheckCircle2,
  ShieldCheck,
  Stethoscope,
  Info
} from 'lucide-react';
import { COMMON_DIAGNOSES, DIAGNOSTIC_TESTS } from './data/vetFormulary';

export default function SoapConsultationModal({
  patient,
  doctorName,
  doctorRegNo,
  clinicName,
  onClose = () => {},
  onSaveSoap,
  onSave,
  onProceedToPrescription,
  onOpenRxBuilder
}) {
  const [soapData, setSoapData] = useState({
    // S: Subjective
    subjective: patient?.reason || '',
    appetite: 'Normal', // 'Normal' | 'Decreased' | 'Anorexic' | 'Increased'
    waterIntake: 'Normal',
    bowelMovement: 'Normal', // 'Normal' | 'Diarrhea' | 'Constipated' | 'Soft'
    duration: '2-3 days',

    // O: Objective (Vitals & Physical Exam)
    temperature: patient?.temp !== '—' ? patient?.temp : '101.5',
    weight: patient?.weight !== '—' ? patient?.weight.replace(' kg', '') : '12.5',
    heartRate: patient?.hr !== '—' ? patient?.hr : '95',
    respiratoryRate: '24',
    mucousMembrane: 'Pink (Normal)', // 'Pink (Normal)' | 'Pale' | 'Icteric' | 'Cyanotic' | 'Congested'
    crt: '< 2 seconds', // '< 2 seconds' | 'Prolonged (> 2s)'
    bodyConditionScore: '5/9 (Ideal)', // '1-3 (Underweight)' | '4-5 (Ideal)' | '6-7 (Overweight)' | '8-9 (Obese)'
    hydration: 'Adequate (< 5%)', // 'Adequate (< 5%)' | 'Mild (5-7%)' | 'Moderate (8-10%)' | 'Severe (> 10%)'
    objectiveExam: 'Alert and responsive. Thoracic auscultation clear bilaterally with no murmurs. Abdominal palpation non-painful.',

    // A: Assessment (Diagnosis)
    primaryDiagnosis: patient?.reason || 'Acute Clinical Gastroenteritis',
    differentialDiagnosis: 'Dietary Indiscretion, Viral enteritis, Parasitic infestation',
    isChronicAlert: false,
    chronicCondition: 'None reported',

    // P: Plan (Orders & Care Advice)
    selectedLabTests: ['Complete Blood Count (CBC) with Platelet Count'],
    dietaryAdvice: 'Bland gastrointestinal diet (boiled rice + chicken broth or veterinary GI kibble). Ensure free access to clean water.',
    followUpDays: '5',
    doctorNotes: 'Monitor hydration and appetite. If vomiting recurs >2 times in 24 hours, report to emergency triage.',
  });

  const [error, setError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Toggle lab test order
  const handleToggleLabTest = (testName) => {
    setSoapData((prev) => {
      const exists = prev.selectedLabTests.includes(testName);
      if (exists) {
        return { ...prev, selectedLabTests: prev.selectedLabTests.filter((t) => t !== testName) };
      } else {
        return { ...prev, selectedLabTests: [...prev.selectedLabTests, testName] };
      }
    });
  };

  const handleValidateAndSave = (proceedToRx = false) => {
    setError(null);

    if (!soapData.primaryDiagnosis.trim()) {
      setError('Primary Assessment / Clinical Diagnosis is required.');
      return;
    }

    if (soapData.temperature) {
      const tempVal = parseFloat(soapData.temperature);
      if (isNaN(tempVal) || tempVal < 90 || tempVal > 112) {
        setError('Body temperature must be between 90°F and 112°F.');
        return;
      }
    }

    if (soapData.weight) {
      const wtVal = parseFloat(soapData.weight);
      if (isNaN(wtVal) || wtVal <= 0 || wtVal > 300) {
        setError('Patient weight must be a realistic positive number in kilograms.');
        return;
      }
    }

    setSubmitting(true);
    try {
      const saveFn = onSaveSoap || onSave || (() => {});
      const rxFn = onProceedToPrescription || onOpenRxBuilder || (() => {});
      if (proceedToRx) {
        rxFn(soapData);
      } else {
        saveFn(soapData);
      }
    } catch (err) {
      setError(err.message || 'Failed to save clinical consultation.');
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-2 sm:p-4 overflow-y-auto">
      <div className="bg-white rounded-[32px] max-w-4xl w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 my-auto text-left max-h-[94vh] flex flex-col animate-in zoom-in-95">
        
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/10 shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-2xl bg-[#E8EEF4] text-[#10324E] flex items-center justify-center font-extrabold text-sm">
              {patient?.token || 'T-01'}
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-lg font-black text-[#10324E]">
                  SOAP Case Record: {patient?.petName || 'Patient Companion'}
                </h3>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
                  Active Consultation
                </span>
              </div>
              <p className="text-xs text-[#242826]/70">
                {patient?.species} • {patient?.breed} • Parent: <strong>{patient?.ownerName}</strong> ({patient?.ownerPhone})
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl text-[#242826]/50 hover:bg-slate-100 hover:text-[#10324E] cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form Body */}
        <div className="overflow-y-auto py-4 space-y-5 text-xs flex-1 pr-1">
          {error && (
            <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold flex items-center justify-between">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0" />
                <span>{error}</span>
              </div>
              <button onClick={() => setError(null)} className="text-rose-600 font-extrabold">✕</button>
            </div>
          )}

          {/* ── SECTION S: SUBJECTIVE ── */}
          <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-3">
            <div className="flex items-center justify-between">
              <div className="font-black text-[#10324E] uppercase tracking-wider text-[11px] flex items-center gap-2">
                <span className="w-5 h-5 rounded-md bg-[#10324E] text-white flex items-center justify-center text-[10px] font-black">S</span>
                <span>Subjective (Owner Observations &amp; History)</span>
              </div>
              <span className="text-[10px] text-[#242826]/50">Duration: {soapData.duration}</span>
            </div>

            <div>
              <label className="block text-[11px] font-bold text-[#10324E] mb-1">Chief Complaints &amp; Clinical History</label>
              <textarea
                rows="2"
                value={soapData.subjective}
                onChange={(e) => setSoapData({ ...soapData, subjective: e.target.value })}
                placeholder="Owner complaints, onset, frequency of episodes, diet changes..."
                className="w-full p-2.5 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#10324E] focus:outline-none focus:ring-1 focus:ring-[#10324E]"
              />
            </div>

            <div className="grid grid-cols-3 gap-2.5 pt-1">
              <div>
                <label className="block text-[10px] font-bold text-[#242826]/70 mb-1">Appetite</label>
                <select
                  value={soapData.appetite}
                  onChange={(e) => setSoapData({ ...soapData, appetite: e.target.value })}
                  className="w-full p-1.5 rounded-xl bg-white border border-[#10324E]/10 text-xs font-bold"
                >
                  <option value="Normal">Normal</option>
                  <option value="Decreased">Decreased / Hyporexic</option>
                  <option value="Anorexic">Completely Anorexic</option>
                  <option value="Increased">Polyphagic (Increased)</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-[#242826]/70 mb-1">Water Intake</label>
                <select
                  value={soapData.waterIntake}
                  onChange={(e) => setSoapData({ ...soapData, waterIntake: e.target.value })}
                  className="w-full p-1.5 rounded-xl bg-white border border-[#10324E]/10 text-xs font-bold"
                >
                  <option value="Normal">Normal</option>
                  <option value="Polydipsia">Polydipsia (Increased)</option>
                  <option value="Decreased">Decreased Intake</option>
                  <option value="Adipsia">Refusing Water</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-[#242826]/70 mb-1">Bowel Habits</label>
                <select
                  value={soapData.bowelMovement}
                  onChange={(e) => setSoapData({ ...soapData, bowelMovement: e.target.value })}
                  className="w-full p-1.5 rounded-xl bg-white border border-[#10324E]/10 text-xs font-bold"
                >
                  <option value="Normal">Normal Stool</option>
                  <option value="Soft / Mucoid">Soft / Mucoid</option>
                  <option value="Watery Diarrhea">Watery Diarrhea</option>
                  <option value="Hemorrhagic Enteritis">Hemorrhagic (Blood)</option>
                  <option value="Constipated">Constipated</option>
                </select>
              </div>
            </div>
          </div>

          {/* ── SECTION O: OBJECTIVE (Vitals & Physical Exam) ── */}
          <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-3">
            <div className="font-black text-[#10324E] uppercase tracking-wider text-[11px] flex items-center gap-2">
              <span className="w-5 h-5 rounded-md bg-[#2D8A7A] text-white flex items-center justify-center text-[10px] font-black">O</span>
              <span>Objective (Vital Signs &amp; Systematic Physical Examination)</span>
            </div>

            {/* Vitals Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
              <div className="p-2.5 rounded-xl bg-white border border-[#10324E]/10 text-left">
                <span className="text-[10px] font-bold text-[#242826]/60 flex items-center gap-1">
                  <Thermometer className="w-3 h-3 text-rose-500" />
                  <span>Temp (°F)</span>
                </span>
                <input
                  type="text"
                  value={soapData.temperature}
                  onChange={(e) => setSoapData({ ...soapData, temperature: e.target.value })}
                  className="w-full text-base font-black font-mono text-[#10324E] mt-0.5 focus:outline-none"
                />
                <span className="text-[9px] text-[#242826]/50">Norm: 100.5–102.5°F</span>
              </div>

              <div className="p-2.5 rounded-xl bg-white border border-[#10324E]/10 text-left">
                <span className="text-[10px] font-bold text-[#242826]/60 flex items-center gap-1">
                  <Weight className="w-3 h-3 text-[#10324E]" />
                  <span>Weight (kg)</span>
                </span>
                <input
                  type="text"
                  value={soapData.weight}
                  onChange={(e) => setSoapData({ ...soapData, weight: e.target.value })}
                  className="w-full text-base font-black font-mono text-[#10324E] mt-0.5 focus:outline-none"
                />
                <span className="text-[9px] text-[#242826]/50">Required for Rx dosage</span>
              </div>

              <div className="p-2.5 rounded-xl bg-white border border-[#10324E]/10 text-left">
                <span className="text-[10px] font-bold text-[#242826]/60 flex items-center gap-1">
                  <Heart className="w-3 h-3 text-red-500" />
                  <span>Heart Rate</span>
                </span>
                <input
                  type="text"
                  value={soapData.heartRate}
                  onChange={(e) => setSoapData({ ...soapData, heartRate: e.target.value })}
                  className="w-full text-base font-black font-mono text-[#10324E] mt-0.5 focus:outline-none"
                />
                <span className="text-[9px] text-[#242826]/50">bpm (Norm: 70–140)</span>
              </div>

              <div className="p-2.5 rounded-xl bg-white border border-[#10324E]/10 text-left">
                <span className="text-[10px] font-bold text-[#242826]/60 flex items-center gap-1">
                  <Activity className="w-3 h-3 text-[#2D8A7A]" />
                  <span>Respiration Rate</span>
                </span>
                <input
                  type="text"
                  value={soapData.respiratoryRate}
                  onChange={(e) => setSoapData({ ...soapData, respiratoryRate: e.target.value })}
                  className="w-full text-base font-black font-mono text-[#10324E] mt-0.5 focus:outline-none"
                />
                <span className="text-[9px] text-[#242826]/50">breaths/min (Norm: 18–34)</span>
              </div>
            </div>

            {/* Mucous Membrane & BCS */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 pt-1">
              <div>
                <label className="block text-[10px] font-bold text-[#242826]/70 mb-1">Mucous Membrane</label>
                <select
                  value={soapData.mucousMembrane}
                  onChange={(e) => setSoapData({ ...soapData, mucousMembrane: e.target.value })}
                  className="w-full p-1.5 rounded-xl bg-white border border-[#10324E]/10 text-[11px] font-bold"
                >
                  <option value="Pink (Normal)">Pink (Normal)</option>
                  <option value="Pale (Anemic)">Pale (Anemic / Shock)</option>
                  <option value="Icteric (Jaundice)">Icteric (Jaundice / Liver)</option>
                  <option value="Cyanotic (Hypoxic)">Cyanotic (Hypoxic / Cyanosis)</option>
                  <option value="Congested (Hyperemic)">Congested (Sepsis)</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-[#242826]/70 mb-1">Capillary Refill (CRT)</label>
                <select
                  value={soapData.crt}
                  onChange={(e) => setSoapData({ ...soapData, crt: e.target.value })}
                  className="w-full p-1.5 rounded-xl bg-white border border-[#10324E]/10 text-[11px] font-bold"
                >
                  <option value="< 2 seconds">&lt; 2 seconds (Normal)</option>
                  <option value="Prolonged (> 2s)">Prolonged (&gt; 2s - Dehydrated)</option>
                  <option value="Rapid (< 1s)">Rapid (&lt; 1s - Hyperdynamic)</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-[#242826]/70 mb-1">BCS (Body Score)</label>
                <select
                  value={soapData.bodyConditionScore}
                  onChange={(e) => setSoapData({ ...soapData, bodyConditionScore: e.target.value })}
                  className="w-full p-1.5 rounded-xl bg-white border border-[#10324E]/10 text-[11px] font-bold"
                >
                  <option value="1-3 (Underweight)">1-3 (Underweight / Cachexic)</option>
                  <option value="4-5 (Ideal)">4-5 (Ideal Weight)</option>
                  <option value="6-7 (Overweight)">6-7 (Overweight)</option>
                  <option value="8-9 (Obese)">8-9 (Obese / Heavy)</option>
                </select>
              </div>

              <div>
                <label className="block text-[10px] font-bold text-[#242826]/70 mb-1">Hydration Status</label>
                <select
                  value={soapData.hydration}
                  onChange={(e) => setSoapData({ ...soapData, hydration: e.target.value })}
                  className="w-full p-1.5 rounded-xl bg-white border border-[#10324E]/10 text-[11px] font-bold"
                >
                  <option value="Adequate (< 5%)">Adequate (&lt; 5%)</option>
                  <option value="Mild (5-7%)">Mild (5-7% - Tacky gums)</option>
                  <option value="Moderate (8-10%)">Moderate (8-10% - Skin tent)</option>
                  <option value="Severe (> 10%)">Severe (&gt; 10% - Enophthalmos)</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-bold text-[#10324E] mb-1">Systemic Examination Findings</label>
              <textarea
                rows="2"
                value={soapData.objectiveExam}
                onChange={(e) => setSoapData({ ...soapData, objectiveExam: e.target.value })}
                placeholder="Physical palpation, lymph nodes, lung auscultation, eyes, ears, skin lesions..."
                className="w-full p-2.5 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#10324E]"
              />
            </div>
          </div>

          {/* ── SECTION A: ASSESSMENT (Diagnosis & Chronic Alert) ── */}
          <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 space-y-3">
            <div className="font-black text-[#10324E] uppercase tracking-wider text-[11px] flex items-center gap-2">
              <span className="w-5 h-5 rounded-md bg-amber-600 text-white flex items-center justify-center text-[10px] font-black">A</span>
              <span>Assessment &amp; Clinical Diagnosis</span>
            </div>

            <div>
              <label className="block text-[11px] font-bold text-[#10324E] mb-1">Primary Diagnosis *</label>
              <div className="relative">
                <input
                  type="text"
                  required
                  value={soapData.primaryDiagnosis}
                  onChange={(e) => setSoapData({ ...soapData, primaryDiagnosis: e.target.value })}
                  placeholder="e.g. Acute Canine Gastroenteritis, Canine Parvovirus, Atopic Dermatitis"
                  className="w-full p-2.5 rounded-xl bg-white border border-[#10324E]/20 text-xs font-bold text-[#10324E] focus:outline-none"
                />
              </div>

              {/* Autocomplete Quick Pills */}
              <div className="flex flex-wrap items-center gap-1.5 mt-2">
                <span className="text-[10px] text-[#242826]/50">Quick common diagnoses:</span>
                {COMMON_DIAGNOSES.slice(0, 5).map((diag, i) => (
                  <button
                    type="button"
                    key={i}
                    onClick={() => setSoapData({ ...soapData, primaryDiagnosis: diag })}
                    className="text-[10px] font-bold px-2 py-0.5 rounded-lg bg-white border border-[#10324E]/10 hover:bg-[#E8EEF4] transition cursor-pointer text-[#10324E]"
                  >
                    + {diag.split('(')[0].trim()}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-bold text-[#10324E] mb-1">Differential Diagnoses (Rule Outs)</label>
              <input
                type="text"
                value={soapData.differentialDiagnosis}
                onChange={(e) => setSoapData({ ...soapData, differentialDiagnosis: e.target.value })}
                placeholder="Alternative possibilities to investigate"
                className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#242826]"
              />
            </div>

            {/* Chronic Conditions Alert Toggle */}
            <div className="p-3 rounded-xl bg-amber-50/70 border border-amber-200 space-y-2">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={soapData.isChronicAlert}
                  onChange={(e) => setSoapData({ ...soapData, isChronicAlert: e.target.checked })}
                  className="rounded border-amber-400 text-amber-600 focus:ring-0"
                />
                <span className="text-xs font-bold text-amber-900">
                  Flag as Chronic Disease / High Risk Patient (e.g. Renal Failure, Epilepsy, Cardiac Murmur)
                </span>
              </label>
              {soapData.isChronicAlert && (
                <input
                  type="text"
                  value={soapData.chronicCondition}
                  onChange={(e) => setSoapData({ ...soapData, chronicCondition: e.target.value })}
                  placeholder="Specify chronic condition details (e.g. Stage II CKD, on Phenobarbitone)"
                  className="w-full p-2 rounded-lg bg-white border border-amber-300 text-xs font-bold text-amber-900"
                />
              )}
            </div>
          </div>

          {/* ── SECTION P: PLAN (Lab Orders & Instructions) ── */}
          <div className="p-4 rounded-2xl bg-[#E0F2EB]/40 border border-[#2D8A7A]/25 space-y-3">
            <div className="font-black text-[#10324E] uppercase tracking-wider text-[11px] flex items-center gap-2">
              <span className="w-5 h-5 rounded-md bg-[#2D8A7A] text-white flex items-center justify-center text-[10px] font-black">P</span>
              <span>Plan (Diagnostic Investigations, Diet &amp; Follow-up)</span>
            </div>

            {/* Lab Orders Multi-Selector */}
            <div>
              <label className="block text-[11px] font-bold text-[#10324E] mb-1.5 flex items-center justify-between">
                <span>Diagnostic Lab Orders &amp; Imaging</span>
                <span className="text-[10px] text-[#2D8A7A] font-extrabold">{soapData.selectedLabTests.length} tests ordered</span>
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5 max-h-36 overflow-y-auto p-2 bg-white rounded-xl border border-[#10324E]/10">
                {DIAGNOSTIC_TESTS.map((test, idx) => {
                  const isChecked = soapData.selectedLabTests.includes(test.name);
                  return (
                    <label
                      key={idx}
                      className={`flex items-start gap-2 p-1.5 rounded-lg transition cursor-pointer text-[11px] ${
                        isChecked ? 'bg-[#E0F2EB] text-[#10324E] font-bold' : 'hover:bg-slate-50 text-[#242826]/80'
                      }`}
                    >
                      <input
                        type="checkbox"
                        checked={isChecked}
                        onChange={() => handleToggleLabTest(test.name)}
                        className="mt-0.5 rounded border-slate-300 text-[#2D8A7A] focus:ring-0"
                      />
                      <span className="truncate">{test.name}</span>
                    </label>
                  );
                })}
              </div>
            </div>

            {/* Dietary Advice & Follow-up */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
              <div className="sm:col-span-2">
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Dietary &amp; Nursing Care Advice</label>
                <input
                  type="text"
                  value={soapData.dietaryAdvice}
                  onChange={(e) => setSoapData({ ...soapData, dietaryAdvice: e.target.value })}
                  placeholder="e.g. Bland diet, electrolytes, restrict jumping"
                  className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#10324E]"
                />
              </div>

              <div>
                <label className="block text-[11px] font-bold text-[#10324E] mb-1">Follow-Up (Days)</label>
                <div className="relative">
                  <Calendar className="w-3.5 h-3.5 absolute left-3 top-2.5 text-[#242826]/40" />
                  <input
                    type="number"
                    min="1"
                    max="60"
                    value={soapData.followUpDays}
                    onChange={(e) => setSoapData({ ...soapData, followUpDays: e.target.value })}
                    className="w-full pl-8 pr-3 py-1.5 rounded-xl bg-white border border-[#10324E]/15 text-xs font-mono font-bold"
                  />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-bold text-[#10324E] mb-1">Doctor's Private Clinical Notes (Confidential)</label>
              <input
                type="text"
                value={soapData.doctorNotes}
                onChange={(e) => setSoapData({ ...soapData, doctorNotes: e.target.value })}
                placeholder="Prognosis, guarded recovery, client compliance..."
                className="w-full p-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#242826]/80"
              />
            </div>
          </div>
        </div>

        {/* Modal Footer Actions */}
        <div className="pt-3 border-t border-[#10324E]/10 shrink-0 flex flex-col sm:flex-row items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-[11px] text-[#242826]/70">
            <ShieldCheck className="w-4 h-4 text-[#2D8A7A]" />
            <span>Digital seal by <strong>{doctorName}</strong> ({doctorRegNo || 'VCI Verified'})</span>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 sm:flex-none px-4 py-2.5 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] hover:bg-slate-50 cursor-pointer"
            >
              Cancel
            </button>

            <button
              type="button"
              disabled={submitting}
              onClick={() => handleValidateAndSave(false)}
              className="flex-1 sm:flex-none px-4 py-2.5 rounded-xl bg-white border border-[#10324E]/20 text-[#10324E] text-xs font-bold hover:bg-slate-50 cursor-pointer"
            >
              Save SOAP Only
            </button>

            <button
              type="button"
              disabled={submitting}
              onClick={() => handleValidateAndSave(true)}
              className="flex-1 sm:flex-none px-5 py-2.5 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-extrabold shadow-md flex items-center justify-center gap-1.5 cursor-pointer disabled:opacity-50"
            >
              <FileText className="w-3.5 h-3.5" />
              <span>Proceed to Rx Builder →</span>
            </button>
          </div>
        </div>

      </div>
    </div>
  );
}


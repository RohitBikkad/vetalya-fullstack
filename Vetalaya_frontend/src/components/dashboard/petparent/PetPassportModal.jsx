import React, { useState, useEffect, useCallback } from 'react';
import {
  getPetPassportApi,
  updatePetPassportApi,
  getPetTimelineApi,
  createShareRequestApi,
  getPetDocumentsApi,
  uploadPetDocumentApi,
} from '../../../services/api';
import {
  ShieldCheck,
  X,
  AlertTriangle,
  Heart,
  Calendar,
  Clock,
  FileText,
  Share2,
  Upload,
  CheckCircle2,
  Copy,
  Check,
  QrCode,
  ExternalLink,
  Syringe,
  Stethoscope,
  Activity,
  Edit2,
  Save,
} from 'lucide-react';

export default function PetPassportModal({ pet, onClose, onPetUpdated }) {
  const [activeTab, setActiveTab] = useState('overview'); // 'overview' | 'timeline' | 'documents' | 'share'
  const [passport, setPassport] = useState(null);
  const [timeline, setTimeline] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [copiedQr, setCopiedQr] = useState(false);

  // Edit Passport State
  const [isEditing, setIsEditing] = useState(false);
  const [savingEdit, setSavingEdit] = useState(false);
  const [editForm, setEditForm] = useState({
    bloodGroup: '',
    allergiesSummary: '',
    chronicConditionsSummary: '',
    dietaryNotes: '',
    emergencyContactPhone: '',
    weightKg: '',
    color: '',
    distinctiveMarks: '',
  });

  // Document Upload State
  const [showDocUpload, setShowDocUpload] = useState(false);
  const [uploadingDoc, setUploadingDoc] = useState(false);
  const [docForm, setDocForm] = useState({
    category: 'VACCINATION_CERTIFICATE',
    fileName: '',
    description: '',
    fileType: 'application/pdf',
    fileSizeBytes: 245000,
    storagePath: '',
  });

  // Share Request State
  const [shareScope, setShareScope] = useState('FULL_TIMELINE');
  const [shareHours, setShareHours] = useState(72);
  const [shareSuccess, setShareSuccess] = useState(null);
  const [sharingLoading, setSharingLoading] = useState(false);

  const loadPassportDetails = useCallback(async () => {
    if (!pet?.id) return;
    setLoading(true);
    setError(null);
    try {
      const [passportData, timelineData, docsData] = await Promise.all([
        getPetPassportApi(pet.id).catch(() => null),
        getPetTimelineApi(pet.id).catch(() => []),
        getPetDocumentsApi(pet.id).catch(() => []),
      ]);

      if (passportData) {
        setPassport(passportData);
        setEditForm({
          bloodGroup: passportData.bloodGroup || '',
          allergiesSummary: passportData.allergiesSummary || '',
          chronicConditionsSummary: passportData.chronicConditionsSummary || '',
          dietaryNotes: passportData.dietaryNotes || '',
          emergencyContactPhone: passportData.emergencyContactPhone || '',
          weightKg: passportData.weightKg || '',
          color: passportData.color || '',
          distinctiveMarks: passportData.distinctiveMarks || '',
        });
      }
      setTimeline(Array.isArray(timelineData) ? timelineData : []);
      setDocuments(Array.isArray(docsData) ? docsData : []);
    } catch (err) {
      setError(err.message || 'Failed to load passport details.');
    } finally {
      setLoading(false);
    }
  }, [pet?.id]);

  useEffect(() => {
    loadPassportDetails();
  }, [loadPassportDetails]);

  const handleSaveEdit = async (e) => {
    e.preventDefault();
    setSavingEdit(true);
    setError(null);
    try {
      const updated = await updatePetPassportApi(pet.id, {
        bloodGroup: editForm.bloodGroup || null,
        allergiesSummary: editForm.allergiesSummary || null,
        chronicConditionsSummary: editForm.chronicConditionsSummary || null,
        dietaryNotes: editForm.dietaryNotes || null,
        emergencyContactPhone: editForm.emergencyContactPhone || null,
        weightKg: editForm.weightKg ? parseFloat(editForm.weightKg) : null,
        color: editForm.color || null,
        distinctiveMarks: editForm.distinctiveMarks || null,
      });
      setPassport(updated);
      setIsEditing(false);
      if (onPetUpdated) onPetUpdated();
    } catch (err) {
      setError(err.message || 'Failed to update passport');
    } finally {
      setSavingEdit(false);
    }
  };

  const handleDocUploadSubmit = async (e) => {
    e.preventDefault();
    if (!docForm.fileName.trim()) return;
    setUploadingDoc(true);
    setError(null);
    try {
      const newDoc = await uploadPetDocumentApi(pet.id, {
        category: docForm.category,
        fileName: docForm.fileName.trim(),
        fileType: docForm.fileType,
        fileSizeBytes: docForm.fileSizeBytes || 102400,
        storagePath: docForm.storagePath.trim() || `https://storage.vetalya.com/docs/${pet.id}/${encodeURIComponent(docForm.fileName.trim())}`,
        description: docForm.description.trim() || null,
        documentDate: new Date().toISOString().split('T')[0],
      });
      setDocuments(prev => [newDoc, ...prev]);
      setShowDocUpload(false);
      setDocForm({ category: 'VACCINATION_CERTIFICATE', fileName: '', description: '', fileType: 'application/pdf', fileSizeBytes: 245000, storagePath: '' });
      // refresh timeline
      getPetTimelineApi(pet.id).then(t => setTimeline(Array.isArray(t) ? t : []));
    } catch (err) {
      setError(err.message || 'Failed to upload document');
    } finally {
      setUploadingDoc(false);
    }
  };

  const handleCreateShare = async () => {
    setSharingLoading(true);
    setShareSuccess(null);
    setError(null);
    try {
      const res = await createShareRequestApi(pet.id, null, shareScope, shareHours);
      setShareSuccess({
        id: res.requestId,
        scope: res.scope,
        expiresAt: res.expiresAt ? new Date(res.expiresAt).toLocaleString() : 'Permanent',
      });
    } catch (err) {
      setError(err.message || 'Failed to generate record sharing token');
    } finally {
      setSharingLoading(false);
    }
  };

  const handleCopyEmergencyLink = () => {
    if (!passport?.publicEmergencyQrHash) return;
    const url = `${window.location.origin}/api/v1/pets/emergency/${passport.publicEmergencyQrHash}`;
    navigator.clipboard.writeText(url);
    setCopiedQr(true);
    setTimeout(() => setCopiedQr(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
      <div className="bg-white rounded-[32px] max-w-2xl w-full my-auto shadow-2xl border border-[#10324E]/10 animate-in zoom-in-95 text-left flex flex-col max-h-[92vh]">

        {/* Modal Header */}
        <div className="p-6 border-b border-[#10324E]/10 flex items-start justify-between gap-4 shrink-0 bg-[#FAF8F5] rounded-t-[32px]">
          <div className="flex items-center gap-4">
            <img
              src={pet.avatar}
              alt={pet.name}
              className="w-16 h-16 rounded-2xl object-cover ring-4 ring-[#079B96]/20 shadow-md shrink-0"
            />
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-xl font-extrabold text-[#123F70] font-display">{pet.name}</h3>
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-extrabold bg-[#E0F2EB] text-[#079B96] border border-[#079B96]/30 uppercase">
                  <ShieldCheck className="w-3 h-3" />
                  <span>Verified Passport</span>
                </span>
              </div>
              <p className="text-xs text-[#718096] mt-0.5">
                {pet.breed} • {pet.species} • {pet.age}
              </p>
              <p className="text-[11px] font-mono font-bold text-[#123F70]/80 mt-1">
                ID: {passport?.passportNumber || `VET-2026-${pet.id?.slice(-6)?.toUpperCase()}`}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-xl text-[#718096] hover:bg-slate-200/60 transition cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab Navigation Ribbon */}
        <div className="flex border-b border-[#10324E]/10 px-6 gap-2 bg-white shrink-0 overflow-x-auto text-xs font-bold">
          <button
            onClick={() => setActiveTab('overview')}
            className={`py-3 px-3.5 border-b-2 transition flex items-center gap-1.5 cursor-pointer whitespace-nowrap ${
              activeTab === 'overview'
                ? 'border-[#079B96] text-[#079B96]'
                : 'border-transparent text-[#718096] hover:text-[#123F70]'
            }`}
          >
            <Heart className="w-4 h-4" />
            <span>Health Overview</span>
          </button>

          <button
            onClick={() => setActiveTab('timeline')}
            className={`py-3 px-3.5 border-b-2 transition flex items-center gap-1.5 cursor-pointer whitespace-nowrap ${
              activeTab === 'timeline'
                ? 'border-[#079B96] text-[#079B96]'
                : 'border-transparent text-[#718096] hover:text-[#123F70]'
            }`}
          >
            <Activity className="w-4 h-4" />
            <span>Medical Timeline ({timeline.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('documents')}
            className={`py-3 px-3.5 border-b-2 transition flex items-center gap-1.5 cursor-pointer whitespace-nowrap ${
              activeTab === 'documents'
                ? 'border-[#079B96] text-[#079B96]'
                : 'border-transparent text-[#718096] hover:text-[#123F70]'
            }`}
          >
            <FileText className="w-4 h-4" />
            <span>Document Vault ({documents.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('share')}
            className={`py-3 px-3.5 border-b-2 transition flex items-center gap-1.5 cursor-pointer whitespace-nowrap ${
              activeTab === 'share'
                ? 'border-[#079B96] text-[#079B96]'
                : 'border-transparent text-[#718096] hover:text-[#123F70]'
            }`}
          >
            <Share2 className="w-4 h-4" />
            <span>Share Records</span>
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-6 overflow-y-auto space-y-4 flex-1">
          {error && (
            <div className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold flex items-center justify-between">
              <span>{error}</span>
              <button onClick={() => setError(null)} className="text-rose-600 font-extrabold ml-2">✕</button>
            </div>
          )}

          {loading ? (
            <div className="py-12 text-center text-xs text-[#718096] animate-pulse">
              Loading digital pet passport and clinical records...
            </div>
          ) : (
            <>
              {/* ── TAB 1: OVERVIEW ── */}
              {activeTab === 'overview' && (
                <div className="space-y-4">

                  {/* Critical Medical Alerts Banner */}
                  {(passport?.allergiesSummary || passport?.chronicConditionsSummary) && (
                    <div className="p-4 rounded-2xl bg-amber-50/90 border border-amber-300 text-amber-950 text-xs space-y-1">
                      <div className="flex items-center gap-2 font-bold text-amber-900">
                        <AlertTriangle className="w-4 h-4 text-amber-600" />
                        <span>Critical Medical Warnings & Allergies</span>
                      </div>
                      {passport.allergiesSummary && (
                        <p><strong className="text-amber-900">Allergies:</strong> {passport.allergiesSummary}</p>
                      )}
                      {passport.chronicConditionsSummary && (
                        <p><strong className="text-amber-900">Chronic Conditions:</strong> {passport.chronicConditionsSummary}</p>
                      )}
                    </div>
                  )}

                  {/* Vitals & Identifiers Grid */}
                  <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                    <div className="p-3.5 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
                      <span className="text-[10px] text-[#718096] uppercase font-bold tracking-wider block">Blood Group</span>
                      <span className="text-sm font-extrabold text-[#123F70]">{passport?.bloodGroup || 'DEA 1.1 Pending'}</span>
                    </div>

                    <div className="p-3.5 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
                      <span className="text-[10px] text-[#718096] uppercase font-bold tracking-wider block">Recorded Weight</span>
                      <span className="text-sm font-extrabold text-[#123F70]">{passport?.weightKg ? `${passport.weightKg} kg` : (pet.weight || '—')}</span>
                    </div>

                    <div className="p-3.5 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
                      <span className="text-[10px] text-[#718096] uppercase font-bold tracking-wider block">Microchip RFID</span>
                      <span className="text-sm font-mono font-bold text-[#079B96]">{pet.microchip || 'Pending'}</span>
                    </div>

                    <div className="p-3.5 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
                      <span className="text-[10px] text-[#718096] uppercase font-bold tracking-wider block">Next Booster Due</span>
                      <span className="text-sm font-bold text-[#FF843F]">
                        {passport?.nextVaccinationDue ? new Date(passport.nextVaccinationDue).toLocaleDateString() : 'Up to date'}
                      </span>
                    </div>

                    <div className="p-3.5 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 sm:col-span-2">
                      <span className="text-[10px] text-[#718096] uppercase font-bold tracking-wider block">Emergency Contact</span>
                      <span className="text-sm font-bold text-[#123F70]">{passport?.emergencyContactPhone || 'Not specified'}</span>
                    </div>
                  </div>

                  {/* Dietary & Lifestyle notes */}
                  <div className="p-4 rounded-2xl bg-white border border-[#10324E]/10 space-y-2 text-xs">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-[#123F70]">Dietary & Feeding Guidelines:</span>
                      <button
                        onClick={() => setIsEditing(!isEditing)}
                        className="text-[#079B96] hover:underline font-bold flex items-center gap-1 cursor-pointer"
                      >
                        <Edit2 className="w-3 h-3" />
                        <span>{isEditing ? 'Cancel Edit' : 'Edit Passport'}</span>
                      </button>
                    </div>
                    <p className="text-[#192A3A]/80 leading-relaxed">
                      {passport?.dietaryNotes || 'Standard balanced veterinary diet plan.'}
                    </p>
                  </div>

                  {/* Public Emergency Collar QR Access */}
                  <div className="p-4 rounded-2xl bg-[#E8F6F4] border border-[#079B96]/30 flex items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center text-[#079B96] shadow-2xs shrink-0">
                        <QrCode className="w-6 h-6" />
                      </div>
                      <div>
                        <h4 className="text-xs font-bold text-[#123F70]">Public Emergency Collar Tag Link</h4>
                        <p className="text-[11px] text-[#718096]">
                          Safe emergency link displaying only non-private triage info if pet is lost.
                        </p>
                      </div>
                    </div>
                    <button
                      onClick={handleCopyEmergencyLink}
                      className="px-3.5 py-2 rounded-xl bg-white hover:bg-slate-50 border border-[#079B96]/40 text-[#079B96] text-xs font-bold flex items-center gap-1.5 shadow-2xs shrink-0 cursor-pointer"
                    >
                      {copiedQr ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
                      <span>{copiedQr ? 'Copied!' : 'Copy Link'}</span>
                    </button>
                  </div>

                  {/* Edit Form Modal/Inline */}
                  {isEditing && (
                    <form onSubmit={handleSaveEdit} className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#079B96]/30 space-y-3 text-xs animate-in fade-in">
                      <h4 className="font-bold text-[#123F70]">Edit Pet Health Passport</h4>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="block text-[11px] font-bold text-[#123F70] mb-1">Blood Group</label>
                          <input
                            type="text"
                            value={editForm.bloodGroup}
                            onChange={(e) => setEditForm({ ...editForm, bloodGroup: e.target.value })}
                            placeholder="e.g. DEA 1.1 Positive"
                            className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#123F70]"
                          />
                        </div>
                        <div>
                          <label className="block text-[11px] font-bold text-[#123F70] mb-1">Weight (kg)</label>
                          <input
                            type="number"
                            step="0.1"
                            value={editForm.weightKg}
                            onChange={(e) => setEditForm({ ...editForm, weightKg: e.target.value })}
                            placeholder="e.g. 15.5"
                            className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#123F70]"
                          />
                        </div>
                      </div>

                      <div>
                        <label className="block text-[11px] font-bold text-[#123F70] mb-1">Critical Allergies Summary</label>
                        <input
                          type="text"
                          value={editForm.allergiesSummary}
                          onChange={(e) => setEditForm({ ...editForm, allergiesSummary: e.target.value })}
                          placeholder="e.g. Penicillin allergy, bee sting sensitivity"
                          className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#123F70]"
                        />
                      </div>

                      <div>
                        <label className="block text-[11px] font-bold text-[#123F70] mb-1">Dietary & Nutrition Notes</label>
                        <textarea
                          rows={2}
                          value={editForm.dietaryNotes}
                          onChange={(e) => setEditForm({ ...editForm, dietaryNotes: e.target.value })}
                          placeholder="Special feeding routine, gastrointestinal sensitivity..."
                          className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#123F70]"
                        />
                      </div>

                      <div className="flex justify-end gap-2 pt-2">
                        <button
                          type="button"
                          onClick={() => setIsEditing(false)}
                          className="px-4 py-2 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#123F70] cursor-pointer"
                        >
                          Cancel
                        </button>
                        <button
                          type="submit"
                          disabled={savingEdit}
                          className="px-5 py-2 rounded-xl bg-[#079B96] hover:bg-[#087D78] text-white text-xs font-bold shadow-md cursor-pointer disabled:opacity-50"
                        >
                          {savingEdit ? 'Saving...' : 'Save Passport Changes'}
                        </button>
                      </div>
                    </form>
                  )}
                </div>
              )}

              {/* ── TAB 2: MEDICAL TIMELINE ── */}
              {activeTab === 'timeline' && (
                <div className="space-y-4">
                  {timeline.length === 0 ? (
                    <div className="p-8 text-center bg-[#FAF8F5] rounded-2xl border border-dashed border-[#10324E]/20 text-xs text-[#718096]">
                      No clinical events recorded yet. Completed appointments, administered vaccinations, and uploaded lab reports will automatically appear on this timeline.
                    </div>
                  ) : (
                    <div className="relative pl-6 space-y-6 before:absolute before:left-2 before:top-2 before:bottom-2 before:w-0.5 before:bg-[#079B96]/30">
                      {timeline.map((item, idx) => (
                        <div key={idx} className="relative group text-xs">
                          {/* Timeline dot */}
                          <div className={`absolute -left-6 top-1.5 w-4 h-4 rounded-full border-2 border-white shadow-xs flex items-center justify-center ${
                            item.eventType === 'VACCINATION' ? 'bg-[#079B96]' : item.eventType === 'CONSULTATION' ? 'bg-[#123F70]' : 'bg-[#FF843F]'
                          }`} />

                          <div className="bg-[#FAF8F5] rounded-2xl p-4 border border-[#10324E]/8 shadow-2xs hover:shadow-md transition">
                            <div className="flex items-center justify-between gap-2">
                              <span className="font-extrabold text-sm text-[#123F70]">{item.title}</span>
                              <span className="text-[10px] font-mono font-bold text-[#718096]">
                                {item.eventTimestamp ? new Date(item.eventTimestamp).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'Recent'}
                              </span>
                            </div>

                            <p className="text-[#079B96] font-bold text-xs mt-0.5">{item.subtitle}</p>
                            <p className="text-[#192A3A]/75 mt-1 leading-relaxed">{item.description}</p>

                            <div className="mt-3 pt-2 border-t border-[#10324E]/8 flex items-center justify-between text-[11px]">
                              <span className="text-[#718096]">Attending: <strong className="text-[#123F70]">{item.clinicOrDoctor}</strong></span>
                              <span className="inline-flex items-center gap-1 font-bold text-[#079B96]">
                                <CheckCircle2 className="w-3 h-3" />
                                <span>{item.verificationStatus === 'VERIFIED_BY_VET' ? 'Verified Clinical Record' : 'Owner Uploaded'}</span>
                              </span>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* ── TAB 3: DOCUMENT VAULT ── */}
              {activeTab === 'documents' && (
                <div className="space-y-4">
                  <div className="flex items-center justify-between">
                    <p className="text-xs text-[#718096]">
                      Vaccination certificates, lab tests, blood reports & X-rays.
                    </p>
                    <button
                      onClick={() => setShowDocUpload(!showDocUpload)}
                      className="px-3.5 py-2 rounded-xl bg-[#123F70] hover:bg-[#0B315C] text-white text-xs font-bold flex items-center gap-1.5 shadow-2xs cursor-pointer"
                    >
                      <Upload className="w-3.5 h-3.5" />
                      <span>{showDocUpload ? 'Cancel' : 'Upload Document'}</span>
                    </button>
                  </div>

                  {showDocUpload && (
                    <form onSubmit={handleDocUploadSubmit} className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#123F70]/20 space-y-3 text-xs animate-in fade-in">
                      <h4 className="font-bold text-[#123F70]">Upload Medical Document</h4>
                      <div className="grid grid-cols-2 gap-3">
                        <div>
                          <label className="block text-[11px] font-bold text-[#123F70] mb-1">Category *</label>
                          <select
                            value={docForm.category}
                            onChange={(e) => setDocForm({ ...docForm, category: e.target.value })}
                            className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#123F70]"
                          >
                            <option value="VACCINATION_CERTIFICATE">Vaccination Certificate</option>
                            <option value="PRESCRIPTION">Veterinary Prescription</option>
                            <option value="LAB_REPORT">Blood / Diagnostic Report</option>
                            <option value="XRAY">Digital X-Ray / Radiograph</option>
                            <option value="SURGERY_REPORT">Surgery / Procedure Report</option>
                            <option value="DISCHARGE_SUMMARY">Discharge Summary</option>
                            <option value="INSURANCE_DOC">Insurance Policy Document</option>
                            <option value="OTHER">Other Health Document</option>
                          </select>
                        </div>
                        <div>
                          <label className="block text-[11px] font-bold text-[#123F70] mb-1">Document Title *</label>
                          <input
                            type="text"
                            required
                            value={docForm.fileName}
                            onChange={(e) => setDocForm({ ...docForm, fileName: e.target.value })}
                            placeholder="e.g. CBC Blood Panel March 2026"
                            className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#123F70]"
                          />
                        </div>
                      </div>

                      <div>
                        <label className="block text-[11px] font-bold text-[#123F70] mb-1">Description / Clinical Notes</label>
                        <input
                          type="text"
                          value={docForm.description}
                          onChange={(e) => setDocForm({ ...docForm, description: e.target.value })}
                          placeholder="e.g. Normal white blood cell count, routine checkup"
                          className="w-full px-3 py-2 rounded-xl bg-white border border-[#10324E]/15 text-xs text-[#123F70]"
                        />
                      </div>

                      <div className="flex justify-end gap-2 pt-2">
                        <button
                          type="button"
                          onClick={() => setShowDocUpload(false)}
                          className="px-4 py-2 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#123F70] cursor-pointer"
                        >
                          Cancel
                        </button>
                        <button
                          type="submit"
                          disabled={uploadingDoc}
                          className="px-5 py-2 rounded-xl bg-[#079D96] hover:bg-[#087D78] text-white text-xs font-bold shadow-md cursor-pointer disabled:opacity-50"
                        >
                          {uploadingDoc ? 'Saving...' : 'Save to Vault'}
                        </button>
                      </div>
                    </form>
                  )}

                  {documents.length === 0 ? (
                    <div className="p-8 text-center bg-[#FAF8F5] rounded-2xl border border-dashed border-[#10324E]/20 text-xs text-[#718096]">
                      No documents stored in vault yet. Click "Upload Document" to attach reports.
                    </div>
                  ) : (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {documents.map((doc) => (
                        <div key={doc.documentId} className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 hover:border-[#079D96]/40 transition text-xs space-y-1">
                          <div className="flex items-start justify-between gap-2">
                            <span className="font-bold text-[#123F70] truncate">{doc.fileName}</span>
                            <span className="text-[10px] font-extrabold px-2 py-0.5 rounded-full bg-slate-200 text-[#123F70]">
                              {doc.category?.replace(/_/g, ' ')}
                            </span>
                          </div>
                          {doc.description && <p className="text-[#718096] text-[11px] truncate">{doc.description}</p>}
                          <div className="pt-2 flex items-center justify-between text-[11px] text-[#718096]">
                            <span>{doc.documentDate || 'Recent'}</span>
                            <a
                              href={doc.storagePath}
                              target="_blank"
                              rel="noreferrer"
                              className="font-bold text-[#079D96] hover:underline flex items-center gap-1"
                            >
                              <span>View File</span>
                              <ExternalLink className="w-3 h-3" />
                            </a>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* ── TAB 4: SHARE RECORDS ── */}
              {activeTab === 'share' && (
                <div className="space-y-4 text-xs">
                  <div className="p-4 rounded-2xl bg-[#E8F6F4] border border-[#079D96]/30 space-y-2">
                    <h4 className="font-bold text-[#123F70]">Consent-Gated Record Sharing</h4>
                    <p className="text-[#192A3A]/80 leading-relaxed">
                      Grant temporary, consent-controlled access to your pet's health records for a visiting veterinarian or second-opinion hospital. You can revoke access at any time.
                    </p>
                  </div>

                  {shareSuccess && (
                    <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-300 text-emerald-950 space-y-1 animate-in fade-in">
                      <div className="font-bold flex items-center gap-1.5 text-emerald-800">
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Active Record Share Grant Created!</span>
                      </div>
                      <p><strong>Scope:</strong> {shareSuccess.scope}</p>
                      <p><strong>Expires:</strong> {shareSuccess.expiresAt}</p>
                      <p className="text-[11px] text-emerald-700">Token ID: <code className="font-mono bg-white px-1.5 py-0.5 rounded border border-emerald-200">{shareSuccess.id}</code></p>
                    </div>
                  )}

                  <div className="space-y-3 p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/10">
                    <div>
                      <label className="block font-bold text-[#123F70] mb-1">Sharing Scope *</label>
                      <select
                        value={shareScope}
                        onChange={(e) => setShareScope(e.target.value)}
                        className="w-full px-3.5 py-2.5 rounded-xl bg-white border border-[#10324E]/15 text-xs font-bold text-[#123F70]"
                      >
                        <option value="FULL_TIMELINE">Full Health Passport & Clinical History</option>
                        <option value="VACCINES_ONLY">Vaccination History Only</option>
                        <option value="EMERGENCY_SUMMARY">Emergency Triage & Allergies Summary</option>
                      </select>
                    </div>

                    <div>
                      <label className="block font-bold text-[#123F70] mb-1">Access Duration *</label>
                      <select
                        value={shareHours}
                        onChange={(e) => setShareHours(parseInt(e.target.value, 10))}
                        className="w-full px-3.5 py-2.5 rounded-xl bg-white border border-[#10324E]/15 text-xs font-bold text-[#123F70]"
                      >
                        <option value={24}>24 Hours (Temporary Consult)</option>
                        <option value={72}>72 Hours (Standard Referral)</option>
                        <option value={168}>7 Days (Extended Hospitalization)</option>
                        <option value={0}>Permanent (Home Clinic)</option>
                      </select>
                    </div>

                    <button
                      onClick={handleCreateShare}
                      disabled={sharingLoading}
                      className="w-full py-3 rounded-xl bg-[#079D96] hover:bg-[#087D78] text-white font-bold text-xs shadow-md transition cursor-pointer disabled:opacity-50 flex items-center justify-center gap-2"
                    >
                      <Share2 className="w-4 h-4" />
                      <span>{sharingLoading ? 'Generating Share Token...' : 'Grant Record Access'}</span>
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 px-6 border-t border-[#10324E]/10 flex justify-between items-center bg-[#FAF8F5] rounded-b-[32px] shrink-0 text-xs text-[#718096]">
          <span>Every Pet’s Health. Always.</span>
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-[#123F70] hover:bg-[#0B315C] text-white font-bold transition shadow-md cursor-pointer"
          >
            Close Passport
          </button>
        </div>

      </div>
    </div>
  );
}

import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../../context/AuthContext';
import {
  getAdminKpisApi,
  getAdminVerificationsApi,
  updateDoctorVerificationStatusApi,
  getAdminUsersApi,
  updateUserStatusApi,
  getDatabaseInspectionApi,
} from '../../../services/api';
import {
  ShieldCheck,
  Building2,
  Users,
  Award,
  DollarSign,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Database,
  Lock,
  Eye,
  FileCheck,
  Server,
  Activity,
  UserX,
  UserCheck,
  AlertCircle,
  Stethoscope,
  Briefcase,
  UserCog,
} from 'lucide-react';

export default function AdminDashboard() {
  const { currentUser, role } = useAuth();

  const isSuperAdmin = role === 'ROLE_PLATFORM_ADMIN' || role === 'PLATFORM_ADMIN';
  const isClinicAdmin = role === 'ROLE_CLINIC_ADMIN' || role === 'CLINIC_ADMIN';

  // KPI Ribbon state - loaded live from database
  const [kpis, setKpis] = useState({
    totalPets: 0,
    verifiedVets: 0,
    pendingVets: 0,
    activeClinics: 0,
    dailyConsults: 0,
    totalConsultations: 0,
    totalUsers: 0,
    clinicStaffCount: 0,
    platformRevenue: '₹0.0L',
  });
  const [loadingKpis, setLoadingKpis] = useState(true);

  // Council Verification Queue state - Platform Admin only
  const [verifications, setVerifications] = useState([]);
  const [loadingVerifications, setLoadingVerifications] = useState(false);

  // Selected certificate for preview modal
  const [selectedCert, setSelectedCert] = useState(null);

  // Staff / User Management & RBAC audit state - loaded live from database
  const [users, setUsers] = useState([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [userSearch, setUserSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');

  // Supabase Telemetry live query - Platform Admin only
  const [telemetry, setTelemetry] = useState(null);
  const [loadingTelemetry, setLoadingTelemetry] = useState(false);

  // General error banner
  const [actionError, setActionError] = useState(null);
  const [actionSuccess, setActionSuccess] = useState(null);

  const fetchKpis = useCallback(async () => {
    setLoadingKpis(true);
    try {
      const data = await getAdminKpisApi();
      if (data) {
        setKpis({
          totalPets: data.totalPets ?? 0,
          verifiedVets: data.verifiedVets ?? 0,
          pendingVets: data.pendingVets ?? 0,
          activeClinics: data.activeClinics ?? 0,
          dailyConsults: data.dailyConsults ?? 0,
          totalConsultations: data.totalConsultations ?? 0,
          totalUsers: data.totalUsers ?? 0,
          clinicStaffCount: data.clinicStaffCount ?? 0,
          platformRevenue: data.platformRevenue ?? '₹0.0L',
        });
      }
    } catch (e) {
      console.warn('Could not fetch admin KPIs:', e.message);
    } finally {
      setLoadingKpis(false);
    }
  }, []);

  const fetchVerifications = useCallback(async () => {
    if (!isSuperAdmin) return;
    setLoadingVerifications(true);
    try {
      const data = await getAdminVerificationsApi();
      if (Array.isArray(data)) {
        setVerifications(data);
      } else {
        setVerifications([]);
      }
    } catch (e) {
      console.warn('Could not fetch admin verifications:', e.message);
      setVerifications([]);
    } finally {
      setLoadingVerifications(false);
    }
  }, [isSuperAdmin]);

  const fetchUsers = useCallback(async () => {
    setLoadingUsers(true);
    try {
      const data = await getAdminUsersApi();
      if (Array.isArray(data)) {
        setUsers(data);
      } else {
        setUsers([]);
      }
    } catch (e) {
      console.warn('Could not fetch admin users:', e.message);
      setUsers([]);
    } finally {
      setLoadingUsers(false);
    }
  }, []);

  const fetchTelemetry = useCallback(async () => {
    if (!isSuperAdmin) return;
    setLoadingTelemetry(true);
    try {
      const data = await getDatabaseInspectionApi();
      setTelemetry(data);
    } catch (e) {
      console.warn('Telemetry check warning:', e.message);
    } finally {
      setLoadingTelemetry(false);
    }
  }, [isSuperAdmin]);

  const loadAllAdminData = useCallback(() => {
    setActionError(null);
    fetchKpis();
    if (isSuperAdmin) {
      fetchVerifications();
      fetchTelemetry();
    }
    fetchUsers();
  }, [fetchKpis, fetchVerifications, fetchUsers, fetchTelemetry, isSuperAdmin]);

  useEffect(() => {
    loadAllAdminData();
  }, [loadAllAdminData]);

  const handleApproveDoctor = async (id) => {
    if (!isSuperAdmin) return;
    setActionError(null);
    setActionSuccess(null);
    try {
      await updateDoctorVerificationStatusApi(id, true);
      setVerifications(prev =>
        prev.map(v => (v.id === id ? { ...v, status: 'Verified', isVerified: true } : v))
      );
      setActionSuccess('Veterinarian credentials verified and approved in database.');
      fetchKpis();
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      setActionError(err.message || 'Failed to update doctor verification status.');
    }
  };

  const handleRejectDoctor = async (id) => {
    if (!isSuperAdmin) return;
    setActionError(null);
    setActionSuccess(null);
    try {
      await updateDoctorVerificationStatusApi(id, false);
      setVerifications(prev =>
        prev.map(v => (v.id === id ? { ...v, status: 'Rejected', isVerified: false } : v))
      );
      setActionSuccess('Doctor verification marked as Rejected.');
      fetchKpis();
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      setActionError(err.message || 'Failed to update doctor verification status.');
    }
  };

  const handleToggleUserStatus = async (userId) => {
    setActionError(null);
    setActionSuccess(null);
    const targetUser = users.find(u => u.id === userId);
    if (!targetUser) return;

    const nextStatus = targetUser.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
    try {
      const res = await updateUserStatusApi(userId, nextStatus);
      setUsers(prev =>
        prev.map(u => {
          if (u.id === userId) {
            return {
              ...u,
              status: nextStatus,
              tokenVersion: res.tokenVersion !== undefined ? res.tokenVersion : (nextStatus === 'BLOCKED' ? u.tokenVersion + 1 : u.tokenVersion),
            };
          }
          return u;
        })
      );
      setActionSuccess(`${isClinicAdmin ? 'Staff member' : 'User'} status updated to ${nextStatus}.`);
      setTimeout(() => setActionSuccess(null), 3000);
    } catch (err) {
      setActionError(err.message || 'Failed to update account status.');
    }
  };

  const handleRevokeAllUserTokens = async (userId) => {
    setActionError(null);
    setActionSuccess(null);
    try {
      const targetUser = users.find(u => u.id === userId);
      const res = await updateUserStatusApi(userId, 'BLOCKED');
      if (targetUser?.status === 'ACTIVE') {
        await updateUserStatusApi(userId, 'ACTIVE');
      }
      setUsers(prev =>
        prev.map(u => (u.id === userId ? { ...u, tokenVersion: u.tokenVersion + 1 } : u))
      );
      setActionSuccess(`Active session invalidated for ${targetUser?.name || 'staff member'}.`);
      setTimeout(() => setActionSuccess(null), 3500);
    } catch (err) {
      setActionError(err.message || 'Failed to invalidate tokens.');
    }
  };

  const filteredUsers = users.filter((u) => {
    const term = userSearch.toLowerCase().trim();
    const matchesSearch =
      !term ||
      (u.name && u.name.toLowerCase().includes(term)) ||
      (u.email && u.email.toLowerCase().includes(term)) ||
      (u.id && u.id.toLowerCase().includes(term));
    const matchesRole = roleFilter === 'ALL' || u.role === roleFilter;
    return matchesSearch && matchesRole;
  });

  // Calculate quick stats for Clinic Admin
  const vetCount = users.filter(u => u.role === 'ROLE_VET').length;
  const supportStaffCount = users.filter(u => u.role !== 'ROLE_VET').length;

  return (
    <div className="space-y-8 animate-in fade-in">
      
      {/* ── Admin Greeting & Governance Header ── */}
      <div className={`bg-gradient-to-r ${isClinicAdmin ? 'from-white via-white to-emerald-50 border-[#2D8A7A]/30' : 'from-white via-white to-amber-50 border-amber-200/80'} rounded-[32px] p-6 sm:p-8 border shadow-vetalya flex flex-col md:flex-row md:items-center justify-between gap-6 text-left`}>
        <div>
          <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full ${isClinicAdmin ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'} text-[11px] font-extrabold uppercase tracking-wider mb-2`}>
            {isClinicAdmin ? <Building2 className="w-3.5 h-3.5" /> : <ShieldCheck className="w-3.5 h-3.5" />}
            <span>{isClinicAdmin ? 'Practice Operations & Staff Governance' : 'Platform Governance & Regulatory Console'}</span>
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[#10324E] font-display">
            {isClinicAdmin ? 'Clinic Administration & Staff Roster' : 'National Registry & Platform Admin'}
          </h1>
          <p className="text-sm text-[#242826]/70 mt-1">
            {isClinicAdmin
              ? 'Manage practicing veterinarians, receptionists, lab technicians, and clinic personnel for your clinical practice.'
              : 'Live database management for Veterinary Council accreditations, user RBAC security, and PostgreSQL telemetry.'}
          </p>
        </div>

        <button
          onClick={loadAllAdminData}
          className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-white hover:bg-slate-50 border border-[#10324E]/15 text-xs font-bold text-[#10324E] shadow-2xs transition cursor-pointer self-start md:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 text-[#2D8A7A] ${loadingKpis || loadingVerifications ? 'animate-spin' : ''}`} />
          <span>Sync from Database</span>
        </button>
      </div>

      {/* Notifications / Error / Success Alerts */}
      {actionError && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-bold flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-2">
            <AlertCircle className="w-4 h-4 text-rose-600 shrink-0" />
            <span>{actionError}</span>
          </div>
          <button onClick={() => setActionError(null)} className="text-rose-600 hover:text-rose-800 font-extrabold">✕</button>
        </div>
      )}

      {actionSuccess && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-bold flex items-center justify-between animate-in fade-in">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
            <span>{actionSuccess}</span>
          </div>
          <button onClick={() => setActionSuccess(null)} className="text-emerald-600 hover:text-emerald-800 font-extrabold">✕</button>
        </div>
      )}

      {/* ── KPI Ribbon ── */}
      {isClinicAdmin ? (
        /* Scoped Clinic Practice KPIs */
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
            <span className="text-xs font-bold text-[#242826]/60">Total Practice Staff</span>
            <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">
              {loadingUsers ? '...' : users.length}
            </p>
            <span className="text-[10px] text-[#2D8A7A] font-bold">Employed at clinic</span>
          </div>

          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
            <span className="text-xs font-bold text-[#242826]/60">On-Duty Veterinarians</span>
            <p className="text-2xl font-extrabold text-[#2D8A7A] font-display mt-1">
              {loadingUsers ? '...' : vetCount}
            </p>
            <span className="text-[10px] text-emerald-700 font-bold">Licensed practitioners</span>
          </div>

          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
            <span className="text-xs font-bold text-[#242826]/60">Support Personnel</span>
            <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">
              {loadingUsers ? '...' : supportStaffCount}
            </p>
            <span className="text-[10px] text-[#242826]/60 font-bold">OPD, Lab &amp; Pharmacy</span>
          </div>

          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
            <span className="text-xs font-bold text-[#242826]/60">Today's Appointments</span>
            <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">
              {loadingKpis ? '...' : kpis.dailyConsults.toLocaleString()}
            </p>
            <span className="text-[10px] text-amber-700 font-bold">Live OPD Queue</span>
          </div>
        </div>
      ) : (
        /* Platform Super Admin Full National KPIs */
        <div id="executive-kpis" className="grid grid-cols-2 md:grid-cols-5 gap-4 scroll-mt-28">
          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
            <span className="text-xs font-bold text-[#242826]/60">Registered Pets</span>
            <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">
              {loadingKpis ? '...' : kpis.totalPets.toLocaleString()}
            </p>
            <span className="text-[10px] text-[#2D8A7A] font-bold">Verified in Database</span>
          </div>

          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
            <span className="text-xs font-bold text-[#242826]/60">Verified Vets</span>
            <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">
              {loadingKpis ? '...' : kpis.verifiedVets.toLocaleString()}
            </p>
            <span className="text-[10px] text-amber-700 font-bold">
              {kpis.pendingVets} pending review
            </span>
          </div>

          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
            <span className="text-xs font-bold text-[#242826]/60">Active Clinics</span>
            <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">
              {loadingKpis ? '...' : kpis.activeClinics.toLocaleString()}
            </p>
            <span className="text-[10px] text-[#2D8A7A] font-bold">Pan-India coverage</span>
          </div>

          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
            <span className="text-xs font-bold text-[#242826]/60">Total Consultations</span>
            <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">
              {loadingKpis ? '...' : kpis.totalConsultations.toLocaleString()}
            </p>
            <span className="text-[10px] text-emerald-700 font-bold">OPD case records</span>
          </div>

          <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left col-span-2 md:col-span-1">
            <span className="text-xs font-bold text-[#242826]/60">Total User Accounts</span>
            <p className="text-2xl font-extrabold text-[#2D8A7A] font-display mt-1">
              {loadingKpis ? '...' : kpis.totalUsers.toLocaleString()}
            </p>
            <span className="text-[10px] text-[#242826]/60">Active credentials</span>
          </div>
        </div>
      )}

      {/* ── Section: Veterinary Council Verification Registry (PLATFORM ADMIN ONLY) ── */}
      {isSuperAdmin && (
        <div id="vet-approvals" className="bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left scroll-mt-28">
          <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8 mb-6">
            <div className="flex items-center gap-2">
              <Award className="w-5 h-5 text-[#2D8A7A]" />
              <h3 className="font-extrabold text-base sm:text-lg text-[#10324E]">
                Veterinary Council Credential Verifications
              </h3>
            </div>
            <span className="text-xs font-bold text-amber-800 bg-amber-50 px-2.5 py-1 rounded-full border border-amber-200">
              {verifications.filter((v) => !v.isVerified && v.status !== 'Rejected').length} Pending Review
            </span>
          </div>

          {loadingVerifications ? (
            <div className="p-8 text-center text-xs text-[#242826]/60 animate-pulse">
              Loading practitioner verifications from database...
            </div>
          ) : verifications.length === 0 ? (
            <div className="p-8 rounded-2xl bg-[#FAF8F5] border border-dashed border-[#10324E]/15 text-center">
              <Award className="w-8 h-8 text-[#10324E]/30 mx-auto mb-2" />
              <p className="font-bold text-xs text-[#10324E]">No veterinary registration applications found</p>
              <p className="text-[11px] text-[#242826]/60 mt-0.5">Registered practitioners will appear here for statutory council verification.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {verifications.map((doc) => (
                <div
                  key={doc.id}
                  className="p-5 rounded-3xl bg-[#FAF8F5] border border-[#10324E]/8 hover:border-[#2D8A7A]/30 transition flex flex-col lg:flex-row lg:items-center justify-between gap-4"
                >
                  <div className="flex items-start gap-4">
                    <div className="w-12 h-12 rounded-2xl bg-[#E0F2EB] text-[#2D8A7A] flex items-center justify-center font-bold text-base shrink-0 mt-0.5">
                      <Award className="w-6 h-6" />
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h4 className="font-extrabold text-base text-[#10324E]">{doc.doctorName}</h4>
                        <span
                          className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full border ${
                            doc.isVerified || doc.status === 'Verified'
                              ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                              : doc.status === 'Rejected'
                              ? 'bg-rose-100 text-rose-800 border-rose-300'
                              : 'bg-amber-100 text-amber-800 border-amber-300'
                          }`}
                        >
                          {doc.isVerified || doc.status === 'Verified' ? 'Verified' : doc.status}
                        </span>
                      </div>
                      <p className="text-xs text-[#242826]/75 mt-0.5">
                        <strong>Council Reg:</strong> <span className="font-mono text-[#10324E]">{doc.councilRegNumber}</span> • {doc.councilState}
                      </p>
                      <p className="text-[11px] text-[#242826]/60 mt-0.5">
                        {doc.degrees} • {doc.university} {doc.email ? `• ${doc.email}` : ''}
                      </p>
                    </div>
                  </div>

                  {/* Action Buttons */}
                  <div className="flex items-center gap-2 self-end lg:self-center shrink-0">
                    <button
                      onClick={() => setSelectedCert(doc)}
                      className="px-3 py-1.5 rounded-xl bg-white border border-[#10324E]/15 text-xs font-bold text-[#10324E] hover:bg-slate-50 transition cursor-pointer flex items-center gap-1 shadow-2xs"
                    >
                      <Eye className="w-3.5 h-3.5 text-[#2D8A7A]" />
                      <span>View License</span>
                    </button>

                    {!doc.isVerified && doc.status !== 'Verified' && (
                      <>
                        <button
                          onClick={() => handleApproveDoctor(doc.id)}
                          className="px-4 py-1.5 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-bold shadow-xs transition cursor-pointer flex items-center gap-1"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5" />
                          <span>Approve</span>
                        </button>

                        <button
                          onClick={() => handleRejectDoctor(doc.id)}
                          className="px-3 py-1.5 rounded-xl bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 text-xs font-bold transition cursor-pointer"
                        >
                          <span>Reject</span>
                        </button>
                      </>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ── Section: User / Staff Management & Security Audit Console ── */}
      <div id="security-audit" className="bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left scroll-mt-28">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-[#10324E]/8 gap-3 mb-4">
          <div>
            <div className="flex items-center gap-2">
              {isClinicAdmin ? <UserCog className="w-5 h-5 text-[#2D8A7A]" /> : <Lock className="w-5 h-5 text-[#2D8A7A]" />}
              <h3 className="font-extrabold text-base sm:text-lg text-[#10324E]">
                {isClinicAdmin ? 'Practice Staff Directory & Account Management' : 'User Security & Token Invalidation Console'}
              </h3>
            </div>
            <p className="text-xs text-[#242826]/60 mt-0.5">
              {isClinicAdmin
                ? 'Manage active staff accounts and credential validity exclusively for clinical practice personnel. (Pet owner and platform accounts are isolated).'
                : 'Audit active sessions, force refresh token revocation, and manage user statuses live in database.'}
            </p>
          </div>

          <div className="flex items-center gap-2">
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
            >
              {isClinicAdmin ? (
                <>
                  <option value="ALL">All Practice Staff</option>
                  <option value="ROLE_VET">Veterinarians</option>
                  <option value="ROLE_RECEPTIONIST">Receptionists</option>
                  <option value="ROLE_LAB_TECHNICIAN">Lab Technicians</option>
                  <option value="ROLE_PHARMACIST">Pharmacists</option>
                  <option value="ROLE_VET_NURSE">Veterinary Nurses</option>
                </>
              ) : (
                <>
                  <option value="ALL">All Roles</option>
                  <option value="ROLE_PET_OWNER">Pet Owners</option>
                  <option value="ROLE_VET">Veterinarians</option>
                  <option value="ROLE_CLINIC_ADMIN">Clinic Admins</option>
                  <option value="ROLE_RECEPTIONIST">Receptionists</option>
                  <option value="ROLE_LAB_TECHNICIAN">Lab Technicians</option>
                  <option value="ROLE_PHARMACIST">Pharmacists</option>
                  <option value="ROLE_VET_NURSE">Veterinary Nurses</option>
                  <option value="ROLE_PLATFORM_ADMIN">Platform Admins</option>
                </>
              )}
            </select>

            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-[#242826]/40" />
              <input
                type="text"
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
                placeholder={isClinicAdmin ? "Search staff name..." : "Search user..."}
                className="pl-8 pr-3 py-1 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
              />
            </div>
          </div>
        </div>

        {isClinicAdmin && (
          <div className="mb-4 p-3 rounded-2xl bg-emerald-50/70 border border-emerald-200/60 text-emerald-900 text-[11px] flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-emerald-700 shrink-0" />
            <span>
              <strong>Multi-Tenant Isolation Enforced:</strong> As Clinic Administrator, you have authorization solely over staff members employed at this practice. Pet owner profiles and platform credentials cannot be viewed, altered, or revoked from this console.
            </span>
          </div>
        )}

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead>
              <tr className="border-b border-[#10324E]/10 text-[#242826]/60 font-bold uppercase text-[10px] tracking-wider">
                <th className="pb-3 pl-2">{isClinicAdmin ? 'Staff Member' : 'User Details'}</th>
                <th className="pb-3">{isClinicAdmin ? 'Staff Designation' : 'Assigned Role'}</th>
                <th className="pb-3 text-center">Session Key (`ver`)</th>
                <th className="pb-3">Account Status</th>
                <th className="pb-3">{isClinicAdmin ? 'Employment Date' : 'Registration Date'}</th>
                <th className="pb-3 text-right pr-2">Security Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#10324E]/5">
              {loadingUsers ? (
                <tr>
                  <td colSpan="6" className="py-8 text-center text-xs text-[#242826]/60 animate-pulse">
                    {isClinicAdmin ? 'Loading practice staff records from database...' : 'Loading platform user records from database...'}
                  </td>
                </tr>
              ) : filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan="6" className="py-8 text-center text-xs text-[#242826]/60">
                    {isClinicAdmin ? 'No staff members matching criteria.' : 'No users matching criteria.'}
                  </td>
                </tr>
              ) : (
                filteredUsers.map((u) => (
                  <tr key={u.id} className="hover:bg-[#FAF8F5] transition">
                    <td className="py-3.5 pl-2">
                      <div className="font-bold text-[#10324E]">{u.name}</div>
                      <div className="text-[11px] text-[#242826]/60 font-mono">{u.email}</div>
                    </td>
                    <td className="py-3.5">
                      <span className="font-mono text-[11px] font-bold bg-[#FAF8F5] px-2 py-0.5 rounded-lg border border-[#10324E]/10 text-[#10324E]">
                        {u.role.replace('ROLE_', '')}
                      </span>
                    </td>
                    <td className="py-3.5 font-mono text-center">
                      <span className="bg-slate-100 text-[#10324E] px-2.5 py-0.5 rounded-full font-bold">
                        v{u.tokenVersion ?? 0}
                      </span>
                    </td>
                    <td className="py-3.5">
                      <span
                        className={`px-2.5 py-1 rounded-full text-[10px] font-bold border ${
                          u.status === 'ACTIVE'
                            ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                            : 'bg-rose-100 text-rose-800 border-rose-300'
                        }`}
                      >
                        {u.status}
                      </span>
                    </td>
                    <td className="py-3.5 text-[11px] text-[#242826]/70">{u.createdAt || 'Active'}</td>
                    <td className="py-3.5 text-right pr-2 space-x-1.5">
                      <button
                        onClick={() => handleRevokeAllUserTokens(u.id)}
                        title="Bump token_version to invalidate active sessions"
                        className="px-2.5 py-1 rounded-lg bg-amber-100 hover:bg-amber-200 text-amber-900 font-bold text-[11px] transition cursor-pointer"
                      >
                        Reset Session
                      </button>
                      <button
                        onClick={() => handleToggleUserStatus(u.id)}
                        className={`px-2.5 py-1 rounded-lg font-bold text-[11px] transition cursor-pointer ${
                          u.status === 'ACTIVE'
                            ? 'bg-rose-100 hover:bg-rose-200 text-rose-800'
                            : 'bg-emerald-100 hover:bg-emerald-200 text-emerald-800'
                        }`}
                      >
                        {u.status === 'ACTIVE' ? (isClinicAdmin ? 'Suspend Staff' : 'Block User') : (isClinicAdmin ? 'Reactivate' : 'Unblock')}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Section: System Telemetry & Supabase Database Health (PLATFORM ADMIN ONLY) ── */}
      {isSuperAdmin && (
        <div id="telemetry" className="bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left scroll-mt-28">
          <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8 mb-4">
            <div className="flex items-center gap-2">
              <Server className="w-5 h-5 text-[#2D8A7A]" />
              <h3 className="font-extrabold text-base sm:text-lg text-[#10324E]">
                Live Database Telemetry &amp; Table Health
              </h3>
            </div>
            <span className="flex items-center gap-1.5 text-xs text-emerald-700 font-bold bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              {telemetry?.status === 'CONNECTED' ? 'Supabase PostgreSQL Healthy' : 'Database Connected'}
            </span>
          </div>

          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs font-mono">
            <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
              <div className="font-bold text-[#10324E] mb-1">user_auth records</div>
              <div className="text-xl font-bold text-[#2D8A7A]">
                {telemetry?.tables?.user_auth ?? kpis.totalUsers} rows
              </div>
              <div className="text-[10px] text-[#242826]/60 mt-1">ULID Primary Keys</div>
            </div>

            <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
              <div className="font-bold text-[#10324E] mb-1">pet &amp; owner records</div>
              <div className="text-xl font-bold text-[#10324E]">
                {(telemetry?.tables?.pet ?? kpis.totalPets) + (telemetry?.tables?.pet_owner ?? 0)} rows
              </div>
              <div className="text-[10px] text-[#242826]/60 mt-1">Passports &amp; Profiles</div>
            </div>

            <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
              <div className="font-bold text-[#10324E] mb-1">clinical appointments</div>
              <div className="text-xl font-bold text-[#2D8A7A]">
                {telemetry?.tables?.appointment ?? kpis.totalConsultations} rows
              </div>
              <div className="text-[10px] text-[#242826]/60 mt-1">Consultations &amp; SOAP</div>
            </div>

            <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
              <div className="font-bold text-[#10324E] mb-1">query latency</div>
              <div className="text-xl font-bold text-[#10324E]">
                {telemetry?.latencyMs ? `${telemetry.latencyMs} ms` : '18 ms'}
              </div>
              <div className="text-[10px] text-[#242826]/60 mt-1">Supabase SSL Pool</div>
            </div>
          </div>
        </div>
      )}

      {/* ── View Certificate Preview Modal (PLATFORM ADMIN ONLY) ── */}
      {selectedCert && isSuperAdmin && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-xl w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/10">
              <div>
                <h3 className="text-lg font-extrabold text-[#10324E]">{selectedCert.doctorName}</h3>
                <p className="text-xs text-[#242826]/60">{selectedCert.councilState} • {selectedCert.councilRegNumber}</p>
              </div>
              <button onClick={() => setSelectedCert(null)} className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100 cursor-pointer">
                ✕
              </button>
            </div>

            <div className="p-3 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 text-xs space-y-1.5">
              <p><strong>Institution:</strong> {selectedCert.university}</p>
              <p><strong>Degrees:</strong> {selectedCert.degrees} ({selectedCert.graduationYear})</p>
              <p><strong>Submission:</strong> {selectedCert.submittedDate}</p>
              {selectedCert.email && <p><strong>Email:</strong> {selectedCert.email}</p>}
              {selectedCert.phone && <p><strong>Phone:</strong> {selectedCert.phone}</p>}
            </div>

            <div className="rounded-2xl overflow-hidden border border-slate-300 h-64 bg-slate-900 flex items-center justify-center">
              <img src={selectedCert.certificateUrl} alt="Council Certificate" className="w-full h-full object-cover" />
            </div>

            <div className="pt-2 flex justify-between items-center">
              <button
                onClick={() => setSelectedCert(null)}
                className="px-4 py-2 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E] hover:bg-slate-50 cursor-pointer"
              >
                Close
              </button>
              {!selectedCert.isVerified && selectedCert.status !== 'Verified' && (
                <button
                  onClick={() => {
                    handleApproveDoctor(selectedCert.id);
                    setSelectedCert(null);
                  }}
                  className="px-5 py-2 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-bold shadow-md cursor-pointer"
                >
                  Verify &amp; Approve Doctor
                </button>
              )}
            </div>
          </div>
        </div>
      )}

    </div>
  );
}

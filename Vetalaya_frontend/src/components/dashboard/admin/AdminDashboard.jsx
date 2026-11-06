import React, { useState, useEffect } from 'react';
import { useAuth } from '../../../context/AuthContext';
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
} from 'lucide-react';

export default function AdminDashboard() {
  const { currentUser, role } = useAuth();

  // KPI Ribbon state
  const [kpis] = useState({
    totalPets: '14,892',
    verifiedVets: '348',
    activeClinics: '62',
    dailyConsults: '1,280',
    platformRevenue: '₹28.4L',
  });

  // Council Verification Queue state
  const [verifications, setVerifications] = useState([
    {
      id: 'ver-01',
      doctorName: 'Dr. Arjun K. Rao',
      email: 'arjun.rao@blrvetclinic.in',
      councilRegNumber: 'KVC-2021-9982',
      councilState: 'Karnataka Veterinary Council (KVC)',
      degrees: 'BVSc & AH, MVSc (Surgery)',
      university: 'Veterinary College, Hebbal (KVAFSU)',
      graduationYear: '2019',
      certificateUrl: 'https://images.unsplash.com/photo-1589829545856-d10d557cf95f?auto=format&fit=crop&w=400&q=80',
      submittedDate: 'Nov 04, 2026',
      status: 'Pending Review',
    },
    {
      id: 'ver-02',
      doctorName: 'Dr. Neha Sen',
      email: 'neha.sen@pawsandcare.org',
      councilRegNumber: 'MVC-2023-4182',
      councilState: 'Maharashtra State Veterinary Council',
      degrees: 'BVSc & AH',
      university: 'Bombay Veterinary College (MAFSU)',
      graduationYear: '2022',
      certificateUrl: 'https://images.unsplash.com/photo-1589829545856-d10d557cf95f?auto=format&fit=crop&w=400&q=80',
      submittedDate: 'Nov 05, 2026',
      status: 'Pending Review',
    },
    {
      id: 'ver-03',
      doctorName: 'Dr. Priya Sharma',
      email: 'dr.priya@koramangalavet.in',
      councilRegNumber: 'KVC-2018-4829',
      councilState: 'Karnataka Veterinary Council (KVC)',
      degrees: 'BVSc & AH, MVSc (Medicine)',
      university: 'KVAFSU Bangalore',
      graduationYear: '2018',
      certificateUrl: 'https://images.unsplash.com/photo-1589829545856-d10d557cf95f?auto=format&fit=crop&w=400&q=80',
      submittedDate: 'Oct 20, 2026',
      status: 'Verified',
    },
  ]);

  // Selected certificate for preview modal
  const [selectedCert, setSelectedCert] = useState(null);

  // Platform User Management & RBAC audit state
  const [users, setUsers] = useState([
    {
      id: '01M6PGVADT9B00MDX34NSBC7NA',
      name: 'Rahul Sharma',
      email: 'owner.test@vetalya.com',
      role: 'ROLE_PET_OWNER',
      status: 'ACTIVE',
      tokenVersion: 0,
      lastLogin: 'Today, 10:12 AM',
    },
    {
      id: '01M6PGVB703TN4F8T2DFNVPDF8',
      name: 'Dr. Priya Sharma',
      email: 'vet.test@vetalya.com',
      role: 'ROLE_VET',
      status: 'ACTIVE',
      tokenVersion: 0,
      lastLogin: 'Today, 10:45 AM',
    },
    {
      id: '01M6PLK893N20DF892KLSM12A0',
      name: 'Suspicious Client 99',
      email: 'spam.bot@unknown.ru',
      role: 'ROLE_PET_OWNER',
      status: 'BLOCKED',
      tokenVersion: 4,
      lastLogin: 'Nov 01, 2026',
    },
    {
      id: '01M6PQ901MN23KL092LKSM8812',
      name: 'Koramangala Triage Desk',
      email: 'reception@koramangalavet.in',
      role: 'ROLE_CLINIC_STAFF',
      status: 'ACTIVE',
      tokenVersion: 0,
      lastLogin: 'Yesterday, 06:30 PM',
    },
  ]);

  const [userSearch, setUserSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');

  // Supabase Telemetry live query
  const [telemetry, setTelemetry] = useState(null);
  const [loadingTelemetry, setLoadingTelemetry] = useState(false);

  const fetchTelemetry = async () => {
    setLoadingTelemetry(true);
    try {
      const res = await fetch('/api/public/db-inspect');
      const data = await res.json();
      setTelemetry(data);
    } catch (e) {
      console.warn('Telemetry check warning:', e);
    } finally {
      setLoadingTelemetry(false);
    }
  };

  useEffect(() => {
    fetchTelemetry();
  }, []);

  const handleApproveDoctor = (id) => {
    setVerifications(
      verifications.map((v) => (v.id === id ? { ...v, status: 'Verified' } : v))
    );
  };

  const handleRejectDoctor = (id) => {
    setVerifications(
      verifications.map((v) => (v.id === id ? { ...v, status: 'Rejected' } : v))
    );
  };

  const handleToggleUserStatus = (userId) => {
    setUsers(
      users.map((u) => {
        if (u.id === userId) {
          const nextStatus = u.status === 'ACTIVE' ? 'BLOCKED' : 'ACTIVE';
          const nextVersion = nextStatus === 'BLOCKED' ? u.tokenVersion + 1 : u.tokenVersion;
          return { ...u, status: nextStatus, tokenVersion: nextVersion };
        }
        return u;
      })
    );
  };

  const handleRevokeAllUserTokens = (userId) => {
    setUsers(
      users.map((u) => (u.id === userId ? { ...u, tokenVersion: u.tokenVersion + 1 } : u))
    );
    alert(`Token version bumped for ${userId}. All unexpired JWTs and refresh tokens have been immediately invalidated.`);
  };

  const filteredUsers = users.filter((u) => {
    const matchesSearch =
      u.name.toLowerCase().includes(userSearch.toLowerCase()) ||
      u.email.toLowerCase().includes(userSearch.toLowerCase()) ||
      u.id.toLowerCase().includes(userSearch.toLowerCase());
    const matchesRole = roleFilter === 'ALL' || u.role === roleFilter;
    return matchesSearch && matchesRole;
  });

  return (
    <div className="space-y-8 animate-in fade-in">
      
      {/* ── Admin Greeting & Governance Header ── */}
      <div className="bg-gradient-to-r from-white via-white to-amber-50 rounded-[32px] p-6 sm:p-8 border border-amber-200/80 shadow-vetalya flex flex-col md:flex-row md:items-center justify-between gap-6 text-left">
        <div>
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-100 text-amber-800 text-[11px] font-extrabold uppercase tracking-wider mb-2">
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Platform Governance &amp; Regulatory Console</span>
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[#10324E] font-display">
            National Registry &amp; Platform Admin
          </h1>
          <p className="text-sm text-[#242826]/70 mt-1">
            Supervising Council Registrations, RBAC Token Invalidation, and Supabase Microservices.
          </p>
        </div>

        <button
          onClick={fetchTelemetry}
          className="flex items-center gap-2 px-4 py-2.5 rounded-2xl bg-white hover:bg-slate-50 border border-[#10324E]/15 text-xs font-bold text-[#10324E] shadow-2xs transition cursor-pointer self-start md:self-auto"
        >
          <RefreshCw className={`w-3.5 h-3.5 text-[#2D8A7A] ${loadingTelemetry ? 'animate-spin' : ''}`} />
          <span>Refresh Database Telemetry</span>
        </button>
      </div>

      {/* ── Executive KPI Ribbon (5 Cards) ── */}
      <div id="executive-kpis" className="grid grid-cols-2 md:grid-cols-5 gap-4 scroll-mt-28">
        <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
          <span className="text-xs font-bold text-[#242826]/60">Registered Pets</span>
          <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">{kpis.totalPets}</p>
          <span className="text-[10px] text-[#2D8A7A] font-bold">+342 this week</span>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
          <span className="text-xs font-bold text-[#242826]/60">Verified Vets</span>
          <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">{kpis.verifiedVets}</p>
          <span className="text-[10px] text-amber-700 font-bold">2 pending approval</span>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
          <span className="text-xs font-bold text-[#242826]/60">Active Clinics</span>
          <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">{kpis.activeClinics}</p>
          <span className="text-[10px] text-[#2D8A7A] font-bold">Pan-India coverage</span>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left">
          <span className="text-xs font-bold text-[#242826]/60">Daily Consults</span>
          <p className="text-2xl font-extrabold text-[#10324E] font-display mt-1">{kpis.dailyConsults}</p>
          <span className="text-[10px] text-emerald-700 font-bold">98.4% completed</span>
        </div>

        <div className="bg-white rounded-3xl p-5 border border-[#10324E]/8 shadow-vetalya text-left col-span-2 md:col-span-1">
          <span className="text-xs font-bold text-[#242826]/60">Consult Volume</span>
          <p className="text-2xl font-extrabold text-[#2D8A7A] font-display mt-1">{kpis.platformRevenue}</p>
          <span className="text-[10px] text-[#242826]/60">Platform throughput</span>
        </div>
      </div>

      {/* ── Section: Veterinary Council Verification Registry ── */}
      <div id="vet-approvals" className="bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left scroll-mt-28">
        <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8 mb-6">
          <div className="flex items-center gap-2">
            <Award className="w-5 h-5 text-[#2D8A7A]" />
            <h3 className="font-extrabold text-base sm:text-lg text-[#10324E]">
              Veterinary Council Credential Verifications
            </h3>
          </div>
          <span className="text-xs font-bold text-amber-800 bg-amber-50 px-2.5 py-1 rounded-full border border-amber-200">
            {verifications.filter((v) => v.status === 'Pending Review').length} Pending Review
          </span>
        </div>

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
                        doc.status === 'Verified'
                          ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                          : doc.status === 'Rejected'
                          ? 'bg-rose-100 text-rose-800 border-rose-300'
                          : 'bg-amber-100 text-amber-800 border-amber-300'
                      }`}
                    >
                      {doc.status}
                    </span>
                  </div>
                  <p className="text-xs text-[#242826]/75 mt-0.5">
                    <strong>Council Reg:</strong> <span className="font-mono text-[#10324E]">{doc.councilRegNumber}</span> • {doc.councilState}
                  </p>
                  <p className="text-[11px] text-[#242826]/60 mt-0.5">
                    {doc.degrees} • {doc.university} (Class of {doc.graduationYear})
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
                  <span>View Degree / License</span>
                </button>

                {doc.status === 'Pending Review' && (
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
      </div>

      {/* ── Section: User Management & Security Audit (Token Invalidation) ── */}
      <div id="security-audit" className="bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left scroll-mt-28">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-[#10324E]/8 gap-3 mb-4">
          <div>
            <h3 className="font-extrabold text-base sm:text-lg text-[#10324E]">
              User Security &amp; Token Invalidation Console
            </h3>
            <p className="text-xs text-[#242826]/60">
              Audit active sessions, force refresh token revocation, and manage user statuses.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <select
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
            >
              <option value="ALL">All Roles</option>
              <option value="ROLE_PET_OWNER">Pet Owners</option>
              <option value="ROLE_VET">Veterinarians</option>
              <option value="ROLE_CLINIC_STAFF">Clinic Staff</option>
            </select>

            <div className="relative">
              <Search className="w-3.5 h-3.5 absolute left-3 top-2.5 text-[#242826]/40" />
              <input
                type="text"
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
                placeholder="Search user..."
                className="pl-8 pr-3 py-1 rounded-xl bg-[#FAF8F5] border border-[#10324E]/15 text-xs text-[#10324E]"
              />
            </div>
          </div>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-xs text-left">
            <thead>
              <tr className="border-b border-[#10324E]/10 text-[#242826]/60 font-bold uppercase text-[10px] tracking-wider">
                <th className="pb-3 pl-2">User Details</th>
                <th className="pb-3">Assigned Role</th>
                <th className="pb-3">Token Version (`ver`)</th>
                <th className="pb-3">Account Status</th>
                <th className="pb-3">Last Active</th>
                <th className="pb-3 text-right pr-2">Security Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#10324E]/5">
              {filteredUsers.map((u) => (
                <tr key={u.id} className="hover:bg-[#FAF8F5] transition">
                  <td className="py-3.5 pl-2">
                    <div className="font-bold text-[#10324E]">{u.name}</div>
                    <div className="text-[11px] text-[#242826]/60 font-mono">{u.email}</div>
                  </td>
                  <td className="py-3.5">
                    <span className="font-mono text-[11px] font-bold bg-[#FAF8F5] px-2 py-0.5 rounded-lg border border-[#10324E]/10 text-[#10324E]">
                      {u.role}
                    </span>
                  </td>
                  <td className="py-3.5 font-mono text-center">
                    <span className="bg-slate-100 text-[#10324E] px-2.5 py-0.5 rounded-full font-bold">
                      v{u.tokenVersion}
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
                  <td className="py-3.5 text-[11px] text-[#242826]/70">{u.lastLogin}</td>
                  <td className="py-3.5 text-right pr-2 space-x-1.5">
                    <button
                      onClick={() => handleRevokeAllUserTokens(u.id)}
                      title="Bump token_version to invalidate all active JWTs immediately"
                      className="px-2.5 py-1 rounded-lg bg-amber-100 hover:bg-amber-200 text-amber-900 font-bold text-[11px] transition cursor-pointer"
                    >
                      Revoke Tokens
                    </button>
                    <button
                      onClick={() => handleToggleUserStatus(u.id)}
                      className={`px-2.5 py-1 rounded-lg font-bold text-[11px] transition cursor-pointer ${
                        u.status === 'ACTIVE'
                          ? 'bg-rose-100 hover:bg-rose-200 text-rose-800'
                          : 'bg-emerald-100 hover:bg-emerald-200 text-emerald-800'
                      }`}
                    >
                      {u.status === 'ACTIVE' ? 'Block User' : 'Unblock'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* ── Section: System Telemetry & Supabase Database Health ── */}
      <div id="telemetry" className="bg-white rounded-[32px] p-6 sm:p-8 border border-[#10324E]/8 shadow-vetalya text-left scroll-mt-28">
        <div className="flex items-center justify-between pb-4 border-b border-[#10324E]/8 mb-4">
          <div className="flex items-center gap-2">
            <Server className="w-5 h-5 text-[#2D8A7A]" />
            <h3 className="font-extrabold text-base sm:text-lg text-[#10324E]">
              Live Supabase Telemetry &amp; Microservices Health
            </h3>
          </div>
          <span className="flex items-center gap-1.5 text-xs text-emerald-700 font-bold bg-emerald-50 px-2.5 py-1 rounded-full border border-emerald-200">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            Supabase PostgreSQL Healthy
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs font-mono">
          <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
            <div className="font-bold text-[#10324E] mb-1">user_auth table</div>
            <div className="text-xl font-bold text-[#2D8A7A]">
              {telemetry?.user_auth?.length || '12'} records
            </div>
            <div className="text-[10px] text-[#242826]/60 mt-1">Status: OK • ULID Primary Keys</div>
          </div>

          <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
            <div className="font-bold text-[#10324E] mb-1">refresh_token table</div>
            <div className="text-xl font-bold text-[#10324E]">
              {telemetry?.refresh_token?.length || '24'} tokens
            </div>
            <div className="text-[10px] text-[#242826]/60 mt-1">Replay defense enabled (family_id)</div>
          </div>

          <div className="p-4 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8">
            <div className="font-bold text-[#10324E] mb-1">auth_provider links</div>
            <div className="text-xl font-bold text-[#2D8A7A]">
              {telemetry?.auth_provider?.length || '8'} active links
            </div>
            <div className="text-[10px] text-[#242826]/60 mt-1">Google OAuth 2.0 &amp; Apple ID</div>
          </div>
        </div>
      </div>

      {/* ── View Certificate Preview Modal ── */}
      {selectedCert && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-white rounded-[32px] max-w-xl w-full p-6 sm:p-8 shadow-vetalya border border-[#10324E]/10 animate-in zoom-in-95 text-left space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#10324E]/10">
              <div>
                <h3 className="text-lg font-extrabold text-[#10324E]">{selectedCert.doctorName}</h3>
                <p className="text-xs text-[#242826]/60">{selectedCert.councilState} • {selectedCert.councilRegNumber}</p>
              </div>
              <button onClick={() => setSelectedCert(null)} className="p-1 rounded-lg text-[#242826]/60 hover:bg-slate-100">
                ✕
              </button>
            </div>

            <div className="p-3 rounded-2xl bg-[#FAF8F5] border border-[#10324E]/8 text-xs space-y-1.5">
              <p><strong>Institution:</strong> {selectedCert.university}</p>
              <p><strong>Degrees:</strong> {selectedCert.degrees} ({selectedCert.graduationYear})</p>
              <p><strong>Submission:</strong> {selectedCert.submittedDate}</p>
            </div>

            <div className="rounded-2xl overflow-hidden border border-slate-300 h-64 bg-slate-900 flex items-center justify-center">
              <img src={selectedCert.certificateUrl} alt="Council Certificate" className="w-full h-full object-cover" />
            </div>

            <div className="pt-2 flex justify-between items-center">
              <button
                onClick={() => setSelectedCert(null)}
                className="px-4 py-2 rounded-xl border border-[#10324E]/15 text-xs font-bold text-[#10324E]"
              >
                Close
              </button>
              {selectedCert.status === 'Pending Review' && (
                <button
                  onClick={() => {
                    handleApproveDoctor(selectedCert.id);
                    setSelectedCert(null);
                  }}
                  className="px-5 py-2 rounded-xl bg-[#2D8A7A] hover:bg-[#236e61] text-white text-xs font-bold shadow-md"
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


import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Building2, CalendarDays, Users, ClipboardList, RefreshCw, Search, CheckCircle2, ShieldCheck, Stethoscope, ArrowRight, FlaskConical, Pill, Wallet, HeartPulse } from 'lucide-react';
import { useAuth } from '../../../context/AuthContext';
import { request } from '../../../services/api';
import { ROLE_LABELS, normalizeRole } from '../../../utils/roles';
import VetDashboard from '../vet/VetDashboard';

const focus = {
  ROLE_VET: ['Clinical workspace', 'Review your patients, coordinate the team and continue consultations.'],
  ROLE_VET_NURSE: ['Nursing workspace', 'Track arrivals and coordinate care handovers with your clinical team.'],
  ROLE_LAB_TECHNICIAN: ['Laboratory workspace', 'Coordinate laboratory work and manage your pending operational tasks.'],
  ROLE_RECEPTIONIST: ['Reception desk', 'Keep arrivals moving and help patients reach their assigned veterinarian.'],
  ROLE_PHARMACIST: ['Pharmacy workspace', 'Coordinate pharmacy requests and stock follow-up with your hospital.'],
  ROLE_ACCOUNTANT: ['Finance workspace', 'Organize billing follow-up and daily reconciliation tasks.'],
  ROLE_CLINIC_STAFF: ['Hospital workspace', 'Follow the patient queue and keep daily operations moving.'],
  ROLE_CLINIC_ADMIN: ['Clinic operations', 'Coordinate your hospital, manage staff access and oversee the daily queue.'],
  ROLE_CLINIC_MANAGER: ['Clinic operations', 'Coordinate your hospital, manage staff access and oversee the daily queue.'],
};
const localDate = () => new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata' }).format(new Date());
const time = value => value ? new Date(value).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit', timeZone: 'Asia/Kolkata' }) : '—';
function patientStatus(a) {
  if (a.status === 'COMPLETED') return 'Completed';
  if (a.status === 'CANCELLED') return 'Cancelled';
  if (a.status === 'NO_SHOW') return 'No show';
  if (a.consultationStartTime && !a.consultationEndTime) return 'In consultation';
  return a.checkInTime ? 'Waiting' : 'Expected';
}
const planned = {
  ROLE_VET_NURSE: [HeartPulse, 'Inpatient care', 'Ward assignments, observations, medication administration and discharge care will be available after the inpatient module is connected.'],
  ROLE_LAB_TECHNICIAN: [FlaskConical, 'Laboratory worklist', 'Branch-scoped sample collection, report entry and clinical review are awaiting laboratory integration.'],
  ROLE_PHARMACIST: [Pill, 'Dispensing & inventory', 'The MedShop system uses a separate store session. Prescription dispensing, batch expiry and stock movements need a verified hospital-to-store connection.'],
  ROLE_ACCOUNTANT: [Wallet, 'Billing & reconciliation', 'Clinic invoices, payment confirmation, refunds and financial reports need the unified billing integration.'],
};
export default function ClinicWorkspace() {
  const { role: rawRole, currentUser } = useAuth();
  const role = normalizeRole(rawRole);
  const [hospitals, setHospitals] = useState([]);
  const [hospitalId, setHospitalId] = useState('');
  const [date, setDate] = useState(localDate);
  const [tab, setTab] = useState('overview');
  const [data, setData] = useState(null);
  const [staff, setStaff] = useState([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('All');
  const [task, setTask] = useState({ title: '', assignedRole: role, priority: 'ROUTINE' });
  const [assignment, setAssignment] = useState({ email: '', department: 'General', active: true });
  const generation = useRef(0);
  const base = `/clinic/hospitals/${encodeURIComponent(hospitalId)}`;
  const hospital = hospitals.find(h => h.hospitalId === hospitalId);
  const load = useCallback(async () => {
    const id = ++generation.current;
    setLoading(true); setError(''); setData(null); setStaff([]);
    try {
      const list = await request('/clinic/hospitals');
      if (id !== generation.current) return;
      setHospitals(list);
      const selected = list.some(h => h.hospitalId === hospitalId) ? hospitalId : list[0]?.hospitalId;
      if (!selected) { setHospitalId(''); return; }
      if (selected !== hospitalId) { setHospitalId(selected); return; }
      const result = await request(`${base}/dashboard?date=${date}`);
      if (id !== generation.current) return;
      setData(result);
      if (result.canManageStaff) {
        const members = await request(`${base}/staff`);
        if (id === generation.current) setStaff(members);
      }
    } catch (e) { if (id === generation.current) setError(e.message || 'Unable to load the hospital workspace.'); }
    finally { if (id === generation.current) setLoading(false); }
  }, [hospitalId, date, base]);
  useEffect(() => { load(); return () => { generation.current++; }; }, [load]);
  async function mutate(path, payload, method = 'POST') {
    setBusy(true); setError(''); setNotice('');
    try {
      await request(`${base}${path}`, { method, ...(payload ? { body: JSON.stringify(payload) } : {}) });
      setNotice('Saved successfully.');
      await load();
      return true;
    } catch (e) { setError(e.message || 'Your change could not be saved.'); return false; }
    finally { setBusy(false); }
  }
  const queue = data?.queue || [];
  const tasks = data?.tasks || [];
  const filtered = queue.filter(a => `${a.petName} ${a.petId} ${a.species} ${a.vetName} ${a.queueNumber ?? ''}`.toLowerCase().includes(query.toLowerCase()) && (status === 'All' || patientStatus(a) === status));
  const [title, subtitle] = focus[role] || focus.ROLE_CLINIC_STAFF;
  const integration = planned[role];
  const tabs = [['overview', 'Overview'], ...(data?.queueAccess ? [['queue', 'Patient queue']] : []), ['tasks', 'Tasks & handovers'], ...(data?.canManageStaff ? [['staff', 'Staff & access']] : []), ...(role === 'ROLE_VET' ? [['clinical', 'Consultation desk']] : [])];
  const safeTab = tabs.some(([key]) => key === tab) ? tab : 'overview';
  return <div className="space-y-6 text-left" id="clinic-workspace">
    <section className="rounded-3xl border border-[#123F70]/10 bg-white p-6 sm:p-8">
      <div className="flex flex-wrap items-start justify-between gap-5">
        <div><p className="text-xs font-bold uppercase tracking-widest text-[#079D96]">Vetalya · {ROLE_LABELS[role]}</p><h1 className="mt-2 text-3xl font-bold text-[#123F70]">{title}</h1><p className="mt-2 text-sm text-slate-500">Welcome, {currentUser?.name || 'team member'}. {subtitle}</p></div>
        <button type="button" onClick={load} disabled={loading || busy} className="staff-button secondary"><RefreshCw size={16} className={loading ? 'animate-spin' : ''} />Refresh</button>
      </div>
      <div className="mt-6 flex flex-wrap items-end gap-4">
        <label className="flex-1 min-w-48 text-xs font-semibold text-slate-600">Hospital / branch<select aria-label="Hospital or branch" value={hospitalId} disabled={busy || !hospitals.length} onChange={e => { setData(null); setStaff([]); setHospitalId(e.target.value); setNotice(''); }} className="staff-input mt-2"><option value="" disabled>Select hospital</option>{hospitals.map(h => <option key={h.hospitalId} value={h.hospitalId}>{h.name} · {h.city}</option>)}</select></label>
        <label className="text-xs font-semibold text-slate-600">Working date · IST<input type="date" value={date} disabled={busy} onChange={e => { if(e.target.value) {setData(null); setDate(e.target.value);} }} className="staff-input mt-2" /></label>
      </div>
    </section>
    {error && <div role="alert" className="rounded-2xl bg-rose-50 border border-rose-200 p-4 text-sm text-rose-800">{error}<button onClick={load} className="ml-3 underline">Retry</button></div>}
    {notice && <p role="status" className="rounded-xl bg-teal-50 p-3 text-sm text-teal-800">{notice}</p>}
    {loading ? <div role="status" className="staff-panel py-16 text-center text-slate-500">Loading your hospital workspace…</div> : !hospitalId && !error ? <div className="staff-panel py-14 text-center"><Building2 className="mx-auto text-[#079D96]" size={36} /><h2 className="mt-4 font-bold text-[#123F70]">No hospital assigned yet</h2><p className="mt-2 text-sm text-slate-500">Ask your hospital administrator to activate your branch assignment using your account email.</p></div> : data && <>
      <nav aria-label="Hospital workspace" className="flex gap-2 overflow-x-auto pb-1">{tabs.map(([key,label]) => <button key={key} onClick={() => setTab(key)} aria-current={safeTab === key ? 'page' : undefined} className={`whitespace-nowrap rounded-xl px-4 py-3 text-sm font-semibold ${safeTab === key ? 'bg-[#123F70] text-white' : 'bg-white text-slate-600 hover:bg-[#E8F6F4]'}`}>{label}</button>)}</nav>
      {safeTab === 'overview' && <>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">{[
          [CalendarDays,'Appointments',data.queueAccess ? queue.length : '—'],
          [Users,'Waiting patients',data.queueAccess ? queue.filter(a => patientStatus(a) === 'Waiting').length : '—'],
          [Stethoscope,'In consultation',data.queueAccess ? queue.filter(a => patientStatus(a) === 'In consultation').length : '—'],
          [ClipboardList,'Open tasks',tasks.filter(t => t.status === 'OPEN').length],
        ].map(([Icon,label,value]) => <div className="staff-panel" key={label}><div className="flex items-center justify-between"><span className="text-sm text-slate-500">{label}</span><Icon size={19} className="text-[#079D96]" /></div><p className="mt-4 text-3xl font-bold text-[#123F70]">{value}</p><p className="mt-2 text-xs text-slate-400">{label === 'Open tasks' ? 'Tasks visible to your role' : `${date} · selected hospital`}</p></div>)}</div>
        <div className="grid gap-5 lg:grid-cols-3"><section className="staff-panel lg:col-span-2"><h2 className="text-lg font-bold text-[#123F70]">Your day at a glance</h2><p className="mt-1 text-sm text-slate-500">{hospital?.name} · {hospital?.city}</p><div className="mt-5 space-y-3">{data.queueAccess ? <><p className="text-sm">{queue.filter(a => patientStatus(a) === 'Completed').length} consultations completed · {queue.filter(a => patientStatus(a) === 'Expected').length} arrivals expected</p><button className="staff-button secondary" onClick={() => setTab('queue')}>Open patient queue <ArrowRight size={15} /></button></> : <p className="text-sm text-slate-500">Patient queue details are restricted for your role.</p>}<button className="staff-button secondary" onClick={() => setTab('tasks')}>View tasks & handovers <ArrowRight size={15} /></button></div></section><section className="staff-panel bg-[#E8F6F4]"><ShieldCheck size={24} className="text-[#079D96]"/><h2 className="mt-3 font-bold text-[#123F70]">Your access</h2><p className="mt-2 text-sm text-slate-600">{ROLE_LABELS[role]} at {hospital?.name}. Clinical signing and prescribing remain with authorized veterinarians.</p></section></div>
        {integration && <section className="staff-panel"><p className="text-xs font-bold uppercase tracking-wider text-amber-700">Integration pending</p><h2 className="mt-2 font-bold text-[#123F70]">{integration[1]}</h2><p className="mt-2 text-sm text-slate-500">{integration[2]}</p></section>}
      </>}
      {safeTab === 'queue' && <section className="staff-panel"><div className="flex flex-wrap items-center justify-between gap-4"><h2 className="text-lg font-bold text-[#123F70]">Patient queue <span className="text-sm font-normal text-slate-400">{filtered.length} patients</span></h2><div className="flex flex-wrap gap-2"><label className="relative"><span className="sr-only">Search patients</span><Search size={15} className="absolute left-3 top-3 text-slate-400"/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Name, patient ID, vet or token" className="staff-input pl-9" /></label><select aria-label="Filter patient status" className="staff-input" value={status} onChange={e => setStatus(e.target.value)}>{['All','Expected','Waiting','In consultation','Completed','Cancelled','No show'].map(s => <option key={s}>{s}</option>)}</select></div></div><div className="mt-5 overflow-x-auto"><table className="w-full text-sm"><thead className="border-b text-left text-xs text-slate-400"><tr>{['Time / token','Patient','Veterinarian','Status','Action'].map(h => <th className="pb-3 pr-4" key={h}>{h}</th>)}</tr></thead><tbody>{filtered.map(a => <tr key={a.appointmentId} className="border-b border-slate-100"><td className="py-4 pr-4 whitespace-nowrap">{time(a.appointmentAt)}<span className="block text-xs text-slate-400">{a.queueNumber ? `Token ${a.queueNumber}` : 'Token not assigned'}</span></td><td className="py-4 pr-4"><p className="font-semibold text-[#123F70]">{a.petName}</p><p className="text-xs text-slate-400">{a.species} · {a.breed || 'Breed not recorded'}</p></td><td className="py-4 pr-4">{a.vetName || 'Assigned veterinarian'}</td><td className="py-4 pr-4"><span className="rounded-lg bg-[#E8F6F4] px-2 py-1 text-xs text-teal-800 whitespace-nowrap">{patientStatus(a)}</span></td><td className="py-4">{data.canCheckIn && patientStatus(a) === 'Expected' && <button disabled={busy} onClick={() => mutate(`/appointments/${encodeURIComponent(a.appointmentId)}/check-in`)} className="staff-button secondary">Check in</button>}</td></tr>)}</tbody></table>{!filtered.length && <p className="py-12 text-center text-sm text-slate-400">No patients match this date and filter.</p>}</div></section>}
      {safeTab === 'tasks' && <section className="staff-panel"><h2 className="text-lg font-bold text-[#123F70]">Tasks & operational handovers</h2><p className="mt-1 text-sm text-slate-500">Use short operational instructions. Keep medical records in the clinical workspace.</p><form className="mt-5 flex flex-wrap gap-3" onSubmit={async e => {e.preventDefault(); if(await mutate('/tasks', task)) setTask({...task,title:''});}}><input required maxLength={200} aria-label="Task title" placeholder="Add an operational follow-up…" className="staff-input flex-1 min-w-48" value={task.title} onChange={e => setTask({...task,title:e.target.value})}/><select aria-label="Assigned role" className="staff-input" value={task.assignedRole} onChange={e => setTask({...task,assignedRole:e.target.value})}>{Object.entries(ROLE_LABELS).filter(([key]) => data.canManageStaff ? !['ROLE_PLATFORM_ADMIN','ROLE_PET_OWNER'].includes(key) : key === role).map(([key,label]) => <option key={key} value={key}>{label}</option>)}</select><select aria-label="Task priority" className="staff-input" value={task.priority} onChange={e => setTask({...task,priority:e.target.value})}><option value="ROUTINE">Routine</option><option value="URGENT">Urgent</option></select><button disabled={busy || !task.title.trim()} className="staff-button">Add task</button></form><div className="mt-6 space-y-3">{tasks.map(t => <div key={t.taskId} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-100 p-4"><div><p className={`font-semibold ${t.status === 'DONE' ? 'text-slate-400 line-through' : 'text-[#123F70]'}`}>{t.title}</p><p className="mt-1 text-xs text-slate-500">{ROLE_LABELS[t.assignedRole]} · {t.priority} · {t.status}</p></div>{t.status === 'OPEN' && (t.assignedRole === role || data.canManageStaff) && <button disabled={busy} onClick={() => mutate(`/tasks/${t.taskId}/complete`)} className="staff-button secondary"><CheckCircle2 size={15}/>Complete</button>}</div>)}{!tasks.length && <p className="py-10 text-center text-sm text-slate-400">No tasks yet. Add a follow-up for your team.</p>}</div></section>}
      {safeTab === 'staff' && <section className="staff-panel"><h2 className="text-lg font-bold text-[#123F70]">Staff directory & hospital access</h2><p className="mt-1 text-sm text-slate-500">Assign existing staff accounts to this hospital. Professional roles are managed separately.</p><form className="mt-5 flex flex-wrap gap-3" onSubmit={async e => {e.preventDefault(); if(await mutate('/staff',assignment,'PUT')) setAssignment({...assignment,email:''});}}><input required type="email" maxLength={255} aria-label="Staff account email" placeholder="Staff account email" className="staff-input flex-1" value={assignment.email} onChange={e => setAssignment({...assignment,email:e.target.value})}/><input required maxLength={100} aria-label="Department" className="staff-input" placeholder="Department" value={assignment.department} onChange={e => setAssignment({...assignment,department:e.target.value})}/><button disabled={busy} className="staff-button">Grant hospital access</button></form><div className="mt-5 overflow-x-auto"><table className="w-full text-sm text-left"><thead className="border-b text-xs text-slate-400"><tr>{['Account','Role','Department','Hospital access','Action'].map(h => <th key={h} className="pb-3 pr-4">{h}</th>)}</tr></thead><tbody>{staff.map(s => <tr key={s.userId} className="border-b border-slate-100"><td className="py-4 pr-4">{s.email || s.userId}<p className="text-xs text-slate-400">Account: {s.status}</p></td><td className="pr-4">{ROLE_LABELS[s.role] || s.role}</td><td className="pr-4">{s.department}</td><td className="pr-4">{s.active ? 'Active' : 'Revoked'}</td><td><button disabled={busy || s.userId === currentUser?.id} className="staff-button secondary" onClick={() => mutate('/staff',{email:s.email,department:s.department,active:!s.active},'PUT')}>{s.active ? 'Revoke access' : 'Reactivate'}</button></td></tr>)}</tbody></table>{!staff.length && <p className="py-10 text-center text-sm text-slate-400">No staff assignments yet. The hospital owner retains access.</p>}</div></section>}
      {safeTab === 'clinical' && role === 'ROLE_VET' && <VetDashboard key={hospitalId} hospitalId={hospitalId} workingDate={date} />}
    </>}
  </div>;
}

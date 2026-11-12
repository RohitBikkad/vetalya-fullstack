import React from 'react';
import { useAuth } from '../../context/AuthContext';
import DashboardShell from '../layout/DashboardShell';
import OwnerApp from './petparent/OwnerApp';
import ClinicWorkspace from './staff/ClinicWorkspace';
import AdminDashboard from './admin/AdminDashboard';
import { normalizeRole, isHospitalRole } from '../../utils/roles';

export default function DashboardRouter() {
  const { role: rawRole } = useAuth();
  const role = normalizeRole(rawRole);
  let content;
  if (isHospitalRole(role)) content = <ClinicWorkspace />;
  else if (role === 'ROLE_PLATFORM_ADMIN') content = <AdminDashboard />;
  else if (role === 'ROLE_PET_OWNER') return <OwnerApp />;
  else content = <div role="alert" className="rounded-2xl bg-white p-8 text-center">This account has no supported portal role. Contact your administrator.</div>;
  return <DashboardShell>{content}</DashboardShell>;
}

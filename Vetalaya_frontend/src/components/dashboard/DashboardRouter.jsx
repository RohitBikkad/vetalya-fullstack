import React from 'react';
import { useAuth } from '../../context/AuthContext';
import DashboardShell from '../layout/DashboardShell';
import PetParentDashboard from './petparent/PetParentDashboard';
import VetDashboard from './vet/VetDashboard';
import AdminDashboard from './admin/AdminDashboard';

export default function DashboardRouter() {
  const { role } = useAuth();

  // Strict role-gated routing based on authenticated identity
  const renderCurrentDashboard = () => {
    switch (role) {
      case 'ROLE_VET':
      case 'VETERINARIAN':
        return <VetDashboard />;
      case 'ROLE_PLATFORM_ADMIN':
      case 'ROLE_CLINIC_ADMIN':
      case 'PLATFORM_ADMIN':
      case 'CLINIC_ADMIN':
        return <AdminDashboard />;
      case 'ROLE_PET_OWNER':
      case 'PET_PARENT':
      default:
        return <PetParentDashboard />;
    }
  };

  return (
    <DashboardShell>
      {renderCurrentDashboard()}
    </DashboardShell>
  );
}

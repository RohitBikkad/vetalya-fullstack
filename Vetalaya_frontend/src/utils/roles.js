export const ROLE_LABELS = {
  ROLE_VET: 'Veterinarian', ROLE_VET_NURSE: 'Veterinary Nurse', ROLE_LAB_TECHNICIAN: 'Lab Technician',
  ROLE_RECEPTIONIST: 'Receptionist', ROLE_PHARMACIST: 'Pharmacist', ROLE_ACCOUNTANT: 'Accountant',
  ROLE_CLINIC_STAFF: 'Hospital Staff', ROLE_CLINIC_ADMIN: 'Clinic Administrator', ROLE_CLINIC_MANAGER: 'Clinic Manager',
  ROLE_PLATFORM_ADMIN: 'Platform Administrator', ROLE_PET_OWNER: 'Pet Parent',
};
export function normalizeRole(role) {
  const aliases = { VETERINARIAN: 'ROLE_VET', PET_PARENT: 'ROLE_PET_OWNER', VET_NURSE: 'ROLE_VET_NURSE' };
  if (!role) return '';
  return aliases[role] || (role.startsWith('ROLE_') ? role : `ROLE_${role}`);
}
export function isHospitalRole(role) {
  const value = normalizeRole(role);
  return Boolean(ROLE_LABELS[value]) && !['ROLE_PLATFORM_ADMIN', 'ROLE_PET_OWNER'].includes(value);
}

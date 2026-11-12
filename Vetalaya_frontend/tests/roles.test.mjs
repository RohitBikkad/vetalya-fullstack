import test from 'node:test';
import assert from 'node:assert/strict';
import { normalizeRole, isHospitalRole, ROLE_LABELS } from '../src/utils/roles.js';

test('every hospital staff role reaches the staff workspace', () => {
  for (const role of Object.keys(ROLE_LABELS).filter(role => !['ROLE_PET_OWNER','ROLE_PLATFORM_ADMIN'].includes(role))) assert.equal(isHospitalRole(role),true,role);
});
test('legacy roles normalize without granting unknown roles a portal', () => {
  assert.equal(normalizeRole('VETERINARIAN'),'ROLE_VET');
  assert.equal(normalizeRole('CLINIC_ADMIN'),'ROLE_CLINIC_ADMIN');
  for (const role of ['',null,'ROLE_UNKNOWN','ROLE_PLATFORM_ADMIN','ROLE_PET_OWNER']) assert.equal(isHospitalRole(role),false);
});

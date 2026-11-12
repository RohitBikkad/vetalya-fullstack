/**
 * Vetalya API Client Service
 * Interacts with Spring Boot backend at http://localhost:8080 (or through Vite /api proxy)
 */

const getApiBaseUrl = () => {
  const envUrl = import.meta.env.VITE_API_BASE_URL;
  if (envUrl && envUrl.startsWith('http')) {
    return envUrl;
  }
  if (typeof window !== 'undefined' && window.location.hostname.includes('onrender.com')) {
    return 'https://vetalya-fullstack.onrender.com/api';
  }
  return envUrl || 'https://vetalya-fullstack.onrender.com/api';
};

const API_BASE_URL = getApiBaseUrl();

/**
 * Universal request wrapper handling JWT injection and JSON parsing
 */
export async function request(endpoint, options = {}) {
  const token = localStorage.getItem('vetalya_jwt_token');

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const config = {
    credentials: 'include',
    ...options,
    headers,
  };

  const url = `${API_BASE_URL}${endpoint}`;
  const response = await fetch(url, config);

  if (response.status === 401) {
    // If token is invalid or expired, clear local storage
    if (endpoint !== '/auth/login' && endpoint !== '/auth/google') {
      localStorage.removeItem('vetalya_jwt_token');
      localStorage.removeItem('vetalya_auth_user');
    }
  }

  const data = await response.json().catch(() => null);

  if (!response.ok || !data) {
    const errorMessage =
      data?.message ||
      (data?.details ? Object.values(data.details).join(', ') : null) ||
      (!data ? `Failed to connect to backend at ${API_BASE_URL} (Status ${response.status}).` : 'Request failed');
    throw new Error(errorMessage);
  }

  return data;
}

export async function logoutApi() {
  try {
    await request('/auth/logout', { method: 'POST' });
  } catch (e) {
    console.warn('Backend logout failed or not available:', e);
  }
}

export async function exchangeCodeApi(code) {
  return request('/auth/exchange', {
    method: 'POST',
    body: JSON.stringify({ code }),
  });
}

export async function loginApi(email, password, rememberMe = true) {
  return request('/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password, rememberMe }),
  });
}

export async function registerApi(data, emailArg, passwordArg, roleArg) {
  let body;
  if (typeof data === 'object' && data !== null) {
    body = data;
  } else {
    body = {
      fullName: data,
      email: emailArg,
      password: passwordArg,
      role: roleArg || 'ROLE_PET_OWNER',
    };
  }
  return request('/auth/register', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

export async function sendOtpApi(target, purpose = 'REGISTRATION') {
  return request('/auth/otp/send', {
    method: 'POST',
    body: JSON.stringify({ target, purpose }),
  });
}

export async function verifyOtpApi(target, otp, purpose = 'REGISTRATION') {
  return request('/auth/otp/verify', {
    method: 'POST',
    body: JSON.stringify({ target, otp, purpose }),
  });
}

export async function googleAuthApi(idToken, role = 'PET_PARENT') {
  return request('/auth/google', {
    method: 'POST',
    body: JSON.stringify({ idToken, role }),
  });
}

export async function demoLoginApi(role = 'PET_PARENT') {
  return request('/auth/demo-login', {
    method: 'POST',
    body: JSON.stringify({ role }),
  });
}

export async function getCurrentUserApi() {
  try {
    return await request('/users/profile', { method: 'GET' });
  } catch {
    return await request('/auth/me', { method: 'GET' });
  }
}

export async function getPetsApi(page = 0, size = 20) {
  return request(`/owner/pets?page=${page}&size=${size}`);
}

export async function createPetApi(petData) {
  return request('/owner/pets', {
    method: 'POST',
    body: JSON.stringify(petData),
  });
}

export async function getOwnerDashboardApi() {
  return request('/owner/dashboard');
}

export async function getOwnerAppointmentsApi(page = 0, size = 20) {
  return request(`/owner/appointments?page=${page}&size=${size}`);
}

export async function bookOwnerAppointmentApi(bookingData) {
  return request('/owner/appointments', {
    method: 'POST',
    body: JSON.stringify(bookingData),
  });
}

export async function searchVetsApi(pincode = '') {
  return request(`/vets/search${pincode ? `?pincode=${encodeURIComponent(pincode)}` : ''}`);
}

export async function getDueVaccinationsApi(days = 30) {
  return request(`/owner/vaccinations/due?days=${days}`);
}

export async function getPetVaccinationsApi(petId) {
  return request(`/owner/pets/${petId}/vaccinations`);
}

export async function getVetAppointmentsApi(page = 0, size = 20) {
  return request(`/vet/appointments?page=${page}&size=${size}`);
}

export async function getVetDashboardApi() {
  return request('/vet/dashboard');
}

export async function getVetProfileApi() {
  return request('/vet/profile');
}

export async function getVetHospitalsApi() {
  return request('/vet/hospitals');
}

export async function updateVetAppointmentStatusApi(appointmentId, status, notes = '') {
  return request(`/vet/appointments/${appointmentId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status, notes }),
  });
}

export async function updateVetVisitNotesApi(appointmentId, visitNotes) {
  return request(`/vet/appointments/${appointmentId}/notes`, {
    method: 'PUT',
    body: JSON.stringify({ visitNotes }),
  });
}

// ── Admin Platform Management APIs (Live DB) ──

export async function getAdminKpisApi() {
  return request('/admin/kpis');
}

export async function getAdminVerificationsApi() {
  return request('/admin/verifications');
}

export async function updateDoctorVerificationStatusApi(vetUserId, verified) {
  return request(`/admin/verifications/${vetUserId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ verified }),
  });
}

export async function getAdminUsersApi() {
  return request('/admin/users');
}

export async function updateUserStatusApi(userId, status) {
  return request(`/admin/users/${userId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}

export async function getDatabaseInspectionApi() {
  return request('/public/db-inspect');
}

export async function cancelOwnerAppointmentApi(appointmentId, reason = '') {
  return request(`/owner/appointments/${appointmentId}/cancel`, {
    method: 'PATCH',
    body: JSON.stringify({ reason }),
  });
}

// ── Universal Pet Health Passport & Medical Records Sharing APIs ──

export async function getPetPassportApi(petId) {
  return request(`/v1/pets/${petId}/passport`);
}

export async function updatePetPassportApi(petId, data) {
  return request(`/v1/pets/${petId}/passport`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
}

export async function getPetTimelineApi(petId) {
  return request(`/v1/pets/${petId}/passport/timeline`);
}

export async function createShareRequestApi(petId, targetHospitalId, scope = 'FULL_TIMELINE', expiryHours = 72) {
  return request(`/v1/pets/${petId}/passport/share-requests`, {
    method: 'POST',
    body: JSON.stringify({ petId, targetHospitalId, scope, expiryHours }),
  });
}

export async function approveShareRequestApi(requestId) {
  return request(`/v1/pets/share-requests/${requestId}/approve`, {
    method: 'POST',
  });
}

export async function revokeShareRequestApi(requestId) {
  return request(`/v1/pets/share-requests/${requestId}/revoke`, {
    method: 'POST',
  });
}

export async function getPetDocumentsApi(petId) {
  return request(`/v1/pets/${petId}/documents`);
}

export async function uploadPetDocumentApi(petId, documentData) {
  return request(`/v1/pets/${petId}/documents`, {
    method: 'POST',
    body: JSON.stringify(documentData),
  });
}

export async function getEmergencyProfileApi(qrHash) {
  return request(`/v1/pets/emergency/${qrHash}`);
}

// ── Clinical Encounters & SOAP Records APIs ──

export async function createEncounterApi(encounterData) {
  return request('/v1/encounters', {
    method: 'POST',
    body: JSON.stringify(encounterData),
  });
}

export async function getEncounterApi(encounterId) {
  return request(`/v1/encounters/${encounterId}`);
}

export async function updateDraftEncounterApi(encounterId, encounterData) {
  return request(`/v1/encounters/${encounterId}`, {
    method: 'PUT',
    body: JSON.stringify(encounterData),
  });
}

export async function signEncounterApi(encounterId) {
  return request(`/v1/encounters/${encounterId}/sign`, {
    method: 'POST',
  });
}

export async function amendEncounterApi(encounterId, amendmentData) {
  return request(`/v1/encounters/${encounterId}/amend`, {
    method: 'POST',
    body: JSON.stringify(amendmentData),
  });
}

export async function getPetEncountersApi(petId) {
  return request(`/v1/encounters/pet/${petId}`);
}

// ── Appointment Slots & Concurrency APIs ──

export async function generateSlotsApi(slotConfig) {
  return request('/v1/slots/generate', {
    method: 'POST',
    body: JSON.stringify(slotConfig),
  });
}

export async function getAvailableSlotsApi(vetUserId, fromDate, toDate) {
  const params = new URLSearchParams();
  if (vetUserId) params.append('vetUserId', vetUserId);
  if (fromDate) params.append('fromDate', fromDate);
  if (toDate) params.append('toDate', toDate);
  return request(`/v1/slots/available?${params.toString()}`);
}

export async function bookSlotApi(bookingData) {
  return request('/v1/slots/book', {
    method: 'POST',
    body: JSON.stringify(bookingData),
  });
}

export async function checkInAppointmentApi(appointmentId) {
  return request(`/v1/slots/appointments/${appointmentId}/check-in`, {
    method: 'POST',
  });
}

export async function startConsultationApi(appointmentId) {
  return request(`/v1/slots/appointments/${appointmentId}/start-consultation`, {
    method: 'POST',
  });
}

export async function endConsultationApi(appointmentId) {
  return request(`/v1/slots/appointments/${appointmentId}/end-consultation`, {
    method: 'POST',
  });
}

// ── Diagnostics & Laboratory APIs ──

export async function createDiagnosticOrderApi(orderData) {
  return request('/v1/diagnostics/orders', {
    method: 'POST',
    body: JSON.stringify(orderData),
  });
}

export async function getDiagnosticOrderApi(orderId) {
  return request(`/v1/diagnostics/orders/${orderId}`);
}

export async function getPetDiagnosticsApi(petId) {
  return request(`/v1/diagnostics/pet/${petId}`);
}

export async function updateDiagnosticResultApi(itemId, resultData) {
  return request(`/v1/diagnostics/items/${itemId}/results`, {
    method: 'PUT',
    body: JSON.stringify(resultData),
  });
}

export async function getDiagnosticCatalogApi(category) {
  const query = category ? `?category=${encodeURIComponent(category)}` : '';
  return request(`/v1/diagnostics/catalog${query}`);
}


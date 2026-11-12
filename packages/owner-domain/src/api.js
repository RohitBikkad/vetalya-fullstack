import { normalizeSpecies } from "./model.js";
/** Body values are objects; transport adapters handle serialization and authentication. */
export function createOwnerRepository(request) {
  const id = (value) => encodeURIComponent(value);
  async function pages(path) {
    let page = 0;
    let result = [];
    while (true) {
      const data = await request(
        `${path}${path.includes("?") ? "&" : "?"}page=${page}&size=50`,
      );
      if (Array.isArray(data)) return data;
      result = result.concat(data?.content || []);
      if (++page >= (data?.totalPages || 1)) return result;
    }
  }
  return {
    pets: async () =>
      (await pages("/owner/pets")).map((p) => ({
        ...p,
        species: normalizeSpecies(p.species),
      })),
    appointments: () => pages("/owner/appointments"),
    due: () => request("/owner/vaccinations/due?days=60"),
    vets: (pincode) =>
      request("/vets/search" + (pincode ? `?pincode=${id(pincode)}` : "")),
    savePet: (data, petId) =>
      request("/owner/pets" + (petId ? `/${id(petId)}` : ""), {
        method: petId ? "PUT" : "POST",
        body: data,
      }),
    passport: (petId) => request(`/v1/pets/${id(petId)}/passport`),
    savePassport: (petId, data) =>
      request(`/v1/pets/${id(petId)}/passport`, { method: "PUT", body: data }),
    encounters: (petId) => request(`/v1/encounters/pet/${id(petId)}`),
    timeline: (petId) => request(`/v1/pets/${id(petId)}/passport/timeline`),
    documents: (petId) => request(`/v1/pets/${id(petId)}/documents`),
    vaccines: (petId) => request(`/owner/pets/${id(petId)}/vaccinations`),
    slots: (vetId, date) =>
      request(
        `/v1/slots/available?vetUserId=${id(vetId)}&fromDate=${id(date)}&toDate=${id(date)}`,
      ),
    book: (data) => request("/v1/slots/book", { method: "POST", body: data }),
    cancel: (appointmentId, reason) =>
      request(`/owner/appointments/${id(appointmentId)}/cancel`, {
        method: "PATCH",
        body: { reason },
      }),
    profile: () => request("/owner/profile"),
    saveProfile: (data) =>
      request("/owner/profile", { method: "PUT", body: data }),
    shares: (petId) => request(`/v1/pets/${id(petId)}/passport/share-requests`),
    share: (petId, data) =>
      request(`/v1/pets/${id(petId)}/passport/share-requests`, {
        method: "POST",
        body: { ...data, petId },
      }),
    approve: (requestId) =>
      request(`/v1/pets/share-requests/${id(requestId)}/approve`, {
        method: "POST",
      }),
    revoke: (requestId) =>
      request(`/v1/pets/share-requests/${id(requestId)}/revoke`, {
        method: "POST",
      }),
  };
}

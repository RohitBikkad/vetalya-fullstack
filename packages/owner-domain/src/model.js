/** @type {Record<string, string>} */
export const SPECIES = {
  DOG: "Dog",
  CAT: "Cat",
  BIRD: "Bird",
  RABBIT: "Rabbit",
  HORSE: "Horse",
  CATTLE: "Cattle",
  BUFFALO: "Buffalo",
  SHEEP: "Sheep",
  GOAT: "Goat",
  OTHER: "Other",
};
export const dateLabel = (value) =>
  value
    ? new Intl.DateTimeFormat("en-IN", {
        day: "numeric",
        month: "short",
        year: "numeric",
        timeZone: "Asia/Kolkata",
      }).format(new Date(value))
    : "Not recorded";
export const timeLabel = (value) =>
  value
    ? new Intl.DateTimeFormat("en-IN", {
        hour: "2-digit",
        minute: "2-digit",
        timeZone: "Asia/Kolkata",
      }).format(new Date(value))
    : "";
export const todayIST = () =>
  new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kolkata" }).format(
    new Date(),
  );
export function upcomingAppointments(appointments, now = Date.now()) {
  return appointments
    .filter(
      (a) =>
        ["BOOKED", "CONFIRMED", "IN_CONSULTATION"].includes(a.status) &&
        new Date(a.appointmentAt).getTime() >= now,
    )
    .sort((a, b) => new Date(a.appointmentAt) - new Date(b.appointmentAt));
}
export function appointmentLabel(status) {
  return (
    {
      BOOKED: "Booked",
      CONFIRMED: "Confirmed",
      IN_CONSULTATION: "In consultation",
      COMPLETED: "Completed",
      CANCELLED: "Cancelled",
      NO_SHOW: "No show",
    }[status] ||
    status ||
    "Unknown"
  );
}
export function petPayload(form) {
  if (!form.name?.trim()) throw new Error("Your pet’s name is required.");
  if (!SPECIES[form.species]) throw new Error("Choose a supported species.");
  if (
    form.dob &&
    (form.dob > todayIST() || !/^\d{4}-\d{2}-\d{2}$/.test(form.dob))
  )
    throw new Error("Enter a date of birth that is not in the future.");
  if (form.dob) {
    const date = new Date(`${form.dob}T00:00:00Z`);
    if (
      !Number.isFinite(date.getTime()) ||
      date.toISOString().slice(0, 10) !== form.dob
    )
      throw new Error("Enter a valid calendar date.");
  }
  return {
    name: form.name.trim(),
    species: form.species,
    gender: form.gender || "UNKNOWN",
    breed: form.breed?.trim() || null,
    dob: form.dob || null,
    photoUrl: form.photoUrl || null,
    registrationNo: form.registrationNo || null,
  };
}
export function safeExternalUrl(value) {
  if (!value) return null;
  try {
    const url = new URL(value);
    return url.protocol === "https:" && !url.username && !url.password
      ? url.href
      : null;
  } catch {
    return null;
  }
}
export function profilePayload(form) {
  if (!form.firstName?.trim() || !form.lastName?.trim())
    throw new Error("First and last name are required.");
  if (form.pincode && !/^\d{6}$/.test(form.pincode))
    throw new Error("Pincode must contain six digits.");
  return Object.fromEntries(
    [
      "firstName",
      "lastName",
      "address1",
      "address2",
      "pincode",
      "city",
      "state",
      "country",
    ].map((key) => [key, form[key]?.trim() || null]),
  );
}

export const PLANNED_SERVICES = [
  {
    name: "Grooming",
    description: "Baths, coat care and everyday comfort.",
    icon: "scissors",
  },
  {
    name: "Dog walking",
    description: "Movement and companionship for their daily routine.",
    icon: "sun",
  },
  {
    name: "Boarding",
    description: "A place to stay when you are away.",
    icon: "home",
  },
  {
    name: "Training",
    description: "Support for learning and growing together.",
    icon: "award",
  },
  {
    name: "Insurance",
    description: "Policy discovery and document support.",
    icon: "shield",
  },
];
export function availableFutureSlots(slots, hospitalId, now = Date.now()) {
  return slots.filter(
    (slot) =>
      slot.hospitalId === hospitalId &&
      slot.status === "AVAILABLE" &&
      slot.bookedCount < slot.capacity &&
      new Date(`${slot.slotDate}T${slot.startTime}+05:30`).getTime() > now,
  );
}

export function normalizeSpecies(value) {
  const aliases = {
    CANINE: "DOG",
    FELINE: "CAT",
    AVIAN: "BIRD",
    EQUINE: "HORSE",
    "BOVINE (CATTLE)": "CATTLE",
    "BOVINE (BUFFALO)": "BUFFALO",
  };
  const key = String(value || "").toUpperCase();
  return aliases[key] || key;
}

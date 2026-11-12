import React, { useState, useEffect, useCallback, useRef, useId } from "react";
import {
  Home,
  PawPrint,
  CalendarDays,
  FolderHeart,
  HeartPulse,
  UserRound,
  Plus,
  ArrowUpRight,
  ArrowRight,
  ChevronRight,
  ChevronDown,
  Search,
  Bell,
  LogOut,
  ShieldCheck,
  Stethoscope,
  Clock3,
  FileText,
  Syringe,
  X,
  RefreshCw,
  MapPin,
  Phone,
  Check,
  Share2,
  CalendarCheck2,
  Menu,
  AlertCircle,
  ExternalLink,
  Heart,
} from "lucide-react";
import { useAuth } from "../../../context/AuthContext";
import { ownerApi as api } from "./ownerApi";
import {
  SPECIES,
  dateLabel,
  timeLabel,
  todayIST,
  upcomingAppointments,
  appointmentLabel,
  petPayload,
  profilePayload,
  safeExternalUrl,
  PLANNED_SERVICES,
  availableFutureSlots,
} from "../../../../../packages/owner-domain/src/model.js";
import "./owner.css";

const navigation = [
  ["home", "Home", Home],
  ["pets", "My pets", PawPrint],
  ["appointments", "Appointments", CalendarDays],
  ["records", "Health records", FolderHeart],
  ["care", "Care reminders", HeartPulse],
  ["services", "Services", Heart],
  ["profile", "My profile", UserRound],
];
function Avatar({ pet, large = false }) {
  return (
    <div className={`owner-pet-avatar ${large ? "large" : ""}`}>
      {safeExternalUrl(pet?.photoUrl) ? (
        <img
          src={pet.photoUrl}
          alt={pet.name}
          onError={(e) => {
            e.currentTarget.style.display = "none";
          }}
        />
      ) : (
        <PawPrint aria-hidden="true" />
      )}
    </div>
  );
}
function Empty({ icon: Icon = PawPrint, title, children, action }) {
  return (
    <div className="owner-empty">
      <span>
        <Icon size={27} />
      </span>
      <h3>{title}</h3>
      <p>{children}</p>
      {action}
    </div>
  );
}
function Badge({ children, tone = "teal" }) {
  return <span className={`owner-badge ${tone}`}>{children}</span>;
}
function Modal({ title, subtitle, onClose, busy, children }) {
  const panel = useRef();
  const heading = useId();
  useEffect(() => {
    const previous = document.activeElement;
    const old = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    panel.current?.querySelector("input,select,textarea,button")?.focus();
    return () => {
      document.body.style.overflow = old;
      previous?.focus?.();
    };
  }, []);
  function keydown(e) {
    if (e.key === "Escape" && !busy) onClose();
    if (e.key === "Tab") {
      const nodes = [
        ...panel.current.querySelectorAll(
          "button:not(:disabled),input:not(:disabled),select:not(:disabled),textarea:not(:disabled),a[href]",
        ),
      ];
      const first = nodes[0],
        last = nodes.at(-1);
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last?.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first?.focus();
      }
    }
  }
  return (
    <div className="owner-modal-backdrop">
      <section
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={heading}
        className="owner-modal"
        onKeyDown={keydown}
      >
        <header>
          <div>
            <span className="owner-eyebrow">VETALYA · PET CARE</span>
            <h2 id={heading}>{title}</h2>
            {subtitle && <p>{subtitle}</p>}
          </div>
          <button
            aria-label="Close dialog"
            disabled={busy}
            className="owner-icon-btn"
            onClick={onClose}
          >
            <X />
          </button>
        </header>
        <div className="owner-modal-body">{children}</div>
      </section>
    </div>
  );
}
function Field({ label, children, wide = false }) {
  const id = useId();
  const control =
    React.isValidElement(children) &&
    ["input", "select", "textarea"].includes(children.type);
  return control ? (
    <div className={`owner-field ${wide ? "wide" : ""}`}>
      <label htmlFor={id}>
        <span>{label}</span>
      </label>
      {React.cloneElement(children, { id })}
    </div>
  ) : (
    <div
      className={`owner-field ${wide ? "wide" : ""}`}
      role="group"
      aria-label={label}
    >
      <span>{label}</span>
      {children}
    </div>
  );
}

const newPet = {
  name: "",
  species: "DOG",
  gender: "UNKNOWN",
  breed: "",
  dob: "",
  photoUrl: "",
};
export default function OwnerApp() {
  const { currentUser, logout, refreshUserProfile } = useAuth();
  const [page, setPage] = useState(
    () => window.location.hash.split("/")[1] || "home",
  );
  const [pets, setPets] = useState([]),
    [appointments, setAppointments] = useState([]),
    [due, setDue] = useState([]),
    [profile, setProfile] = useState(null);
  const [petId, setPetId] = useState(""),
    [loading, setLoading] = useState(true),
    [errors, setErrors] = useState([]),
    [notice, setNotice] = useState("");
  const [details, setDetails] = useState({}),
    [detailLoading, setDetailLoading] = useState(false),
    [detailErrors, setDetailErrors] = useState([]);
  const [modal, setModal] = useState(null),
    [form, setForm] = useState({}),
    [busy, setBusy] = useState(false),
    [formError, setFormError] = useState("");
  const [query, setQuery] = useState(""),
    [filter, setFilter] = useState("all"),
    [menu, setMenu] = useState(false);
  const [vets, setVets] = useState([]),
    [slots, setSlots] = useState([]),
    [loadingVets, setLoadingVets] = useState(false),
    [loadingSlots, setLoadingSlots] = useState(false);
  const shares = details.shares || [];
  const pet = pets.find((p) => p.petId === petId) || pets[0];
  const upcoming = upcomingAppointments(appointments);
  const next = upcoming.find((a) => !pet || a.pet?.petId === pet.petId) || null;
  const passport = details.passport;
  const timeline = details.timeline || [];
  const documents = details.documents || [];
  const vaccines = details.vaccines || [];
  function navigate(value) {
    window.location.hash = `owner/${value}`;
    setPage(value);
    setQuery("");
    setFilter("all");
    setMenu(false);
  }
  useEffect(() => {
    const onHash = () => {
      const value = window.location.hash.split("/")[1];
      setPage(
        navigation.some((n) => n[0] === value) || value === "passport"
          ? value
          : "home",
      );
    };
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);
  const load = useCallback(async () => {
    setLoading(true);
    setErrors([]);
    const results = await Promise.allSettled([
      api.pets(),
      api.appointments(),
      api.due(),
      api.profile(),
    ]);
    const failures = [];
    const setters = [setPets, setAppointments, setDue, setProfile];
    results.forEach((r, i) => {
      if (r.status === "fulfilled") setters[i](r.value);
      else
        failures.push(
          `${["Pets", "Appointments", "Care reminders", "Profile"][i]}: ${r.reason.message}`,
        );
    });
    setErrors(failures);
    setLoading(false);
  }, []);
  useEffect(() => {
    load();
  }, [load]);
  useEffect(() => {
    if (pets.length && !pets.some((p) => p.petId === petId))
      setPetId(pets[0].petId);
  }, [pets, petId]);
  const loadDetails = useCallback(async () => {
    if (!pet?.petId) {
      setDetails({});
      return;
    }
    let active = true;
    setDetailLoading(true);
    setDetailErrors([]);
    setDetails({});
    const results = await Promise.allSettled([
      api.passport(pet.petId),
      api.timeline(pet.petId),
      api.documents(pet.petId),
      api.vaccines(pet.petId),
      api.encounters(pet.petId),
      api.shares(pet.petId),
    ]);
    if (active) {
      const value = {},
        failures = [];
      results.forEach((r, i) => {
        const key = [
          "passport",
          "timeline",
          "documents",
          "vaccines",
          "encounters",
          "shares",
        ][i];
        if (r.status === "fulfilled") value[key] = r.value;
        else failures.push(`${key}: ${r.reason.message}`);
      });
      setDetails(value);
      setDetailErrors(failures);
      setDetailLoading(false);
    }
    return () => {
      active = false;
    };
  }, [pet?.petId]);
  // Ignore old-pet responses when switching companions.
  useEffect(() => {
    let cancelled = false;
    if (!pet?.petId) {
      setDetails({});
      return;
    }
    setDetails({});
    setDetailLoading(true);
    setDetailErrors([]);
    Promise.allSettled([
      api.passport(pet.petId),
      api.timeline(pet.petId),
      api.documents(pet.petId),
      api.vaccines(pet.petId),
      api.encounters(pet.petId),
      api.shares(pet.petId),
    ]).then((results) => {
      if (cancelled) return;
      const value = {},
        failures = [];
      results.forEach((r, i) => {
        const key = [
          "passport",
          "timeline",
          "documents",
          "vaccines",
          "encounters",
          "shares",
        ][i];
        if (r.status === "fulfilled") value[key] = r.value;
        else failures.push(`${key}: ${r.reason.message}`);
      });
      setDetails(value);
      setDetailErrors(failures);
      setDetailLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [pet?.petId]);
  function open(type, value) {
    setFormError("");
    setModal(type);
    if (type === "pet") setForm(value ? { ...value } : { ...newPet });
    if (type === "book")
      setForm({
        petId: pet?.petId || "",
        vetUserId: "",
        hospitalId: "",
        date: todayIST(),
        slotId: "",
        reason: "",
      });
    if (type === "cancel") setForm({ appointment: value, reason: "" });
    if (type === "profile") setForm({ ...profile });
    if (type === "passport") setForm({ ...passport });
    if (type === "record") setForm(value);
    if (type === "share")
      setForm({
        targetHospitalId: "",
        scope: "VACCINES_ONLY",
        expiryHours: 24,
      });
  }
  function close() {
    if (!busy) {
      setModal(null);
      setFormError("");
    }
  }
  useEffect(() => {
    if (!["book", "share", "emergency"].includes(modal)) return;
    let active = true;
    setLoadingVets(true);
    api
      .vets()
      .then((value) => {
        if (active) setVets(value);
      })
      .catch((e) => {
        if (active) setFormError(e.message);
      })
      .finally(() => {
        if (active) setLoadingVets(false);
      });
    return () => {
      active = false;
    };
  }, [modal]);
  useEffect(() => {
    if (modal !== "book" || !form.vetUserId || !form.date) {
      setSlots([]);
      return;
    }
    let active = true;
    setLoadingSlots(true);
    setSlots([]);
    api
      .slots(form.vetUserId, form.date)
      .then((value) => {
        if (active) setSlots(availableFutureSlots(value, form.hospitalId));
      })
      .catch((e) => {
        if (active) setFormError(e.message);
      })
      .finally(() => {
        if (active) setLoadingSlots(false);
      });
    return () => {
      active = false;
    };
  }, [modal, form.vetUserId, form.hospitalId, form.date]);
  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setFormError("");
    setNotice("");
    try {
      if (modal === "pet") {
        const payload = petPayload(form);
        if (payload.photoUrl && !safeExternalUrl(payload.photoUrl))
          throw new Error("Photo URL must be a valid HTTPS address.");
        await api.savePet(payload, form.petId);
        await load();
        setNotice(
          form.petId ? "Pet profile updated." : "Your pet has been added.",
        );
      }
      if (modal === "book") {
        if (!form.petId || !form.slotId)
          throw new Error("Choose your pet and an available appointment slot.");
        if (form.reason.trim().length < 3)
          throw new Error(
            "Describe the reason for your visit (at least 3 characters).",
          );
        await api.book({
          petId: form.petId,
          slotId: form.slotId,
          reason: form.reason.trim(),
        });
        await load();
        setNotice("Your appointment has been booked.");
      }
      if (modal === "cancel") {
        await api.cancel(form.appointment.appointmentId, form.reason);
        await load();
        setNotice("Appointment cancelled.");
      }
      if (modal === "profile") {
        await api.saveProfile(profilePayload(form));
        await load();
        await refreshUserProfile();
        setNotice("Your profile has been updated.");
      }
      if (modal === "passport") {
        const weight =
          form.weightKg === "" || form.weightKg == null
            ? null
            : Number(form.weightKg);
        if (weight !== null && (!Number.isFinite(weight) || weight <= 0))
          throw new Error("Weight must be a positive number.");
        await api.savePassport(
          pet.petId,
          Object.fromEntries(
            [
              "bloodGroup",
              "allergiesSummary",
              "chronicConditionsSummary",
              "dietaryNotes",
              "emergencyContactPhone",
              "color",
              "distinctiveMarks",
              "microchipNumber",
            ]
              .map((k) => [k, form[k] || null])
              .concat([["weightKg", weight]]),
          ),
        );
        await loadDetails();
        setNotice(
          "Passport details updated. Clinical records remain unchanged.",
        );
      }
      if (modal === "share") {
        if (!form.targetHospitalId)
          throw new Error("Choose the hospital that will receive access.");
        const result = await api.share(pet.petId, {
          targetHospitalId: form.targetHospitalId,
          scope: form.scope,
          expiryHours: Number(form.expiryHours),
        });
        await loadDetails();
        setNotice(
          `Hospital sharing status: ${result.status}. Review or revoke access in your passport.`,
        );
      }
      setModal(null);
    } catch (e) {
      setFormError(
        e.message || "We couldn’t save this change. Please try again.",
      );
    } finally {
      setBusy(false);
    }
  }
  async function shareAction(item, action) {
    setBusy(true);
    setNotice("");
    try {
      const value = await api[action](item.requestId);
      await loadDetails();
      setNotice(`Sharing status: ${value.status}.`);
    } catch (e) {
      setErrors([e.message]);
    } finally {
      setBusy(false);
    }
  }
  const visibleAppointments = appointments.filter(
    (a) =>
      (!query ||
        `${a.pet?.name} ${a.hospital?.name} ${a.vet?.firstName}`
          .toLowerCase()
          .includes(query.toLowerCase())) &&
      (filter === "all" ||
        (filter === "upcoming"
          ? upcoming.some((u) => u.appointmentId === a.appointmentId)
          : !upcoming.some((u) => u.appointmentId === a.appointmentId))),
  );
  const visibleTimeline = timeline.filter(
    (t) =>
      (filter === "all" || t.eventType === filter) &&
      `${t.title} ${t.description || ""}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  function Appointment({ item, compact = false }) {
    return (
      <div className="owner-visit">
        <div className="owner-date-tile">
          <CalendarDays size={20} />
          <strong>
            {new Intl.DateTimeFormat("en-IN", {
              day: "numeric",
              timeZone: "Asia/Kolkata",
            }).format(new Date(item.appointmentAt))}
          </strong>
        </div>
        <div className="owner-visit-info">
          <h3>{item.hospital?.name || "Your veterinary appointment"}</h3>
          <p>
            {item.pet?.name || "Pet"} · {dateLabel(item.appointmentAt)} ·{" "}
            {timeLabel(item.appointmentAt)}
          </p>
          <span>
            {item.vet
              ? `Dr. ${item.vet.firstName || ""} ${item.vet.lastName || ""}`
              : "Assigned veterinarian"}
          </span>
        </div>
        <div className="owner-visit-actions">
          <Badge tone={item.status === "CANCELLED" ? "gray" : "teal"}>
            {appointmentLabel(item.status)}
          </Badge>
          {!compact && ["BOOKED", "CONFIRMED"].includes(item.status) && (
            <button
              className="owner-text-button"
              onClick={() => open("cancel", item)}
            >
              Cancel
            </button>
          )}
        </div>
      </div>
    );
  }
  function PetPicker() {
    return (
      <div className="owner-pet-picker">
        <Avatar pet={pet} />
        <label>
          <span>YOUR COMPANION</span>
          <select
            aria-label="Selected pet"
            value={pet?.petId || ""}
            onChange={(e) => setPetId(e.target.value)}
          >
            {!pets.length && <option value="">Add your first pet</option>}
            {pets.map((p) => (
              <option value={p.petId} key={p.petId}>
                {p.name}
              </option>
            ))}
          </select>
        </label>
        <ChevronDown size={15} />
        <button aria-label="Add a pet" onClick={() => open("pet")}>
          <Plus size={17} />
        </button>
      </div>
    );
  }
  return (
    <div className="owner-app">
      <aside
        id="owner-navigation"
        className={`owner-sidebar ${menu ? "open" : ""}`}
      >
        <a
          className="owner-brand"
          href="#owner/home"
          onClick={() => navigate("home")}
        >
          <img src="/vetalya-logo.png" alt="" />
          <div>
            <strong>
              Vetalya<span>®</span>
            </strong>
            <small>EVERY PET. HEALTH. ALWAYS.</small>
          </div>
        </a>
        <div className="owner-portal-label">YOUR PET’S CARE SPACE</div>
        <nav aria-label="Pet owner navigation">
          {navigation.map(([key, label, Icon]) => (
            <a
              href={`#owner/${key}`}
              key={key}
              aria-current={page === key ? "page" : undefined}
              onClick={() => navigate(key)}
              className={page === key ? "active" : ""}
            >
              <Icon size={19} />
              <span>{label}</span>
              {page === key && <i />}
            </a>
          ))}
        </nav>
        <div className="owner-sidebar-bottom">
          <div className="owner-help">
            <span className="owner-help-icon">
              <Heart size={19} />
            </span>
            <h3>A little help, when you need it.</h3>
            <p>Find a clinic and get in touch with your pet’s care team.</p>
            <button onClick={() => open("emergency")}>
              Find veterinary care <ArrowUpRight size={15} />
            </button>
          </div>
          <button className="owner-signout" onClick={() => logout()}>
            <LogOut size={17} /> Sign out
          </button>
        </div>
      </aside>
      {menu && (
        <button
          className="owner-menu-scrim"
          aria-label="Close navigation"
          onClick={() => setMenu(false)}
        />
      )}
      <div className="owner-workspace">
        <header className="owner-topbar">
          <div className="owner-breadcrumb">
            <button
              className="owner-mobile-menu owner-icon-btn"
              aria-label="Open navigation"
              aria-expanded={menu}
              aria-controls="owner-navigation"
              onClick={() => setMenu(!menu)}
            >
              <Menu size={20} />
            </button>
            <span>Pet parent portal</span>
            <ChevronRight size={14} />
            <strong>
              {page === "passport"
                ? "Health passport"
                : navigation.find((n) => n[0] === page)?.[1] || "Home"}
            </strong>
          </div>
          <div className="owner-topbar-right">
            <span className="owner-today">{dateLabel(new Date())}</span>
            <button
              aria-label="View care reminders"
              className="owner-icon-btn"
              onClick={() => navigate("care")}
            >
              <Bell size={19} />
              {due.length > 0 && <i />}
            </button>
            <button className="owner-user" onClick={() => navigate("profile")}>
              <span>{(currentUser?.name || "P").slice(0, 1)}</span>
              <div>
                <strong>{currentUser?.name || "Pet parent"}</strong>
                <small>Pet parent</small>
              </div>
              <ChevronDown size={14} />
            </button>
          </div>
        </header>
        <main className="owner-main" id="owner-main">
          <div className="owner-page-heading">
            <div>
              <p className="owner-eyebrow">
                {page === "home"
                  ? "A HEALTHIER, HAPPIER LIFE TOGETHER"
                  : "EVERY PET’S HEALTH. ALWAYS."}
              </p>
              <h1>
                {
                  {
                    home: `Welcome back, ${currentUser?.name?.split(" ")[0] || "pet parent"}.`,
                    pets: "Your little family.",
                    appointments: "Make time for their care.",
                    records: "Every chapter of their health.",
                    care: "Good care, right on time.",
                    profile: "A little about you.",
                    services: "Care beyond the clinic.",
                    passport: `${pet?.name || "Your pet"}’s health passport.`,
                  }[page]
                }
              </h1>
              <p>
                {
                  {
                    home: "Everything your companion needs, thoughtfully in one place.",
                    pets: "Their details, their stories, and the care that connects them.",
                    appointments:
                      "Find your veterinarian and plan the next visit.",
                    records: "One place for your companion’s medical journey.",
                    care: "Vaccinations and preventive care from your veterinary records.",
                    profile:
                      "Keep your contact and caregiver details up to date.",
                    passport:
                      "The important details that travel with your companion.",
                  }[page]
                }
              </p>
            </div>
            {!["profile"].includes(page) && (
              <button
                className="owner-button"
                onClick={() => open(page === "pets" ? "pet" : "book")}
              >
                <Plus size={17} />
                {page === "pets" ? "Add a pet" : "Book appointment"}
              </button>
            )}
          </div>
          {errors.length > 0 && (
            <div role="alert" className="owner-alert">
              <AlertCircle size={18} />
              <div>
                {errors.map((e) => (
                  <p key={e}>{e}</p>
                ))}
              </div>
              <button onClick={load}>Retry</button>
            </div>
          )}
          {notice && (
            <div role="status" className="owner-notice">
              <Check size={17} />
              {notice}
              <button aria-label="Dismiss notice" onClick={() => setNotice("")}>
                <X size={15} />
              </button>
            </div>
          )}
          {loading ? (
            <div role="status" className="owner-skeleton">
              <div />
              <div />
              <div />
              <span>Loading your care space…</span>
            </div>
          ) : (
            <>
              {page === "home" && (
                <>
                  <div className="owner-home-grid">
                    <section className="owner-hero">
                      <span className="owner-hero-kicker">
                        <span />
                        THEIR HEALTH. YOUR PEACE OF MIND.
                      </span>
                      <h2>
                        A little care.
                        <br />A lifetime <em>together.</em>
                      </h2>
                      <p>
                        From everyday checkups to life’s little surprises,
                        <br className="owner-desktop" /> we’re here for every
                        chapter.
                      </p>
                      <button onClick={() => open("book")}>
                        Plan their next visit <ArrowUpRight size={17} />
                      </button>
                      <div className="owner-hero-decoration" aria-hidden="true">
                        <div className="owner-hero-ring" />
                        <div className="owner-heart-orbit">
                          <Heart size={42} />
                        </div>
                        <PawPrint className="owner-hero-paw" size={85} />
                        <span className="owner-hero-plus">+</span>
                      </div>
                    </section>
                    <section className="owner-featured-pet">
                      <div className="owner-card-heading">
                        <h2>My companion</h2>
                        <button
                          className="owner-text-button"
                          onClick={() => navigate("pets")}
                        >
                          View all <ArrowUpRight size={14} />
                        </button>
                      </div>
                      {pet ? (
                        <>
                          <PetPicker />
                          <div className="owner-pet-summary">
                            <div>
                              <small>SPECIES</small>
                              <strong>
                                {SPECIES[pet.species] || pet.species}
                              </strong>
                            </div>
                            <div>
                              <small>AGE</small>
                              <strong>{pet.age || "Not recorded"}</strong>
                            </div>
                            <div>
                              <small>BREED</small>
                              <strong>{pet.breed || "Not recorded"}</strong>
                            </div>
                          </div>
                          <button
                            className="owner-passport-link"
                            onClick={() => navigate("passport")}
                          >
                            <ShieldCheck size={18} />
                            <span>Open health passport</span>
                            <ArrowRight size={16} />
                          </button>
                        </>
                      ) : (
                        <Empty
                          title="Meet your care companion"
                          action={
                            <button
                              className="owner-button"
                              onClick={() => open("pet")}
                            >
                              Add your first pet
                            </button>
                          }
                        >
                          Add your pet to start their health journey.
                        </Empty>
                      )}
                    </section>
                  </div>
                  <div className="owner-stat-grid">
                    {[
                      [
                        PawPrint,
                        "My pets",
                        pets.length,
                        "Companions in your care",
                      ],
                      [
                        CalendarCheck2,
                        "Upcoming visits",
                        upcoming.length,
                        "Appointments scheduled",
                      ],
                      [
                        HeartPulse,
                        "Care reminders",
                        due.length,
                        "Vaccinations due within 60 days",
                      ],
                    ].map(([Icon, label, value, caption]) => (
                      <button
                        className="owner-stat"
                        key={label}
                        onClick={() =>
                          navigate(
                            label === "My pets"
                              ? "pets"
                              : label === "Upcoming visits"
                                ? "appointments"
                                : "care",
                          )
                        }
                      >
                        <span>
                          <Icon size={22} />
                        </span>
                        <div>
                          <small>{label}</small>
                          <strong>{value}</strong>
                          <p>{caption}</p>
                        </div>
                        <ArrowUpRight size={17} />
                      </button>
                    ))}
                  </div>
                  <div className="owner-content-grid">
                    <section className="owner-card">
                      <div className="owner-card-heading">
                        <div>
                          <span className="owner-eyebrow">
                            A DATE FOR THEIR WELLBEING
                          </span>
                          <h2>Your next appointment</h2>
                        </div>
                        <button
                          className="owner-text-button"
                          onClick={() => navigate("appointments")}
                        >
                          View all <ArrowRight size={14} />
                        </button>
                      </div>
                      {next ? (
                        <Appointment item={next} compact />
                      ) : (
                        <Empty
                          icon={CalendarDays}
                          title="A little space in the calendar"
                          action={
                            <button
                              className="owner-button secondary"
                              onClick={() => open("book")}
                            >
                              Find an appointment <ArrowRight size={15} />
                            </button>
                          }
                        >
                          Book a visit when your companion needs one. Confirmed
                          details will appear here.
                        </Empty>
                      )}
                      <div className="owner-soft-note">
                        <ShieldCheck size={17} />
                        <span>
                          Bookings and medical records come directly from your
                          care providers.
                        </span>
                      </div>
                    </section>
                    <section className="owner-card">
                      <div className="owner-card-heading">
                        <h2>Care on your radar</h2>
                        <span className="owner-dot-label">NEXT 60 DAYS</span>
                      </div>
                      {due.slice(0, 3).map((v, i) => (
                        <div
                          className="owner-reminder"
                          key={v.vaccinationId || i}
                        >
                          <span>
                            <Syringe size={18} />
                          </span>
                          <div>
                            <h3>{v.vaccineName}</h3>
                            <p>
                              {v.petName} ·{" "}
                              {dateLabel(v.nextVaccinationDate || v.dueDate)}
                            </p>
                          </div>
                          <ArrowUpRight size={16} />
                        </div>
                      ))}
                      {!due.length && (
                        <Empty
                          icon={HeartPulse}
                          title="No due vaccinations returned"
                        >
                          Your veterinarian’s recorded vaccination due dates
                          will appear here.
                        </Empty>
                      )}
                      <button
                        className="owner-wide-link"
                        onClick={() => navigate("care")}
                      >
                        View care records <ArrowRight size={15} />
                      </button>
                    </section>
                  </div>
                  <section className="owner-record-strip">
                    <div className="owner-record-symbol">
                      <FolderHeart size={29} />
                    </div>
                    <div>
                      <span className="owner-eyebrow">
                        ONE PET. ONE HEALTH STORY.
                      </span>
                      <h2>Their whole journey, beautifully together.</h2>
                      <p>
                        Consultations, vaccinations and documents. Organized
                        around the companion you love.
                      </p>
                    </div>
                    <button
                      className="owner-button secondary"
                      onClick={() => navigate("records")}
                    >
                      Explore health records <ArrowUpRight size={16} />
                    </button>
                  </section>
                </>
              )}
              {page === "pets" && (
                <div className="owner-pets-grid">
                  {pets.map((p) => (
                    <article
                      className="owner-card owner-pet-card"
                      key={p.petId}
                    >
                      <div className="owner-card-heading">
                        <Badge>{SPECIES[p.species] || p.species}</Badge>
                        <button
                          className="owner-text-button"
                          onClick={() => open("pet", p)}
                        >
                          Edit profile
                        </button>
                      </div>
                      <Avatar pet={p} large />
                      <h2>{p.name}</h2>
                      <p>
                        {p.breed || "Breed not recorded"} ·{" "}
                        {p.age || "Age not recorded"}
                      </p>
                      <div className="owner-pet-facts">
                        <span>
                          Sex
                          <strong>
                            {p.gender === "UNKNOWN"
                              ? "Not recorded"
                              : p.gender?.toLowerCase()}
                          </strong>
                        </span>
                        <span>
                          Birthday<strong>{dateLabel(p.dob)}</strong>
                        </span>
                      </div>
                      <button
                        className="owner-passport-link"
                        onClick={() => {
                          setPetId(p.petId);
                          navigate("passport");
                        }}
                      >
                        <ShieldCheck size={17} /> Health passport{" "}
                        <ArrowRight size={16} />
                      </button>
                    </article>
                  ))}
                  <button
                    className="owner-add-pet-card"
                    onClick={() => open("pet")}
                  >
                    <span>
                      <Plus size={24} />
                    </span>
                    <h2>A new member of the family?</h2>
                    <p>Add their details and start their care story.</p>
                    <strong>
                      Add a pet <ArrowRight size={15} />
                    </strong>
                  </button>
                </div>
              )}
              {page === "appointments" && (
                <section className="owner-card">
                  <div className="owner-list-toolbar">
                    <div className="owner-segmented">
                      {["all", "upcoming", "history"].map((f) => (
                        <button
                          className={filter === f ? "active" : ""}
                          onClick={() => setFilter(f)}
                          key={f}
                        >
                          {f === "all"
                            ? "All visits"
                            : f === "upcoming"
                              ? "Upcoming"
                              : "Past & cancelled"}
                        </button>
                      ))}
                    </div>
                    <label className="owner-search">
                      <Search size={17} />
                      <input
                        aria-label="Search appointments"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                        placeholder="Search pet or clinic"
                      />
                    </label>
                  </div>
                  {visibleAppointments.map((a) => (
                    <Appointment key={a.appointmentId} item={a} />
                  ))}
                  {!visibleAppointments.length && (
                    <Empty icon={CalendarDays} title="No appointments to show">
                      Try another filter or book a visit with your veterinarian.
                    </Empty>
                  )}
                </section>
              )}
              {["passport", "records", "care"].includes(page) && (
                <>
                  <PetPicker />
                  {detailErrors.length > 0 && (
                    <div role="alert" className="owner-alert">
                      <AlertCircle size={18} />
                      <div>
                        {detailErrors.map((e) => (
                          <p key={e}>{e}</p>
                        ))}
                      </div>
                      <button onClick={loadDetails}>Retry</button>
                    </div>
                  )}
                  {!pet ? (
                    <Empty
                      title="Every health story starts with a pet"
                      action={
                        <button
                          className="owner-button"
                          onClick={() => open("pet")}
                        >
                          Add a pet
                        </button>
                      }
                    >
                      Register your companion to view their records.
                    </Empty>
                  ) : detailLoading ? (
                    <div role="status" className="owner-card owner-loading">
                      Loading {pet.name}’s health records…
                    </div>
                  ) : (
                    <>
                      {page === "passport" && (
                        <>
                          <section className="owner-passport">
                            <div className="owner-passport-header">
                              <div>
                                <span className="owner-eyebrow">
                                  DIGITAL HEALTH PASSPORT
                                </span>
                                <h2>{pet.name}</h2>
                                <p>
                                  {SPECIES[pet.species]} ·{" "}
                                  {pet.breed || "Breed not recorded"}
                                </p>
                                <code>
                                  {passport?.passportNumber ||
                                    "Passport number not available"}
                                </code>
                              </div>
                              <Avatar pet={pet} large />
                            </div>
                            <div className="owner-passport-facts">
                              {[
                                ["Date of birth", dateLabel(pet.dob)],
                                [
                                  "Weight",
                                  passport?.weightKg
                                    ? `${passport.weightKg} kg`
                                    : "Not recorded",
                                ],
                                [
                                  "Microchip",
                                  passport?.microchipNumber || "Not recorded",
                                ],
                                [
                                  "Emergency contact",
                                  passport?.emergencyContactPhone ||
                                    "Not recorded",
                                ],
                              ].map(([k, v]) => (
                                <div key={k}>
                                  <small>{k}</small>
                                  <strong>{v}</strong>
                                </div>
                              ))}
                            </div>
                            <div className="owner-passport-actions">
                              <button
                                className="owner-button secondary"
                                disabled={!passport}
                                onClick={() => open("passport")}
                              >
                                Edit caregiver details
                              </button>
                              <button
                                className="owner-button"
                                onClick={() => open("share")}
                              >
                                <Share2 size={16} />
                                Manage sharing
                              </button>
                            </div>
                          </section>
                          <div className="owner-content-grid">
                            <section className="owner-card">
                              <h2>Important health details</h2>
                              {[
                                ["Allergies", passport?.allergiesSummary],
                                [
                                  "Ongoing conditions",
                                  passport?.chronicConditionsSummary,
                                ],
                                ["Dietary notes", passport?.dietaryNotes],
                              ].map(([k, v]) => (
                                <div className="owner-health-detail" key={k}>
                                  <span>{k}</span>
                                  <p>{v || "Not recorded"}</p>
                                </div>
                              ))}
                              <div className="owner-soft-note">
                                <ShieldCheck size={17} />
                                Caregiver details are distinct from signed
                                veterinary records.
                              </div>
                            </section>
                            <section className="owner-card">
                              <h2>Hospital access history</h2>
                              {shares
                                .filter((s) => s.petId === pet.petId)
                                .map((s) => (
                                  <div
                                    className="owner-share-item"
                                    key={s.requestId}
                                  >
                                    <p>
                                      Hospital:{" "}
                                      {s.targetHospitalId || "Not specified"}
                                    </p>
                                    <Badge>{s.status}</Badge>
                                    <p>
                                      {s.scope
                                        .replaceAll("_", " ")
                                        .toLowerCase()}{" "}
                                      · expires {dateLabel(s.expiresAt)}
                                    </p>
                                    {s.targetHospitalId &&
                                      ["PENDING", "REQUESTED"].includes(
                                        s.status,
                                      ) && (
                                        <button
                                          disabled={busy}
                                          className="owner-text-button"
                                          onClick={() =>
                                            shareAction(s, "approve")
                                          }
                                        >
                                          Approve hospital access
                                        </button>
                                      )}
                                    {s.status !== "REVOKED" && (
                                      <button
                                        disabled={busy}
                                        className="owner-text-button"
                                        onClick={() => shareAction(s, "revoke")}
                                      >
                                        Revoke request
                                      </button>
                                    )}
                                  </div>
                                ))}
                              {!shares.some((s) => s.petId === pet.petId) && (
                                <p className="owner-muted">
                                  Grant scoped access to a specific hospital.
                                  Approval and revocation are controlled by the
                                  backend.
                                </p>
                              )}
                            </section>
                          </div>
                        </>
                      )}
                      {page === "records" && (
                        <section className="owner-card">
                          <div
                            className="owner-document-list"
                            style={{ marginTop: 0, marginBottom: 25 }}
                          >
                            <h2>Signed consultations & prescriptions</h2>
                            {(details.encounters || [])
                              .filter((r) => r.isSigned)
                              .map((r) => (
                                <button
                                  key={r.encounterId}
                                  className="owner-signed-record"
                                  onClick={() => open("record", r)}
                                >
                                  <span className="owner-timeline-icon">
                                    <ShieldCheck size={18} />
                                  </span>
                                  <div>
                                    <h3>
                                      {r.diagnosisPrimary ||
                                        "Clinical consultation"}
                                    </h3>
                                    <p>
                                      {dateLabel(r.signedAt)} ·{" "}
                                      {r.vetName || "Authoring veterinarian"}
                                      {r.prescription
                                        ? " · Prescription attached"
                                        : ""}
                                    </p>
                                  </div>
                                  <ArrowUpRight size={17} />
                                </button>
                              ))}
                            {!(details.encounters || []).some(
                              (r) => r.isSigned,
                            ) && (
                              <p className="owner-muted">
                                No signed clinical records returned for this
                                companion.
                              </p>
                            )}
                          </div>
                          <div className="owner-list-toolbar">
                            <div className="owner-segmented">
                              {[
                                ["all", "Timeline"],
                                ["CONSULTATION", "Consultations"],
                                ["DOCUMENT_UPLOAD", "Documents"],
                              ].map(([k, v]) => (
                                <button
                                  key={k}
                                  className={filter === k ? "active" : ""}
                                  onClick={() => setFilter(k)}
                                >
                                  {v}
                                </button>
                              ))}
                            </div>
                            <label className="owner-search">
                              <Search size={17} />
                              <input
                                aria-label="Search health records"
                                placeholder="Search records"
                                value={query}
                                onChange={(e) => setQuery(e.target.value)}
                              />
                            </label>
                          </div>
                          {visibleTimeline.map((t) => (
                            <article
                              className="owner-timeline-item"
                              key={`${t.eventType}-${t.id}`}
                            >
                              <span className="owner-timeline-icon">
                                <FileText size={18} />
                              </span>
                              <div>
                                <div className="owner-record-title">
                                  <h3>{t.title}</h3>
                                  <Badge
                                    tone={
                                      t.verificationStatus === "VERIFIED_BY_VET"
                                        ? "teal"
                                        : "gray"
                                    }
                                  >
                                    {t.verificationStatus
                                      ?.replaceAll("_", " ")
                                      .toLowerCase() || "Status not supplied"}
                                  </Badge>
                                </div>
                                <p>{t.subtitle || t.description}</p>
                                <small>
                                  {dateLabel(t.eventTimestamp)}
                                  {t.clinicOrDoctor
                                    ? ` · ${t.clinicOrDoctor}`
                                    : ""}
                                </small>
                                {safeExternalUrl(t.downloadUrl) && (
                                  <a
                                    className="owner-text-button"
                                    href={t.downloadUrl}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                  >
                                    Open record <ExternalLink size={14} />
                                  </a>
                                )}
                              </div>
                            </article>
                          ))}
                          {!visibleTimeline.length && (
                            <Empty
                              icon={FolderHeart}
                              title="Their story is just beginning"
                            >
                              Medical events from connected records will appear
                              here.
                            </Empty>
                          )}
                          <div className="owner-document-list">
                            <h2>
                              Document library <span>{documents.length}</span>
                            </h2>
                            {documents.map((d) => (
                              <div
                                className="owner-document"
                                key={d.documentId}
                              >
                                <FileText size={19} />
                                <div>
                                  <h3>{d.fileName}</h3>
                                  <p>
                                    {d.category
                                      ?.replaceAll("_", " ")
                                      .toLowerCase()}{" "}
                                    ·{" "}
                                    {d.verificationStatus
                                      ?.replaceAll("_", " ")
                                      .toLowerCase()}
                                  </p>
                                </div>
                                {safeExternalUrl(d.storagePath) && (
                                  <a
                                    href={d.storagePath}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    aria-label={`Open ${d.fileName}`}
                                  >
                                    <ExternalLink size={17} />
                                  </a>
                                )}
                              </div>
                            ))}
                            <div className="owner-soft-note">
                              <AlertCircle size={17} />
                              <span>
                                New file uploads await private document-storage
                                integration. Existing document records remain
                                available.
                              </span>
                            </div>
                          </div>
                        </section>
                      )}
                      {page === "care" && (
                        <div className="owner-content-grid">
                          <section className="owner-card">
                            <h2>Vaccinations due in the next 60 days</h2>
                            {due
                              .filter(
                                (v) =>
                                  v.petId === pet.petId ||
                                  v.petName === pet.name,
                              )
                              .map((v, i) => (
                                <div
                                  className="owner-reminder"
                                  key={v.vaccinationId || i}
                                >
                                  <span>
                                    <Syringe size={20} />
                                  </span>
                                  <div>
                                    <h3>{v.vaccineName}</h3>
                                    <p>
                                      {dateLabel(
                                        v.nextVaccinationDate || v.dueDate,
                                      )}
                                    </p>
                                  </div>
                                  <button
                                    className="owner-text-button"
                                    onClick={() => open("book")}
                                  >
                                    Book visit
                                  </button>
                                </div>
                              ))}
                            {!due.some(
                              (v) =>
                                v.petId === pet.petId || v.petName === pet.name,
                            ) && (
                              <Empty
                                icon={HeartPulse}
                                title="No due vaccinations returned"
                              >
                                Keep vaccination dates up to date with your
                                veterinarian.
                              </Empty>
                            )}
                          </section>
                          <section className="owner-card">
                            <h2>Vaccination history</h2>
                            {vaccines.map((v, i) => (
                              <div
                                className="owner-reminder"
                                key={v.vaccinationId || i}
                              >
                                <span>
                                  <ShieldCheck size={19} />
                                </span>
                                <div>
                                  <h3>{v.vaccineName}</h3>
                                  <p>
                                    Given {dateLabel(v.lastVaccinationDate)}
                                    <br />
                                    Next due {dateLabel(v.nextVaccinationDate)}
                                  </p>
                                </div>
                              </div>
                            ))}
                            {!vaccines.length && (
                              <Empty
                                icon={Syringe}
                                title="No vaccination records yet"
                              >
                                Vaccinations recorded by your veterinary team
                                will appear here.
                              </Empty>
                            )}
                            <div className="owner-soft-note">
                              <Clock3 size={16} />
                              Medication notifications await clinician-approved
                              schedules and notification integration.
                            </div>
                          </section>
                        </div>
                      )}
                    </>
                  )}
                </>
              )}
              {page === "services" && (
                <>
                  <div className="owner-soft-note">
                    <AlertCircle size={17} />
                    <span>
                      Service-provider booking and insurance integrations are
                      not connected yet. No orders, coverage or payments are
                      created here.
                    </span>
                  </div>
                  <div className="owner-pets-grid">
                    {PLANNED_SERVICES.map((service) => (
                      <section className="owner-card" key={service.name}>
                        <span className="owner-feature-icon">
                          <Heart size={23} />
                        </span>
                        <h2>{service.name}</h2>
                        <p className="owner-muted">{service.description}</p>
                        <div style={{ marginTop: 18 }}>
                          <Badge tone="gray">Integration pending</Badge>
                        </div>
                      </section>
                    ))}
                  </div>
                </>
              )}
              {page === "profile" && (
                <div className="owner-content-grid">
                  <section className="owner-card owner-profile-card">
                    <div className="owner-profile-avatar">
                      {currentUser?.name?.slice(0, 1) || "P"}
                    </div>
                    <h2>
                      {profile?.firstName
                        ? `${profile.firstName} ${profile.lastName}`
                        : currentUser?.name}
                    </h2>
                    <p>{profile?.email || currentUser?.email}</p>
                    <Badge>Pet parent</Badge>
                    <div className="owner-profile-details">
                      {[
                        ["Phone", profile?.phone],
                        ["Address", profile?.address1],
                        ["City", profile?.city],
                        ["Pincode", profile?.pincode],
                      ].map(([k, v]) => (
                        <div key={k}>
                          <span>{k}</span>
                          <strong>{v || "Not provided"}</strong>
                        </div>
                      ))}
                    </div>
                    <button
                      className="owner-button"
                      disabled={!profile}
                      onClick={() => open("profile")}
                    >
                      Edit profile
                    </button>
                  </section>
                  <section className="owner-card">
                    <span className="owner-feature-icon">
                      <ShieldCheck size={26} />
                    </span>
                    <h2>Your care space is personal.</h2>
                    <p className="owner-muted">
                      Your account gives you access to your companions’ records.
                      Signed medical notes stay with their authoring veterinary
                      team.
                    </p>
                    <div className="owner-health-detail">
                      <span>Email verification</span>
                      <p>
                        {profile?.emailVerified
                          ? "Verified by the account service"
                          : "Not verified"}
                      </p>
                    </div>
                    <div className="owner-health-detail">
                      <span>Notification delivery</span>
                      <p>
                        Delivery preferences and push notifications are awaiting
                        backend integration.
                      </p>
                    </div>
                    <button
                      className="owner-button secondary"
                      onClick={() => logout()}
                    >
                      <LogOut size={16} />
                      Sign out of this session
                    </button>
                  </section>
                </div>
              )}
            </>
          )}
          <footer className="owner-footer">
            <span>Vetalya · Every Pet. Health. Always.</span>
            <span>
              Made for the ones who make your life whole. <Heart size={12} />
            </span>
          </footer>
        </main>
      </div>
      <nav
        className="owner-bottom-nav"
        aria-label="Mobile pet owner navigation"
      >
        {navigation.slice(0, 5).map(([key, label, Icon]) => (
          <a
            href={`#owner/${key}`}
            key={key}
            className={page === key ? "active" : ""}
            onClick={() => navigate(key)}
            aria-current={page === key ? "page" : undefined}
          >
            <Icon size={20} />
            <span>
              {
                {
                  home: "Home",
                  pets: "Pets",
                  appointments: "Visits",
                  records: "Records",
                  care: "Care",
                }[key]
              }
            </span>
          </a>
        ))}
      </nav>
      {modal && (
        <Modal
          title={
            {
              pet: form.petId ? "Edit pet profile" : "Meet your new companion",
              book: "Book a veterinary visit",
              cancel: "Cancel this appointment?",
              profile: "Your caregiver profile",
              passport: "Caregiver health details",
              share: "Share with a hospital",
              emergency: "Get in touch with a clinic",
              record: "Signed veterinary record",
            }[modal]
          }
          subtitle={
            modal === "book"
              ? "Choose a real, available slot from your veterinarian’s schedule."
              : undefined
          }
          onClose={close}
          busy={busy}
        >
          {formError && (
            <div role="alert" className="owner-alert">
              {formError}
            </div>
          )}
          {modal === "record" ? (
            <div className="owner-signed-detail">
              <Badge>Signed veterinary record · read-only</Badge>
              <h2>{form.diagnosisPrimary}</h2>
              <p className="owner-muted">
                {form.vetName} · {form.hospitalName} ·{" "}
                {dateLabel(form.signedAt)}
              </p>
              {[
                ["Chief complaint", form.chiefComplaint],
                ["Assessment", form.assessmentNotes],
                ["Treatment plan", form.treatmentPlan],
                ["Diet instructions", form.dietInstructions],
                [
                  "Follow-up",
                  form.followUpDate ? dateLabel(form.followUpDate) : null,
                ],
              ].map(([k, v]) =>
                v ? (
                  <div key={k} className="owner-health-detail">
                    <span>{k}</span>
                    <p>{v}</p>
                  </div>
                ) : null,
              )}
              <h2 style={{ marginTop: 25 }}>Prescription</h2>
              {form.prescription ? (
                (form.prescription.items || []).map((m, i) => (
                  <div key={m.itemId || i} className="owner-health-detail">
                    <span>{m.medicineName}</span>
                    <p>
                      {m.dosage} · {m.frequency} · {m.duration}
                    </p>
                    {m.instructions && <p>{m.instructions}</p>}
                  </div>
                ))
              ) : (
                <p className="owner-muted">
                  No prescription is attached to this encounter.
                </p>
              )}
              {safeExternalUrl(form.prescription?.pdfStorageUrl) && (
                <a
                  className="owner-button secondary"
                  href={form.prescription.pdfStorageUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Open prescription PDF <ExternalLink size={15} />
                </a>
              )}
            </div>
          ) : modal === "emergency" ? (
            <div>
              <p className="owner-muted">
                Contact a veterinary clinic directly for urgent care.
                Availability and response times are confirmed by the clinic.
              </p>
              {loadingVets ? (
                <p>Finding clinics…</p>
              ) : (
                [
                  ...new Map(
                    vets
                      .flatMap((v) => v.hospitals || [])
                      .map((h) => [h.hospitalId, h]),
                  ).values(),
                ].map((h) => (
                  <div className="owner-clinic" key={h.hospitalId}>
                    <div>
                      <h3>{h.name}</h3>
                      <p>
                        {h.city} ·{" "}
                        {h.is24x7
                          ? "Lists 24-hour service"
                          : "Call for opening hours"}
                      </p>
                    </div>
                    {h.mobile && (
                      <a
                        className="owner-button secondary"
                        href={`tel:${h.mobile.replace(/[^+0-9]/g, "")}`}
                      >
                        <Phone size={15} />
                        Call clinic
                      </a>
                    )}
                  </div>
                ))
              )}
              {!loadingVets && !vets.length && (
                <p>
                  No connected clinics were returned. Contact your regular
                  veterinary clinic.
                </p>
              )}
            </div>
          ) : (
            <form onSubmit={submit}>
              <div className="owner-form-grid">
                {modal === "pet" && (
                  <>
                    <Field label="Pet’s name" wide>
                      <input
                        required
                        maxLength={100}
                        value={form.name}
                        onChange={(e) =>
                          setForm({ ...form, name: e.target.value })
                        }
                        placeholder="What do you call your companion?"
                      />
                    </Field>
                    <Field label="Species">
                      <select
                        value={form.species}
                        onChange={(e) =>
                          setForm({ ...form, species: e.target.value })
                        }
                      >
                        {Object.entries(SPECIES).map(([k, v]) => (
                          <option key={k} value={k}>
                            {v}
                          </option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Sex">
                      <select
                        value={form.gender}
                        onChange={(e) =>
                          setForm({ ...form, gender: e.target.value })
                        }
                      >
                        <option value="UNKNOWN">Not recorded</option>
                        <option value="MALE">Male</option>
                        <option value="FEMALE">Female</option>
                      </select>
                    </Field>
                    <Field label="Breed">
                      <input
                        maxLength={100}
                        value={form.breed || ""}
                        onChange={(e) =>
                          setForm({ ...form, breed: e.target.value })
                        }
                        placeholder="Optional"
                      />
                    </Field>
                    <Field label="Date of birth">
                      <input
                        type="date"
                        max={todayIST()}
                        value={form.dob || ""}
                        onChange={(e) =>
                          setForm({ ...form, dob: e.target.value })
                        }
                      />
                    </Field>
                    <Field label="Photo URL (optional, HTTPS)" wide>
                      <input
                        type="url"
                        maxLength={500}
                        value={form.photoUrl || ""}
                        onChange={(e) =>
                          setForm({ ...form, photoUrl: e.target.value })
                        }
                        placeholder="https://…"
                      />
                    </Field>
                  </>
                )}
                {modal === "book" && (
                  <>
                    <Field label="Who is this visit for?" wide>
                      <select
                        required
                        value={form.petId}
                        onChange={(e) =>
                          setForm({ ...form, petId: e.target.value })
                        }
                      >
                        <option value="">Select a pet</option>
                        {pets.map((p) => (
                          <option key={p.petId} value={p.petId}>
                            {p.name}
                          </option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Veterinarian" wide>
                      <select
                        required
                        value={form.vetUserId}
                        disabled={loadingVets}
                        onChange={(e) => {
                          const v = vets.find(
                            (v) => v.vetUserId === e.target.value,
                          );
                          setForm({
                            ...form,
                            vetUserId: e.target.value,
                            hospitalId: v?.hospitals?.[0]?.hospitalId || "",
                            slotId: "",
                          });
                        }}
                      >
                        <option value="">
                          {loadingVets
                            ? "Loading veterinarians…"
                            : "Select a veterinarian"}
                        </option>
                        {vets.map((v) => (
                          <option key={v.vetUserId} value={v.vetUserId}>
                            Dr. {v.firstName} {v.lastName}
                          </option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Clinic">
                      <select
                        required
                        value={form.hospitalId}
                        onChange={(e) =>
                          setForm({
                            ...form,
                            hospitalId: e.target.value,
                            slotId: "",
                          })
                        }
                      >
                        <option value="">Select a clinic</option>
                        {(
                          vets.find((v) => v.vetUserId === form.vetUserId)
                            ?.hospitals || []
                        ).map((h) => (
                          <option value={h.hospitalId} key={h.hospitalId}>
                            {h.name}
                          </option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Visit date · IST">
                      <input
                        required
                        type="date"
                        min={todayIST()}
                        value={form.date}
                        onChange={(e) =>
                          setForm({ ...form, date: e.target.value, slotId: "" })
                        }
                      />
                    </Field>
                    <Field label="Available slots · IST" wide>
                      <div className="owner-slots">
                        {loadingSlots ? (
                          <p>Checking availability…</p>
                        ) : (
                          slots.map((s) => (
                            <button
                              type="button"
                              key={s.slotId}
                              className={
                                form.slotId === s.slotId ? "selected" : ""
                              }
                              onClick={() =>
                                setForm({ ...form, slotId: s.slotId })
                              }
                            >
                              {s.startTime?.slice(0, 5)}–
                              {s.endTime?.slice(0, 5)}
                            </button>
                          ))
                        )}
                        {!loadingSlots && !slots.length && (
                          <p>
                            {form.vetUserId
                              ? "No available slots returned for this clinic and date. Try another date or contact the clinic."
                              : "Choose a veterinarian to see availability."}
                          </p>
                        )}
                      </div>
                    </Field>
                    <Field label="Reason for the visit" wide>
                      <textarea
                        required
                        minLength={3}
                        maxLength={500}
                        rows={3}
                        value={form.reason}
                        onChange={(e) =>
                          setForm({ ...form, reason: e.target.value })
                        }
                        placeholder="Tell your veterinarian what brings you in."
                      />
                    </Field>
                  </>
                )}
                {modal === "cancel" && (
                  <>
                    <p className="wide owner-muted">
                      {form.appointment.pet?.name} ·{" "}
                      {dateLabel(form.appointment.appointmentAt)}. The clinic’s
                      cancellation rules apply.
                    </p>
                    <Field label="Cancellation reason (optional)" wide>
                      <textarea
                        maxLength={500}
                        value={form.reason}
                        onChange={(e) =>
                          setForm({ ...form, reason: e.target.value })
                        }
                      />
                    </Field>
                  </>
                )}
                {modal === "profile" &&
                  [
                    "firstName",
                    "lastName",
                    "address1",
                    "address2",
                    "city",
                    "state",
                    "pincode",
                    "country",
                  ].map((key) => (
                    <Field
                      key={key}
                      label={
                        {
                          firstName: "First name",
                          lastName: "Last name",
                          address1: "Address line 1",
                          address2: "Address line 2",
                          pincode: "Pincode",
                          city: "City",
                          state: "State",
                          country: "Country",
                        }[key]
                      }
                    >
                      <input
                        required={["firstName", "lastName"].includes(key)}
                        value={form[key] || ""}
                        maxLength={key === "pincode" ? 6 : 100}
                        onChange={(e) =>
                          setForm({ ...form, [key]: e.target.value })
                        }
                      />
                    </Field>
                  ))}
                {modal === "passport" &&
                  [
                    "weightKg",
                    "microchipNumber",
                    "emergencyContactPhone",
                    "bloodGroup",
                    "allergiesSummary",
                    "chronicConditionsSummary",
                    "dietaryNotes",
                  ].map((key) => (
                    <Field
                      wide={key.endsWith("Summary") || key === "dietaryNotes"}
                      key={key}
                      label={
                        {
                          weightKg: "Weight (kg)",
                          microchipNumber: "Microchip number",
                          emergencyContactPhone: "Emergency contact",
                          bloodGroup: "Blood group",
                          allergiesSummary: "Known allergies (caregiver notes)",
                          chronicConditionsSummary:
                            "Ongoing conditions (caregiver notes)",
                          dietaryNotes: "Dietary notes",
                        }[key]
                      }
                    >
                      <input
                        type={key === "weightKg" ? "number" : "text"}
                        step={key === "weightKg" ? "0.01" : undefined}
                        min={key === "weightKg" ? "0.01" : undefined}
                        value={form[key] ?? ""}
                        maxLength={
                          key.endsWith("Summary") || key === "dietaryNotes"
                            ? 1000
                            : key === "emergencyContactPhone" ||
                                key === "bloodGroup"
                              ? 20
                              : 50
                        }
                        onChange={(e) =>
                          setForm({ ...form, [key]: e.target.value })
                        }
                      />
                    </Field>
                  ))}
                {modal === "share" && (
                  <>
                    <p className="wide owner-muted">
                      Choose a hospital and limit what you share. Submitting
                      grants that hospital access immediately for the selected
                      duration. You can revoke access from your passport during
                      this session.
                    </p>
                    <Field label="Receiving hospital" wide>
                      <select
                        required
                        value={form.targetHospitalId}
                        onChange={(e) =>
                          setForm({ ...form, targetHospitalId: e.target.value })
                        }
                      >
                        <option value="">Select a hospital</option>
                        {[
                          ...new Map(
                            vets
                              .flatMap((v) => v.hospitals || [])
                              .map((h) => [h.hospitalId, h]),
                          ).values(),
                        ].map((h) => (
                          <option key={h.hospitalId} value={h.hospitalId}>
                            {h.name}
                          </option>
                        ))}
                      </select>
                    </Field>
                    <Field label="Records to share">
                      <select
                        value={form.scope}
                        onChange={(e) =>
                          setForm({ ...form, scope: e.target.value })
                        }
                      >
                        <option value="VACCINES_ONLY">Vaccinations only</option>
                        <option value="EMERGENCY_SUMMARY">
                          Emergency summary
                        </option>
                        <option value="FULL_TIMELINE">Full timeline</option>
                      </select>
                    </Field>
                    <Field label="Access duration">
                      <select
                        value={form.expiryHours}
                        onChange={(e) =>
                          setForm({ ...form, expiryHours: e.target.value })
                        }
                      >
                        <option value={24}>24 hours</option>
                        <option value={72}>3 days</option>
                        <option value={168}>7 days</option>
                      </select>
                    </Field>
                  </>
                )}
              </div>
              <div className="owner-form-actions">
                <button
                  type="button"
                  disabled={busy}
                  className="owner-button secondary"
                  onClick={close}
                >
                  Go back
                </button>
                <button
                  disabled={
                    busy || (modal === "book" && (!form.slotId || !pets.length))
                  }
                  className="owner-button"
                >
                  {busy
                    ? "Saving…"
                    : {
                        pet: form.petId ? "Save pet profile" : "Add my pet",
                        book: "Confirm appointment",
                        cancel: "Cancel appointment",
                        profile: "Save profile",
                        passport: "Save caregiver details",
                        share: "Grant hospital access",
                      }[modal]}
                  {!busy && <ArrowRight size={16} />}
                </button>
              </div>
            </form>
          )}
        </Modal>
      )}
    </div>
  );
}

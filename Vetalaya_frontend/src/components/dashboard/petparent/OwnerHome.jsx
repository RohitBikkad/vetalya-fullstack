import React, { useState } from "react";
import { PawPrint } from "lucide-react";
import {
  appointmentLabel,
  safeExternalUrl,
  timeLabel,
} from "../../../../../packages/owner-domain/src/model.js";
import "./owner-figma.css";

export const ownerTabs = [
  ["home", "Home", "home"],
  ["pets", "My Pets", "paw"],
  ["appointments", "Appointments", "calendar"],
  ["passport", "Passport", "passport"],
  ["profile", "Profile", "profile"],
];
export function DesignIcon({ name, size = 20 }) {
  return (
    <img
      className="figma-icon"
      src={`/figma-owner/${name}.svg`}
      width={size}
      height={size}
      alt=""
      aria-hidden="true"
    />
  );
}
export default function OwnerHome({
  pet,
  pets,
  passport,
  next,
  currentUser,
  setPetId,
  navigate,
  open,
  detailLoading,
  detailErrors,
}) {
  const [failedPhoto, setFailedPhoto] = useState("");
  const hour = Number(
    new Intl.DateTimeFormat("en-GB", {
      hour: "numeric",
      hourCycle: "h23",
      timeZone: "Asia/Kolkata",
    }).format(new Date()),
  );
  const greeting =
    hour < 12
      ? "Good morning!"
      : hour < 17
        ? "Good afternoon!"
        : "Good evening!";
  const initials = (currentUser?.name || "Pet parent")
    .split(" ")
    .map((p) => p[0])
    .slice(0, 2)
    .join("");
  const photo = safeExternalUrl(pet?.photoUrl) && failedPhoto !== pet.photoUrl;
  const datePart = (part) =>
    next
      ? new Intl.DateTimeFormat("en-IN", {
          [part]: part === "day" ? "numeric" : "short",
          timeZone: "Asia/Kolkata",
        }).format(new Date(next.appointmentAt))
      : "";
  return (
    <div className="figma-home">
      <header className="figma-greeting">
        <div>
          <div
            className="figma-official-brand"
            aria-label="Vetalya · Every Pet. Health. Always."
          >
            <img className="brand-emblem" src="/vetalya-logo.png" alt="" />
            <img
              className="brand-wordmark"
              src="/vetalya-wordmark.png"
              alt="Vetalya · Every Pet. Health. Always."
            />
          </div>
          <h1>{greeting}</h1>
          <span>
            {pet
              ? `How is ${pet.name} feeling today?`
              : "Let’s begin their health story."}
          </span>
        </div>
        <button
          className="figma-parent"
          aria-label="Open my profile"
          onClick={() => navigate("profile")}
        >
          {initials}
        </button>
      </header>
      {pet ? (
        <section className="figma-pet-card" aria-label={`${pet.name} profile`}>
          <div className="figma-pet-details">
            <label htmlFor="home-pet">MY PET</label>
            <div className="figma-pet-select">
              <select
                id="home-pet"
                aria-label="Selected pet"
                value={pet.petId}
                onChange={(e) => setPetId(e.target.value)}
              >
                {pets.map((p) => (
                  <option key={p.petId} value={p.petId}>
                    {p.name}
                  </option>
                ))}
              </select>
            </div>
            <p>{pet.breed || pet.species || "Breed not recorded"}</p>
            <p>
              {pet.age || "Age not recorded"}
              {pet.gender && pet.gender !== "UNKNOWN"
                ? ` · ${pet.gender.charAt(0)}${pet.gender.slice(1).toLowerCase()}`
                : ""}
            </p>
            <button onClick={() => navigate("passport")}>View profile</button>
          </div>
          <div className="figma-pet-portrait">
            {photo ? (
              <img
                src={pet.photoUrl}
                alt={pet.name}
                onError={() => setFailedPhoto(pet.photoUrl)}
              />
            ) : (
              <PawPrint size={66} aria-label="No pet photo" />
            )}
          </div>
        </section>
      ) : (
        <section className="figma-pet-card figma-no-pet">
          <h2>Meet your companion</h2>
          <p>Add your first pet to start their care journey.</p>
          <button className="owner-button" onClick={() => open("pet")}>
            Add a pet
          </button>
        </section>
      )}
      <section className="figma-overview">
        <h2>Today’s overview</h2>
        <div className="figma-summary-grid">
          {[
            [
              "♡",
              "Health",
              detailLoading ? "Loading records…" : "View care records",
              "success",
              () => navigate("care"),
            ],
            [
              "◷",
              "Meals",
              "Not tracked",
              "warn",
              () => open("tracking", { kind: "Meals" }),
            ],
            [
              "↗",
              "Activity",
              "Not tracked",
              "mint",
              () => open("tracking", { kind: "Activity" }),
            ],
            [
              "✓",
              "Weight",
              detailLoading
                ? "Loading…"
                : passport?.weightKg != null
                  ? `${passport.weightKg} kg`
                  : "Not recorded",
              "neutral",
              () => navigate("passport"),
            ],
          ].map(([symbol, title, value, tone, action]) => (
            <button className="figma-summary" key={title} onClick={action}>
              <span className={`figma-summary-symbol ${tone}`}>{symbol}</span>
              <span>
                <strong>{title}</strong>
                <small>{value}</small>
              </span>
            </button>
          ))}
        </div>
        {detailErrors.length > 0 && (
          <p className="figma-detail-error" role="alert">
            Some health records couldn’t load. Open Passport to retry.
          </p>
        )}
      </section>
      <section className="figma-upcoming">
        <div className="figma-section-heading">
          <h2>Upcoming appointment</h2>
          <button onClick={() => navigate("appointments")}>See all</button>
        </div>
        {next ? (
          <button
            className="figma-appointment"
            onClick={() => navigate("appointments")}
          >
            <span className="figma-appointment-date">
              <strong>{datePart("day")}</strong>
              <small>{datePart("month")}</small>
            </span>
            <span className="figma-appointment-info">
              <span className="figma-visit-time">
                {timeLabel(next.appointmentAt)}
              </span>
              <strong>{next.reason || "Veterinary consultation"}</strong>
              <small>{next.hospital?.name || "Hospital not recorded"}</small>
              <span className="figma-visit-meta">
                <span>
                  <span className="figma-mini-paw">
                    <DesignIcon name="pet-paw" size={14} />
                  </span>
                  {next.pet?.name || pet?.name}
                </span>
                <span className="figma-status">
                  {appointmentLabel(next.status)}
                </span>
              </span>
            </span>
          </button>
        ) : (
          <div className="figma-appointment figma-appointment-empty">
            <span className="figma-appointment-date">
              <DesignIcon name="calendar" size={24} />
            </span>
            <div>
              <strong>No upcoming appointments</strong>
              <p>Plan the next visit when they need care.</p>
              <button onClick={() => open("book")}>Book a vet visit</button>
            </div>
          </div>
        )}
      </section>
      <section className="figma-actions">
        <h2>Quick actions</h2>
        <div>
          {[
            ["book-vet", "Book vet", "mint", () => open("book")],
            ["reminder", "Reminder", "warn", () => navigate("care")],
            ["records", "Records", "neutral", () => navigate("records")],
            ["emergency", "Emergency", "danger", () => open("emergency")],
          ].map(([icon, label, tone, action]) => (
            <button key={label} onClick={action}>
              <span className={tone}>
                <DesignIcon name={icon} />
              </span>
              <small>{label}</small>
            </button>
          ))}
        </div>
      </section>
    </div>
  );
}

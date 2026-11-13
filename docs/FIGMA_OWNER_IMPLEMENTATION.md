# Figma owner Home implementation

Visual source: [Vetalya App — Home, node 14:345](https://www.figma.com/design/GDyKkUhdVLKu8XCtxtObUJ/Vetalya-App?node-id=14-345). Figma context and screenshot were read through the Figma plugin; no design file was modified.

## Scope

The linked Home frame is implemented in responsive React/Vite and Expo React Native. It replaces the previous owner Home composition with the greeting, mint pet profile, four overview tiles, appointment card, quick actions and five-tab navigation: Home, My Pets, Appointments, Passport and Profile. Records and Care remain accessible through quick actions. Shared owner colors/typography now use the supplied green design tokens and Plus Jakarta Sans. Existing clinic and platform workspaces are unchanged.

Desktop Home adapts the same sections into columns; no desktop design was supplied. Other owner pages retain their existing layouts with the shared owner theme. Native device status bars are provided by the OS; the browser does not fake the reference's 9:41 or signal/battery indicators.

## Real data and intentional differences

Selected pet name, breed, age, gender, photograph, passport weight and appointment clinic/date/reason/status come from the API. The reference's Simba image is a sample pet portrait, not a replacement for a real pet's photograph. It is archived locally for reference only and is not rendered as another pet. Missing or failed pet photos display an accessible fallback.

The greeting uses current India time. A BOOKED appointment remains labelled Booked, not Confirmed. Health opens care records without declaring the animal healthy. Meals and Activity explicitly say Not tracked and explain the missing server integration. Weight does not make an unsupported “Normal” assessment. Empty, loading and error states remain functional. Urgent-care contact actions show actual directory results and do not claim emergency availability.

## Assets

Downloaded, unmodified Figma SVG files are in `Vetalaya_frontend/public/figma-owner` and `apps/pet-owner-mobile/assets/figma-owner`. No temporary Figma asset URL is used at runtime. Web callsites are `OwnerHome.jsx` / `DesignIcon`; native callsites are Home / `DesignIcon.tsx` / the tabs layout.

| Layer / position | Local asset | Rendered size |
|---|---|---|
| Bottom tabs, Home / My Pets / Appointments / Passport / Profile | home.svg / paw.svg / calendar.svg / passport.svg / profile.svg | 24 × 24 |
| Appointment pet row | pet-paw.svg | 14 × 14 |
| Quick actions, Book vet / Reminder / Records / Emergency | book-vet.svg / reminder.svg / records.svg / emergency.svg | 20 × 20 |
| Selected pet portrait (API, not static reference) | API photo URL | 146 × 146, cover |

Plus Jakarta Sans is bundled locally: web font files originate from the installed open-source `@expo-google-fonts/plus-jakarta-sans` package; Expo loads only regular, medium and semibold weights. Expo-compatible `expo-image` renders local SVG files.

## Verification

Vite production build; native TypeScript / Expo lint; Android and iOS bundle export; existing shared API/domain/role routing tests. Browser comparison at 390 × 844 verified all ten Figma icons loaded with exact slot dimensions, actual pet/weight/appointment data, tracking-pending dialog, booking dialog and clinic-directory action. Desktop responsive layout was reviewed separately. Bundles do not establish physical-device validation, APK/IPA signing or store readiness.

Restart Expo after dependency/config changes. If using a development build, rebuild it to include `expo-image` and font plugins. Run/setup instructions remain in `apps/pet-owner-mobile/README.md`.

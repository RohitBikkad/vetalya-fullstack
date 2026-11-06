# Vetalya Standalone Auth Package (`vetalya-auth/`)

> **Standalone, self-contained Login & Signup experience for the Vetalya Community Platform.**  
> Contains the full authentication UI, Google SSO button, role simulation hooks, Tailwind design tokens, and high-resolution transparent assets.

---

## 📁 Directory Structure

```
vetalya-auth/
├── package.json                   # Standalone dependencies (React 19, Lucide, Tailwind, Vite)
├── vite.config.js                 # Standalone Vite configuration (Port 3001)
├── tailwind.config.js             # Vetalya auth color palette, typography & shadows
├── postcss.config.js              # PostCSS autoprefixer pipeline
├── index.html                     # HTML root with Plus Jakarta Sans & Inter fonts
├── README.md                      # Documentation & integration guide
│
├── public/                        # Transparent High-Resolution Assets
│   ├── vetalya-logo.png           # Transparent official canine/feline heart emblem
│   └── vetalya-wordmark.png       # Transparent wordmark with teal paw print & tagline
│
└── src/
    ├── main.jsx                   # React 19 DOM entry point
    ├── App.jsx                    # Root view switcher (Login/Signup vs Authenticated Profile)
    ├── index.css                  # Tailwind styles and background resets
    │
    ├── context/
    │   └── AuthContext.jsx        # Complete useAuth hook, login, signup, Google SSO & demo roles
    │
    └── components/
        └── auth/
            ├── LoginPage.jsx      # Two-column master presentation orchestrator
            ├── AuthCard.jsx       # Tabbed Log in & Sign up form card with password eye toggle
            ├── AuthIllustration.jsx # Left column headline, value checkmarks & emblem
            └── VetalyaBrandLogo.jsx # Header emblem + wordmark brand unit
```

---

## 🚀 Running Independently

```bash
# 1. Navigate to the vetalya-auth directory
cd /Users/rohit/Documents/V1_Vetalya/vetalya-auth

# 2. Install dependencies
npm install

# 3. Start development server
npm run dev
# Opens at: http://127.0.0.1:3001/

# 4. Build for production
npm run build
```

---

## 🔌 Importing into Any React Application

You can drop the `src/components/auth/` components and `src/context/AuthContext.jsx` directly into any other React / Next.js / Vite codebase:

```jsx
import React from 'react';
import { AuthProvider } from './context/AuthContext';
import LoginPage from './components/auth/LoginPage';

export default function MyAuthModule() {
  return (
    <AuthProvider>
      <LoginPage />
    </AuthProvider>
  );
}
```

---

## 🎨 Design Tokens Included
* **Navy Background & Text**: `#10324E`
* **Teal Brand Accent**: `#2D8A7A`
* **Peach Graphic Slices**: `#F49E78`
* **Warm Porcelain Canvas**: `#F7F5EE`
* **Mint Graphic Halo**: `#D8EFE4`
* **Card Corner Radii**: `rounded-[32px]`
* **Pill-shaped Inputs**: `rounded-2xl`

/**
 * Vetalya Indian Veterinary Drug Formulary, Vaccine Catalog & Clinical Standards
 */

export const INDIAN_VET_FORMULARY = [
  // Antibiotics & Antimicrobials
  { name: 'Monocef Vet (Ceftriaxone)', category: 'Antibiotic', formulation: 'Injectable (500mg/1g)', defaultDosage: '20-25 mg/kg IV/IM', frequency: 'BID (Twice daily)', duration: '5 days', instructions: 'Reconstitute fresh with sterile water' },
  { name: 'Clavam Vet (Amoxicillin + Clavulanic Acid)', category: 'Antibiotic', formulation: 'Tablet (375mg/625mg)', defaultDosage: '12.5-20 mg/kg PO', frequency: 'BID (Twice daily)', duration: '7 days', instructions: 'Administer with food' },
  { name: 'Baytril (Enrofloxacin)', category: 'Antibiotic', formulation: 'Tablet (50mg/150mg) / Injectable', defaultDosage: '5 mg/kg PO/SC', frequency: 'SID (Once daily)', duration: '5-7 days', instructions: 'Avoid in growing pups <8 months' },
  { name: 'Cefpet (Cefpodoxime Proxetil)', category: 'Antibiotic', formulation: 'Dry Syrup / Tablet (100mg/200mg)', defaultDosage: '5-10 mg/kg PO', frequency: 'SID (Once daily)', duration: '7 days', instructions: 'Give with or after food' },
  { name: 'Doxypet (Doxycycline)', category: 'Antibiotic', formulation: 'Tablet (100mg/300mg)', defaultDosage: '10 mg/kg PO', frequency: 'SID (Once daily)', duration: '21-28 days', instructions: 'Mandatory follow with water to prevent esophageal stricture' },
  { name: 'Metrogyl (Metronidazole)', category: 'Antiprotozoal / Antibacterial', formulation: 'Suspension / Tablet (200mg/400mg)', defaultDosage: '15 mg/kg PO', frequency: 'BID (Twice daily)', duration: '5 days', instructions: 'For acute diarrhea / Giardia' },

  // NSAIDs & Analgesics
  { name: 'Melonex (Meloxicam)', category: 'NSAID / Anti-inflammatory', formulation: 'Oral Suspension (1.5mg/ml) / Inj', defaultDosage: '0.1-0.2 mg/kg PO', frequency: 'SID (Once daily)', duration: '3-5 days', instructions: 'Strictly after food. Ensure proper hydration' },
  { name: 'Rimadyl (Carprofen)', category: 'NSAID / Orthopedic Pain', formulation: 'Chewable Tablet (25mg/75mg/100mg)', defaultDosage: '2-4 mg/kg PO', frequency: 'SID or divided BID', duration: '5 days', instructions: 'Monitor liver/kidney values for long term' },
  { name: 'TramaVet (Tramadol HCl)', category: 'Analgesic', formulation: 'Tablet (50mg) / Injectable', defaultDosage: '2-4 mg/kg PO', frequency: 'TID (Thrice daily)', duration: '3 days', instructions: 'For acute post-op or orthopedic pain' },
  { name: 'Gabapin (Gabapentin)', category: 'Neuropathic Pain / Sedation', formulation: 'Capsule (100mg/300mg)', defaultDosage: '10-20 mg/kg PO', frequency: 'BID (Twice daily)', duration: '7-14 days', instructions: 'For nerve pain or pre-clinic anxiety' },

  // Ecto & Endoparasiticides
  { name: 'Bravecto (Fluralaner)', category: 'Ectoparasiticide', formulation: 'Chewable Tablet / Spot-on', defaultDosage: '1 dose per weight bracket', frequency: 'Once every 3 months', duration: '90 days protection', instructions: 'Kills ticks & fleas within 12 hours' },
  { name: 'Nexgard Spectra (Afoxolaner + Milbemycin)', category: 'Broad Spectrum Antiparasitic', formulation: 'Chewable Tablet', defaultDosage: '1 chew per weight bracket', frequency: 'Monthly', duration: '30 days protection', instructions: 'Heartworm + Ticks + Fleas + Intestinal worms' },
  { name: 'Simparica Trio (Sarolaner + Moxidectin + Pyrantel)', category: 'Broad Spectrum Antiparasitic', formulation: 'Chewable Tablet', defaultDosage: '1 chew per weight bracket', frequency: 'Monthly', duration: '30 days protection', instructions: 'Comprehensive triple protection' },
  { name: 'Drontal Plus (Praziquantel + Pyrantel + Febantel)', category: 'Dewormer', formulation: 'Tablet (1 tab per 10kg)', defaultDosage: '1 tab per 10kg bodyweight', frequency: 'Repeat after 14 days if heavy load', duration: 'Single dose', instructions: 'Quarterly routine deworming' },
  { name: 'Fiprofort Plus (Fipronil + (S)-Methoprene)', category: 'Spot-On', formulation: 'Pipette Spot-On', defaultDosage: 'Apply between shoulder blades', frequency: 'Monthly', duration: '30 days protection', instructions: 'Do not bathe pet 48 hrs before/after' },

  // Gastrointestinal & Hepatoprotective
  { name: 'Pantocid (Pantoprazole)', category: 'Gastroprotective / PPI', formulation: 'Tablet (20mg/40mg) / IV Inj', defaultDosage: '1 mg/kg PO/IV', frequency: 'SID (Once daily in morning)', duration: '5 days', instructions: '30 minutes before first meal' },
  { name: 'Emeset (Ondansetron)', category: 'Antiemetic', formulation: 'Syrup (2mg/5ml) / Injectable', defaultDosage: '0.2-0.5 mg/kg PO/IV', frequency: 'BID or TID as needed', duration: '2-3 days', instructions: 'Control severe vomiting / Parvovirus nausea' },
  { name: 'Liv.52 Pet', category: 'Hepatoprotective Herbal', formulation: 'Liquid / Drops', defaultDosage: '5-10 ml PO', frequency: 'BID (Twice daily)', duration: '30 days', instructions: 'Supportive liver therapy' },
  { name: 'Samylin (SAMe + Silybin)', category: 'Hepatic Support', formulation: 'Sachets / Tablets', defaultDosage: '1 sachet per weight class', frequency: 'SID (Once daily)', duration: '30 days', instructions: 'Give on empty stomach' },
  { name: 'Enterogermina (Bacillus clausii spores)', category: 'Probiotic', formulation: 'Mini-bottles 5ml', defaultDosage: '1 vial PO', frequency: 'BID (Twice daily)', duration: '5 days', instructions: 'Restore gut flora after antibiotic course' },

  // Dermatology & Allergy
  { name: 'Apoquel (Oclacitinib)', category: 'Antipruritic / Atopic Dermatitis', formulation: 'Tablet (3.6mg/5.4mg/16mg)', defaultDosage: '0.4-0.6 mg/kg PO', frequency: 'BID for 14 days, then SID', duration: '14-30 days', instructions: 'Controls allergic itch without steroid side effects' },
  { name: 'Wysolone (Prednisolone)', category: 'Corticosteroid', formulation: 'Tablet (5mg/10mg/20mg)', defaultDosage: '0.5-1 mg/kg PO tapering', frequency: 'SID morning', duration: '5-7 days tapering', instructions: 'Strictly with food. Never stop abruptly' },
  { name: 'Kiskin Ointment (Clobetasol + Ofloxacin + Miconazole)', category: 'Topical Antibacterial & Antifungal', formulation: 'Ointment Tube', defaultDosage: 'Thin film application', frequency: 'BID (Twice daily)', duration: '7-10 days', instructions: 'Prevent licking with e-collar' },
  { name: 'Ketochlor / Chlorhexidine Shampoo', category: 'Medicated Shampoo', formulation: 'Shampoo (200ml)', defaultDosage: 'Lather and leave for 10 minutes', frequency: 'Twice weekly', duration: '3-4 weeks', instructions: 'Rinse thoroughly with lukewarm water' }
];

export const VACCINE_CATALOG = [
  // Canine Vaccines
  { name: 'Nobivac DHPPi + L (7-in-1 / 9-in-1)', type: 'CORE_PUPPY', species: 'Canine', defaultBoosterDays: 21, annualRenewalDays: 365, description: 'Distemper, Hepatitis, Parvovirus, Parainfluenza & Leptospirosis' },
  { name: 'Nobivac Rabies (Anti-Rabies Vaccine)', type: 'MANDATORY_RABIES', species: 'Canine', defaultBoosterDays: 30, annualRenewalDays: 365, description: 'Statutory Inactivated Rabies Vaccine (VCI & Municipal compliant)' },
  { name: 'Nobivac KC (Kennel Cough / Bordetella)', type: 'LIFESTYLE', species: 'Canine', defaultBoosterDays: 365, annualRenewalDays: 365, description: 'Intranasal / Oral Bordetella bronchiseptica vaccine for boarding pets' },
  { name: 'Corona Vet (Canine Coronavirus)', type: 'OPTIONAL', species: 'Canine', defaultBoosterDays: 21, annualRenewalDays: 365, description: 'Canine enteric coronavirus prevention' },

  // Feline Vaccines
  { name: 'Felocell 4 / Tricat (FPV + FHV + FCV)', type: 'CORE_FELINE', species: 'Feline', defaultBoosterDays: 21, annualRenewalDays: 365, description: 'Feline Panleukopenia, Herpesvirus, Calicivirus & Chlamydia' },
  { name: 'Nobivac Rabies for Cats', type: 'MANDATORY_RABIES', species: 'Feline', defaultBoosterDays: 365, annualRenewalDays: 365, description: 'Statutory Feline Anti-Rabies Vaccine' }
];

export const DIAGNOSTIC_TESTS = [
  { name: 'Complete Blood Count (CBC) with Platelet Count', category: 'Hematology', turnaround: 'Same Day (2 hours)' },
  { name: 'Comprehensive Serum Biochemistry (12 Parameters)', category: 'Biochemistry', turnaround: 'Same Day (4 hours)' },
  { name: 'Renal Function Profile (BUN, Creatinine, Electrolytes)', category: 'Biochemistry', turnaround: '2 hours' },
  { name: 'Hepatic Function Profile (SGPT/ALT, SGOT, Alk Phos, Bilirubin)', category: 'Biochemistry', turnaround: '2 hours' },
  { name: 'Canine Parvovirus / Coronavirus Antigen Rapid Card', category: 'Infectious Disease', turnaround: '15 minutes' },
  { name: 'Canine Tick-Borne 4Dx Snap (Ehrlichia, Anaplasma, Lyme, Heartworm)', category: 'Vector Borne', turnaround: '15 minutes' },
  { name: 'Routine Urinalysis with Microscopic Sediment', category: 'Pathology', turnaround: '2 hours' },
  { name: 'Deep Skin Scraping & Wood’s Lamp (Mites & Demodex)', category: 'Dermatology', turnaround: '30 minutes' },
  { name: 'Digital Radiography (X-Ray) - 2 Views (Orthogonal)', category: 'Imaging', turnaround: 'Immediate' },
  { name: 'Abdominal Diagnostic Ultrasound (USG)', category: 'Imaging', turnaround: 'Immediate' }
];

export const COMMON_DIAGNOSES = [
  'Acute Gastroenteritis (Bacterial / Dietary Indiscretion)',
  'Canine Parvoviral Enteritis',
  'Canine Ehrlichiosis (Tick Fever)',
  'Atopic Dermatitis & Flea Allergy Dermatitis (FAD)',
  'Demodectic Mange / Scabies',
  'Bilateral Otitis Externa (Malassezia / Bacterial)',
  'Canine Infectious Respiratory Disease Complex (Kennel Cough)',
  'Cranial Cruciate Ligament (CCL) Rupture / Stifle Sprain',
  'Chronic Kidney Disease (CKD) - IRIS Stage II',
  'Benign Prostatic Hyperplasia (BPH)',
  'Feline Lower Urinary Tract Disease (FLUTD / FUS)',
  'Corneal Ulcer (Fluorescein Positive)'
];

export const STATE_VETERINARY_COUNCILS = [
  'Veterinary Council of India (VCI - Central Registry)',
  'Maharashtra State Veterinary Council (MSVC)',
  'Karnataka Veterinary Council (KVC)',
  'Tamil Nadu State Veterinary Council (TNSVC)',
  'Delhi Veterinary Council (DVC)',
  'Kerala State Veterinary Council (KSVC)',
  'Uttar Pradesh Veterinary Council (UPVC)',
  'West Bengal Veterinary Council (WBVC)',
  'Gujarat Veterinary Council (GVC)',
  'Telangana State Veterinary Council (TSVC)',
  'Andhra Pradesh State Veterinary Council (APSVC)',
  'Punjab State Veterinary Council (PSVC)',
  'Rajasthan State Veterinary Council (RSVC)',
  'Madhya Pradesh State Veterinary Council (MPSVC)'
];

export const VET_SPECIALIZATIONS = [
  'Small Animal Internal Medicine',
  'Veterinary Orthopedic Surgery & Traumatology',
  'Veterinary Soft Tissue Surgery',
  'Veterinary Dermatology & Allergology',
  'Feline Exclusive Medicine',
  'Canine & Feline Cardiology',
  'Veterinary Ophthalmology',
  'Veterinary Dentistry & Oral Surgery',
  'Radiology & Diagnostic Imaging (USG / X-Ray)',
  'Exotic Pets, Avian & Pocket Pet Medicine',
  'Critical Care, Emergency & Anesthesia',
  'Veterinary Oncology'
];


import React from 'react';
import { Check } from 'lucide-react';
import VetalyaBrandLogo from './VetalyaBrandLogo';

/**
 * AuthIllustration
 * Left-side presentation column matching the user's reference design:
 * Brand crest, "Where every pet parent belongs" display copy, check list,
 * organic peach backdrop slice, and the central heart emblem with companion silhouette.
 */
export default function AuthIllustration() {
  const valueProps = [
    "Share your pet's stories",
    'Talk with fellow pet parents',
    'Get trusted pet care tips',
  ];

  return (
    <div className="relative flex flex-col justify-between h-full py-4 lg:py-8 pr-0 lg:pr-12">
      {/* Brand Header */}
      <div>
        <VetalyaBrandLogo />

        {/* Display Typography */}
        <div className="mt-10 sm:mt-12 space-y-4 max-w-lg">
          <h1 className="text-4xl sm:text-5xl lg:text-[56px] font-extrabold text-[#10324E] tracking-tight leading-[1.08] font-display">
            Where every <span className="text-[#2D8A7A]">pet</span>
            <br />
            <span className="text-[#2D8A7A]">parent</span> belongs.
          </h1>

          <p className="text-base sm:text-lg text-[#242826]/75 font-normal italic leading-relaxed pt-1">
            Stories, conversations and better pet care — all in one friendly community.
          </p>
        </div>
      </div>

      {/* Middle & Bottom: Value Props & Emblem Graphics */}
      <div className="mt-12 lg:mt-16 flex flex-col sm:flex-row items-start sm:items-end justify-between gap-8 relative">
        
        {/* Value Proposition List */}
        <div className="space-y-3.5 relative z-10">
          {valueProps.map((item, idx) => (
            <div key={idx} className="flex items-center space-x-3">
              <div className="w-5 h-5 rounded-full bg-[#2D8A7A] text-white flex items-center justify-center shrink-0 shadow-xs">
                <Check className="w-3.5 h-3.5 stroke-[3]" />
              </div>
              <span className="text-sm sm:text-base font-bold text-[#10324E]">
                {item}
              </span>
            </div>
          ))}
        </div>

        {/* Heart Emblem on Mint Circle (Official Vetalya Emblem) */}
        <div className="relative self-center sm:self-auto sm:ml-auto">
          {/* Mint Circle Halo */}
          <div className="w-52 h-52 sm:w-64 sm:h-64 rounded-full bg-[#D8EFE4]/80 absolute -top-6 -left-6 sm:-top-8 sm:-left-8 blur-xs pointer-events-none" />

          {/* Official Vetalya Heart Emblem */}
          <div className="relative z-10 w-44 h-44 sm:w-52 sm:h-52 flex items-center justify-center">
            <img
              src="/vetalya-logo.png"
              alt="Vetalya Companion Healthcare Emblem"
              className="w-full h-full object-contain drop-shadow-md hover:scale-105 transition-transform"
            />
          </div>
        </div>

      </div>
    </div>
  );
}

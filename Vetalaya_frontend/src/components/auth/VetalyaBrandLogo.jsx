import React from 'react';

/**
 * VetalyaBrandLogo
 * Official Vetalya Brand Identity:
 * Combines the transparent heart emblem (canine & feline silhouettes + cross)
 * followed by the official Vetalya wordmark with teal paw print accent
 * and "Every Pet. Health. Always." tagline.
 */
export default function VetalyaBrandLogo({ className = '', emblemSize = 'w-12 h-12', wordmarkHeight = 'h-10 sm:h-12' }) {
  return (
    <div className={`inline-flex items-center gap-3 ${className}`}>
      {/* Official Vetalya Emblem */}
      <div className={`relative ${emblemSize} shrink-0`}>
        <img
          src="/vetalya-logo.png"
          alt="Vetalya Emblem"
          className="w-full h-full object-contain drop-shadow-xs"
        />
      </div>

      {/* Official Vetalya Wordmark (with teal paw over 'y' & 'Every Pet. Health. Always.') */}
      <div className="flex items-center shrink-0">
        <img
          src="/vetalya-wordmark.png"
          alt="Vetalya — Every Pet. Health. Always."
          className={`${wordmarkHeight} w-auto object-contain`}
        />
      </div>
    </div>
  );
}

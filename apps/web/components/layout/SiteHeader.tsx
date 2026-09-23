// apps/web/components/layout/SiteHeader.tsx

"use client";

import { useState } from "react";

import { auraBrand, auraNavigation } from "@/data/aura";
import { ArrowIcon } from "@/components/ui/ArrowIcon";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Site Header
========================================================= */

function MenuIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
    >
      <path d="M4 7h16" />
      <path d="M4 12h16" />
      <path d="M4 17h16" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-5 w-5"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
    >
      <path d="M6 6l12 12" />
      <path d="M18 6 6 18" />
    </svg>
  );
}

export function SiteHeader() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  function closeMobileMenu() {
    setMobileMenuOpen(false);
  }

  return (
    <header className="sticky top-0 z-50 border-b border-[#2d2824]/10 bg-[#f8f5f0]/90 backdrop-blur-xl">
      <div className="mx-auto flex h-[76px] w-full max-w-[1440px] items-center justify-between px-5 sm:px-8 lg:px-12 xl:px-16">
        {/* =====================================================
            BRAND
        ====================================================== */}
        <a
          href="#inicio"
          aria-label={`${auraBrand.name} - Inicio`}
          className="group flex shrink-0 items-center gap-3"
          onClick={closeMobileMenu}
        >
          <span className="flex h-10 w-10 items-center justify-center rounded-full border border-[#7c6654]/30 bg-[#eee5db] font-serif text-lg text-[#665143] transition-transform duration-300 group-hover:scale-105">
            {auraBrand.monogram}
          </span>

          <span className="leading-none">
            <span className="block font-serif text-[19px] tracking-[0.16em] text-[#2b2521]">
              {auraBrand.shortName.toUpperCase()}
            </span>

            <span className="mt-1 block text-[9px] font-medium uppercase tracking-[0.34em] text-[#8a7565]">
              Studio
            </span>
          </span>
        </a>

        {/* =====================================================
            DESKTOP NAVIGATION
        ====================================================== */}
        <nav
          className="hidden items-center gap-8 text-[13px] font-medium text-[#62574f] lg:flex"
          aria-label="Navegación principal"
        >
          {auraNavigation.map((item) => (
            <a
              key={item.href}
              href={item.href}
              className="relative transition-colors duration-200 hover:text-[#2a2420] after:absolute after:-bottom-2 after:left-0 after:h-px after:w-0 after:bg-[#8b6d59] after:transition-all after:duration-300 hover:after:w-full"
            >
              {item.label}
            </a>
          ))}
        </nav>

        {/* =====================================================
            DESKTOP CTA
        ====================================================== */}
        <div className="hidden items-center gap-3 sm:flex">
          <a
            href="#contacto"
            className="group inline-flex h-11 items-center justify-center gap-2 rounded-full bg-[#2c2622] px-6 text-[11px] font-semibold uppercase tracking-[0.18em] !text-[#fffaf6] shadow-[0_8px_24px_rgba(44,38,34,0.14)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-[#4b3d34] hover:shadow-[0_12px_30px_rgba(44,38,34,0.20)]"
          >
            Pedir cita

            <span className="transition-transform duration-300 group-hover:translate-x-0.5">
              <ArrowIcon />
            </span>
          </a>
        </div>

        {/* =====================================================
            MOBILE MENU BUTTON
        ====================================================== */}
        <button
          type="button"
          aria-label={
            mobileMenuOpen
              ? "Cerrar menú de navegación"
              : "Abrir menú de navegación"
          }
          aria-expanded={mobileMenuOpen}
          aria-controls="mobile-navigation"
          onClick={() => setMobileMenuOpen((current) => !current)}
          className="flex h-11 w-11 items-center justify-center rounded-full border border-[#3b332e]/15 bg-white/30 text-[#312a26] transition-all duration-200 hover:border-[#3b332e]/30 hover:bg-white/60 sm:hidden"
        >
          {mobileMenuOpen ? <CloseIcon /> : <MenuIcon />}
        </button>
      </div>

      {/* =======================================================
          MOBILE NAVIGATION
      ======================================================== */}
      <div
        id="mobile-navigation"
        className={[
          "overflow-hidden border-t border-[#2d2824]/10 bg-[#f8f5f0] transition-all duration-300 sm:hidden",
          mobileMenuOpen
            ? "max-h-[520px] opacity-100"
            : "pointer-events-none max-h-0 opacity-0",
        ].join(" ")}
      >
        <div className="mx-auto w-full max-w-[1440px] px-5 py-6">
          <nav
            aria-label="Navegación móvil"
            className="flex flex-col"
          >
            {auraNavigation.map((item) => (
              <a
                key={item.href}
                href={item.href}
                onClick={closeMobileMenu}
                className="flex items-center justify-between border-b border-[#332b26]/10 py-4 text-base font-medium text-[#40362f] transition-colors duration-200 hover:text-[#8b6d59]"
              >
                <span>{item.label}</span>

                <ArrowIcon className="h-4 w-4" />
              </a>
            ))}
          </nav>

          <a
            href="#contacto"
            onClick={closeMobileMenu}
            className="group mt-6 inline-flex h-14 w-full items-center justify-center gap-3 rounded-full bg-[#2c2622] px-7 text-[11px] font-semibold uppercase tracking-[0.18em] !text-[#fffaf6] shadow-[0_10px_28px_rgba(44,38,34,0.14)] transition-all duration-300 hover:bg-[#4b3d34]"
          >
            Pedir cita

            <span className="transition-transform duration-300 group-hover:translate-x-1">
              <ArrowIcon />
            </span>
          </a>

          <div className="mt-6 border-t border-[#332b26]/10 pt-5">
            <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-[#9a806d]">
              {auraBrand.tagline}
            </p>

            <p className="mt-2 text-sm leading-6 text-[#756a61]">
              {auraBrand.description}
            </p>
          </div>
        </div>
      </div>
    </header>
  );
}
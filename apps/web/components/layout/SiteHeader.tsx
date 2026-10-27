// apps/web/components/layout/SiteHeader.tsx

"use client";

import { useState, useSyncExternalStore } from "react";

import { business } from "@/config/business";
import { ArrowIcon } from "@/components/ui/ArrowIcon";

type HeaderChrome = {
  onVideo: boolean;
  scrolled: boolean;
};

const AURA_CHROME: HeaderChrome = { onVideo: false, scrolled: false };
const PAGE_SCROLLED_CHROME: HeaderChrome = { onVideo: false, scrolled: true };
const VIDEO_TOP_CHROME: HeaderChrome = { onVideo: true, scrolled: false };
const VIDEO_SCROLLED_CHROME: HeaderChrome = { onVideo: true, scrolled: true };

let chromeCache: HeaderChrome = VIDEO_TOP_CHROME;

function subscribeHeaderChrome(onChange: () => void) {
  window.addEventListener("scroll", onChange, { passive: true });
  document.addEventListener("scroll", onChange, { passive: true, capture: true });
  window.addEventListener("resize", onChange, { passive: true });

  return () => {
    window.removeEventListener("scroll", onChange);
    document.removeEventListener("scroll", onChange, true);
    window.removeEventListener("resize", onChange);
  };
}

function readHeaderChrome(): HeaderChrome {
  const scrolledPx = Math.max(
    window.scrollY,
    document.documentElement.scrollTop,
    document.body.scrollTop,
  );
  const scrolled = scrolledPx > 12;
  const probe = 48;
  const selector =
    business.theme === "taller"
      ? "[data-scroll-sequence], [data-header-ink='light']"
      : "[data-scroll-sequence]";
  const zones = document.querySelectorAll(selector);
  let onVideo = false;
  for (const zone of zones) {
    const rect = zone.getBoundingClientRect();
    if (rect.top <= probe && rect.bottom > probe) {
      onVideo = true;
      break;
    }
  }

  const next: HeaderChrome = onVideo
    ? scrolled
      ? VIDEO_SCROLLED_CHROME
      : VIDEO_TOP_CHROME
    : scrolled
      ? PAGE_SCROLLED_CHROME
      : AURA_CHROME;

  if (
    chromeCache.onVideo === next.onVideo &&
    chromeCache.scrolled === next.scrolled
  ) {
    return chromeCache;
  }

  chromeCache = next;
  return chromeCache;
}

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Site Header
========================================================= */

const CONTACT_HREF = "#contacto";

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

export function SiteHeader({
  overSequence = false,
}: {
  overSequence?: boolean;
}) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const chrome = useSyncExternalStore(
    subscribeHeaderChrome,
    readHeaderChrome,
    () => (overSequence ? VIDEO_TOP_CHROME : AURA_CHROME),
  );
  const onVideo = chrome.onVideo;
  const auraOnVideo = onVideo && business.theme !== "taller";
  // La home lleva overflow-x: hidden, así que sticky no se ancla.
  // Cambiar fixed/sticky al cruzar el vídeo mueve el layout y el
  // snapshot rebota: el header se queda fixed en toda la secuencia.
  const pinHeader = overSequence || onVideo || business.theme === "taller";

  function closeMobileMenu() {
    setMobileMenuOpen(false);
  }

  return (
    <header
      className={
        auraOnVideo
          ? [
              "fixed inset-x-0 top-0 z-50 text-[#fffaf6] transition-[background-color,border-color,backdrop-filter] duration-300",
              chrome.scrolled
                ? "border-b border-[#8b6d59]/40 bg-[#29231f]/45 backdrop-blur-md"
                : "border-b border-transparent bg-transparent",
            ].join(" ")
          : onVideo
          ? [
              "fixed inset-x-0 top-0 z-50 text-(--on-inverse) transition-[background-color,border-color,backdrop-filter] duration-300",
              chrome.scrolled
                ? "border-b border-white/15 bg-black/45 backdrop-blur-md"
                : "border-b border-transparent bg-transparent",
            ].join(" ")
          : pinHeader
            ? "fixed inset-x-0 top-0 z-50 border-b border-(--p-2d2824)/10 bg-(--canvas)/90 backdrop-blur-xl"
            : "sticky top-0 z-50 border-b border-(--p-2d2824)/10 bg-(--canvas)/90 backdrop-blur-xl"
      }
    >
      <div className="mx-auto flex h-[76px] w-full max-w-[1440px] items-center justify-between px-5 sm:px-8 lg:px-12 xl:px-16">
        {/* =====================================================
            BRAND
        ====================================================== */}
        <a
          href="#inicio"
          aria-label={`${business.brand.name} - Inicio`}
          className="group flex shrink-0 items-center gap-3"
          onClick={closeMobileMenu}
        >
          <span
            className={
              auraOnVideo
                ? "flex h-10 w-10 items-center justify-center rounded-full border border-[#8b6d59]/50 bg-[#f8f1e9]/15 font-serif text-lg text-[#fffaf6] transition-transform duration-300 group-hover:scale-105"
                : onVideo
                ? "flex h-10 w-10 items-center justify-center rounded-full border border-white/35 bg-white/10 font-serif text-lg text-white transition-transform duration-300 group-hover:scale-105"
                : "flex h-10 w-10 items-center justify-center rounded-full border border-(--p-7c6654)/30 bg-(--p-eee5db) font-serif text-lg text-(--p-665143) transition-transform duration-300 group-hover:scale-105"
            }
          >
            {business.brand.monogram}
          </span>

          <span className="leading-none">
            <span
              className={
                auraOnVideo
                  ? "block font-serif text-[19px] tracking-[0.16em] text-[#fffaf6]"
                  : onVideo
                  ? "block font-serif text-[19px] tracking-[0.12em] text-white"
                  : "block font-serif text-[19px] tracking-[0.16em] text-(--p-2b2521)"
              }
            >
              {business.brand.shortName.toUpperCase()}
            </span>

            <span
              className={
                auraOnVideo
                  ? "mt-1 block text-[9px] font-medium uppercase tracking-[0.34em] text-[#f8f1e9]/80"
                  : onVideo
                  ? "mt-1 block text-[9px] font-medium uppercase text-white/75"
                  : "mt-1 block text-[9px] font-medium uppercase tracking-[0.34em] text-(--p-8a7565)"
              }
            >
              {business.brandSuffix}
            </span>
          </span>
        </a>

        {/* =====================================================
            DESKTOP NAVIGATION
        ====================================================== */}
        <nav
          className={
            auraOnVideo
              ? "hidden items-center gap-8 text-[13px] font-medium text-[#fffaf6]/90 lg:flex"
              : onVideo
              ? "hidden items-center gap-8 text-[13px] font-medium text-white/85 lg:flex"
              : "hidden items-center gap-8 text-[13px] font-medium text-(--p-62574f) lg:flex"
          }
          aria-label="Navegación principal"
        >
          {business.navigation.map((item) => (
            <a
              key={item.href}
              href={item.href}
              className={
                auraOnVideo
                  ? "relative transition-colors duration-200 hover:text-[#fffaf6] after:absolute after:-bottom-2 after:left-0 after:h-px after:w-0 after:bg-[#8b6d59] after:transition-all after:duration-300 hover:after:w-full"
                  : onVideo
                  ? "relative transition-colors duration-200 hover:text-white after:absolute after:-bottom-2 after:left-0 after:h-px after:w-0 after:bg-(--accent-line) after:transition-all after:duration-300 hover:after:w-full"
                  : "relative transition-colors duration-200 hover:text-(--p-2a2420) after:absolute after:-bottom-2 after:left-0 after:h-px after:w-0 after:bg-(--accent) after:transition-all after:duration-300 hover:after:w-full"
              }
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
            href={CONTACT_HREF}
            className="group inline-flex h-11 items-center justify-center gap-2 rounded-full bg-(--button-bg) px-6 text-[11px] font-semibold uppercase tracking-[0.18em] !text-(--button-text) shadow-[0_8px_24px_rgba(44,38,34,0.14)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-(--p-4b3d34) hover:shadow-[0_12px_30px_rgba(44,38,34,0.20)]"
          >
            {business.header.bookingCta}

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
          className={
            auraOnVideo
              ? "flex h-11 w-11 items-center justify-center rounded-full border border-[#8b6d59]/45 bg-[#f8f1e9]/15 text-[#fffaf6] transition-all duration-200 hover:bg-[#f8f1e9]/25 sm:hidden"
              : onVideo
              ? "flex h-11 w-11 items-center justify-center rounded-full border border-white/30 bg-white/10 text-white transition-all duration-200 hover:bg-white/20 sm:hidden"
              : "flex h-11 w-11 items-center justify-center rounded-full border border-(--p-3b332e)/15 bg-white/30 text-(--p-312a26) transition-all duration-200 hover:border-(--p-3b332e)/30 hover:bg-white/60 sm:hidden"
          }
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
          "overflow-hidden border-t border-(--p-2d2824)/10 bg-(--canvas) transition-all duration-300 sm:hidden",
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
            {business.navigation.map((item) => (
              <a
                key={item.href}
                href={item.href}
                onClick={closeMobileMenu}
                className="flex items-center justify-between border-b border-(--heading)/10 py-4 text-base font-medium text-(--p-40362f) transition-colors duration-200 hover:text-(--accent)"
              >
                <span>{item.label}</span>

                <ArrowIcon className="h-4 w-4" />
              </a>
            ))}
          </nav>

          <a
            href={CONTACT_HREF}
            onClick={closeMobileMenu}
            className="group mt-6 inline-flex h-14 w-full items-center justify-center gap-3 rounded-full bg-(--button-bg) px-7 text-[11px] font-semibold uppercase tracking-[0.18em] !text-(--button-text) shadow-[0_10px_28px_rgba(44,38,34,0.14)] transition-all duration-300 hover:bg-(--p-4b3d34)"
          >
            {business.header.bookingCta}

            <span className="transition-transform duration-300 group-hover:translate-x-1">
              <ArrowIcon />
            </span>
          </a>

          <div className="mt-6 border-t border-(--heading)/10 pt-5">
            <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-9a806d)">
              {business.brand.tagline}
            </p>

            <p className="mt-2 text-sm leading-6 text-(--p-756a61)">
              {business.brand.description}
            </p>
          </div>
        </div>
      </div>
    </header>
  );
}
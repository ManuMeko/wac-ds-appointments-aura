// apps/web/components/home/ContactSection.tsx

import { ArrowIcon } from "@/components/ui/ArrowIcon";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Contact Section
========================================================= */

function PhoneIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-4 w-4"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.79 19.79 0 0 1 2.12 4.18 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.12.9.33 1.78.62 2.63a2 2 0 0 1-.45 2.11L8 9.73a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.85.29 1.73.5 2.63.62A2 2 0 0 1 22 16.92Z" />
    </svg>
  );
}

function MailIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-4 w-4"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M4 4h16v16H4z" />
      <path d="m4 7 8 6 8-6" />
    </svg>
  );
}

function LocationIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-4 w-4"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M20 10c0 5-8 12-8 12S4 15 4 10a8 8 0 1 1 16 0Z" />
      <circle cx="12" cy="10" r="2.5" />
    </svg>
  );
}

function ClockIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-4 w-4"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" />
    </svg>
  );
}

export function ContactSection() {
  const whatsappHref = `https://wa.me/${business.contact.whatsappNumber}`;
  const phoneHref = `tel:${business.contact.phoneHref}`;
  const emailHref = `mailto:${business.contact.email}`;

  return (
    <section
      id="contacto"
      className="relative overflow-hidden bg-(--section-sand)"
    >
      {/* =====================================================
          AMBIENT BACKGROUND
      ====================================================== */}
      <div className="pointer-events-none absolute -right-32 -top-32 h-[420px] w-[420px] rounded-full bg-white/20 blur-3xl" />

      <div className="pointer-events-none absolute -bottom-40 -left-32 h-[440px] w-[440px] rounded-full bg-(--p-9a7c66)/10 blur-3xl" />

      <div className="relative mx-auto w-full max-w-[1440px] px-5 py-20 sm:px-8 lg:px-12 lg:py-28 xl:px-16">
        <div className="overflow-hidden rounded-[2rem] border border-white/25 bg-(--p-f6f0e9) shadow-[0_30px_90px_rgba(65,50,40,0.12)]">
          <div className="grid lg:grid-cols-[1.12fr_0.88fr]">
            {/* =================================================
                PRIMARY CTA AREA
            ================================================== */}
            <div className="flex h-full flex-col p-7 sm:p-10 lg:p-14 xl:p-16">
              {/* =================================================
                  MAIN CONTENT
              ================================================== */}
              <div>
                <p className="text-[10px] font-semibold uppercase tracking-[0.27em] text-(--p-947862)">
                  {business.contactSection.eyebrow}
                </p>

                <h2 className="mt-5 max-w-[650px] font-serif text-5xl leading-[0.95] tracking-[-0.04em] text-(--p-2d2723) sm:text-6xl">
                  {business.contactSection.title}
                </h2>

                <p className="mt-7 max-w-[560px] text-base leading-8 text-(--p-72645a)">
                  {business.contactSection.description}
                </p>

                {/* =============================================
                    CTAs
                ============================================== */}
                <div className="mt-9 flex flex-col gap-3 sm:flex-row">
                  <a
                    href={whatsappHref}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="group inline-flex h-14 items-center justify-center gap-3 rounded-full bg-(--p-2e2824) px-8 text-[11px] font-semibold uppercase tracking-[0.18em] !text-(--on-inverse) shadow-[0_12px_30px_rgba(46,40,36,0.12)] transition-all duration-300 hover:-translate-y-0.5 hover:bg-(--p-4b3e35) hover:shadow-[0_16px_36px_rgba(46,40,36,0.18)]"
                  >
                    {business.contactSection.whatsappCta.label}

                    <span className="transition-transform duration-300 group-hover:translate-x-1">
                      <ArrowIcon />
                    </span>
                  </a>

                  <a
                    href={phoneHref}
                    className="group inline-flex h-14 items-center justify-center gap-2 rounded-full border border-(--p-3e352f)/20 bg-transparent px-8 text-[11px] font-semibold uppercase tracking-[0.18em] text-(--p-413730) transition-all duration-300 hover:-translate-y-0.5 hover:border-(--p-3e352f)/35 hover:bg-(--p-ebe1d8)"
                  >
                    <PhoneIcon />

                    {business.contactSection.phoneCta.label}
                  </a>
                </div>

                {/* =============================================
                    CONTACT SHORTCUTS
                ============================================== */}
                <div className="mt-10 grid gap-4 border-t border-(--p-3a312b)/10 pt-8 sm:grid-cols-2">
                  <a
                    href={phoneHref}
                    className="group rounded-[1.1rem] border border-(--p-3a312b)/10 bg-white/25 p-4 transition-all duration-300 hover:-translate-y-0.5 hover:border-(--p-3a312b)/15 hover:bg-white/40"
                  >
                    <div className="flex items-center gap-3 text-(--p-8c715f)">
                      <PhoneIcon />

                      <span className="text-[9px] font-semibold uppercase tracking-[0.2em]">
                        {business.contactSection.phoneLabel}
                      </span>
                    </div>

                    <p className="mt-3 text-sm font-medium text-(--p-443932)">
                      {business.contact.phoneDisplay}
                    </p>
                  </a>

                  <a
                    href={emailHref}
                    className="group rounded-[1.1rem] border border-(--p-3a312b)/10 bg-white/25 p-4 transition-all duration-300 hover:-translate-y-0.5 hover:border-(--p-3a312b)/15 hover:bg-white/40"
                  >
                    <div className="flex items-center gap-3 text-(--p-8c715f)">
                      <MailIcon />

                      <span className="text-[9px] font-semibold uppercase tracking-[0.2em]">
                        {business.contactSection.emailLabel}
                      </span>
                    </div>

                    <p className="mt-3 break-all text-sm font-medium text-(--p-443932)">
                      {business.contact.email}
                    </p>
                  </a>
                </div>
              </div>

              {/* =============================================
                  SUPPORTING COPY
                  En desktop se empuja hacia abajo para
                  equilibrar visualmente el panel derecho.
              ============================================== */}
              <div className="mt-12 border-l border-(--accent)/35 pl-5 lg:mt-auto lg:pt-8">
                <p className="max-w-[510px] font-serif text-xl leading-7 text-(--p-5d4d42)">
                  {business.contactSection.closingNote}
                </p>
              </div>
            </div>

            {/* =================================================
                CONTACT DETAILS
            ================================================== */}
            <aside className="border-t border-(--p-352e29)/10 bg-(--p-ebe1d8) p-7 sm:p-10 lg:border-l lg:border-t-0 lg:p-14 xl:p-16">
              <div className="space-y-8">
                {/* =============================================
                    ADDRESS
                ============================================== */}
                <div>
                  <div className="flex items-center gap-3 text-(--p-967a66)">
                    <LocationIcon />

                    <span className="text-[10px] font-semibold uppercase tracking-[0.2em]">
                      {business.contactSection.addressLabel}
                    </span>
                  </div>

                  <p className="mt-4 font-serif text-2xl leading-8 text-(--p-332c27)">
                    {business.contact.addressLine1}
                    <br />
                    {business.contact.addressLine2}
                  </p>

                  <p className="mt-2 text-sm text-(--p-74665c)">
                    {business.contact.city} · {business.contact.country}
                  </p>
                </div>

                <div className="h-px bg-(--p-3c332d)/10" />

                {/* =============================================
                    OPENING HOURS
                ============================================== */}
                <div>
                  <div className="flex items-center gap-3 text-(--p-967a66)">
                    <ClockIcon />

                    <span className="text-[10px] font-semibold uppercase tracking-[0.2em]">
                      {business.contactSection.hoursLabel}
                    </span>
                  </div>

                  <div className="mt-5 space-y-3 text-sm">
                    {business.openingHours.map((item) => (
                      <div
                        key={`${item.days}-${item.hours}`}
                        className="flex items-center justify-between gap-5 border-b border-(--p-3c332d)/[0.07] pb-3 last:border-b-0 last:pb-0"
                      >
                        <span className="text-(--p-5f534a)">
                          {item.days}
                        </span>

                        <span className="text-right font-medium text-(--p-473c35)">
                          {item.hours}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="h-px bg-(--p-3c332d)/10" />

                {/* =============================================
                    DIRECT CONTACT
                ============================================== */}
                <div>
                  <span className="text-[10px] font-semibold uppercase tracking-[0.2em] text-(--p-967a66)">
                    {business.contactSection.directContactLabel}
                  </span>

                  <div className="mt-4 space-y-3 text-sm text-(--p-5f534a)">
                    <a
                      href={phoneHref}
                      className="flex w-fit items-center gap-2 transition-colors duration-200 hover:text-(--accent-line)"
                    >
                      <PhoneIcon />

                      {business.contact.phoneDisplay}
                    </a>

                    <a
                      href={emailHref}
                      className="flex w-fit items-center gap-2 transition-colors duration-200 hover:text-(--accent-line)"
                    >
                      <MailIcon />

                      {business.contact.email}
                    </a>
                  </div>
                </div>

                <div className="h-px bg-(--p-3c332d)/10" />

                {/* =============================================
                    BOOKING NOTE
                ============================================== */}
                <div className="rounded-[1.25rem] border border-(--p-3c332d)/10 bg-white/30 p-5">
                  <p className="text-[9px] font-semibold uppercase tracking-[0.2em] text-(--p-967a66)">
                    {business.contactSection.guidanceTitle}
                  </p>

                  <p className="mt-3 text-sm leading-6 text-(--p-66584f)">
                    {business.contactSection.guidanceBody}
                  </p>

                  <a
                    href={whatsappHref}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="group mt-4 inline-flex items-center gap-2 text-[10px] font-semibold uppercase tracking-[0.16em] text-(--p-4c4038) transition-colors duration-200 hover:text-(--accent-line)"
                  >
                    {business.contactSection.guidanceCta}

                    <span className="transition-transform duration-300 group-hover:translate-x-1">
                      <ArrowIcon />
                    </span>
                  </a>
                </div>
              </div>
            </aside>
          </div>
        </div>
      </div>
    </section>
  );
}
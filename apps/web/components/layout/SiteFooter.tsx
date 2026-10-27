// apps/web/components/layout/SiteFooter.tsx

import { BackToTop } from "@/components/layout/BackToTop";
import { Noise } from "@/components/motion/Noise";
import { business } from "@/config/business";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Site Footer
========================================================= */

export function SiteFooter() {
  if (business.theme === "taller") return <TallerFooter />;

  return (
    <footer data-header-ink="light" className="bg-(--p-201c19) text-white">
      <Noise id="noise-footer" />
      <div className="relative mx-auto w-full max-w-[1440px] px-5 py-12 sm:px-8 lg:px-12 xl:px-16">
        <div className="flex flex-col justify-between gap-12 border-b border-white/10 pb-10 lg:flex-row lg:items-end">
          <div className="max-w-[430px]">
            <a
              href="#inicio"
              aria-label={`${business.brand.name} - Inicio`}
              className="group inline-flex items-center gap-3"
            >
              <span className="flex h-11 w-11 items-center justify-center rounded-full border border-white/15 bg-white/[0.04] font-serif text-xl text-(--p-d6c0ae) transition-all duration-300 group-hover:border-white/25 group-hover:bg-white/[0.07]">
                {business.brand.monogram}
              </span>

              <span className="leading-none">
                <span className="block font-serif text-[22px] tracking-[0.14em] text-(--p-f0e7df)">
                  {business.brand.shortName.toUpperCase()}
                </span>

                <span className="mt-1 block text-[9px] font-medium uppercase tracking-[0.34em] text-(--p-8e837c)">
                  {business.brandSuffix}
                </span>
              </span>
            </a>

            <p className="mt-5 max-w-[390px] text-sm leading-6 text-(--p-948a84)">
              {business.brand.description}
            </p>

            <p className="mt-5 text-[10px] font-semibold uppercase tracking-[0.23em] text-(--p-766d68)">
              {business.brand.locationLabel}
            </p>
          </div>

          <div className="grid gap-10 sm:grid-cols-2 lg:min-w-[430px]">
            <div>
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-736a65)">
                Navegación
              </p>

              <nav
                aria-label="Navegación del pie de página"
                className="mt-5 flex flex-col gap-3"
              >
                {business.navigation.map((item) => (
                  <a
                    key={item.href}
                    href={item.href}
                    className="w-fit text-sm text-(--p-aca19a) transition-colors duration-200 hover:text-white"
                  >
                    {item.label}
                  </a>
                ))}
              </nav>
            </div>

            <div>
              <p className="text-[10px] font-semibold uppercase tracking-[0.22em] text-(--p-736a65)">
                Síguenos
              </p>

              <div className="mt-5 flex flex-col gap-3">
                {business.socialLinks.map((item) => (
                  <a
                    key={item.name}
                    href={item.href}
                    className="w-fit text-sm text-(--p-aca19a) transition-colors duration-200 hover:text-white"
                  >
                    {item.name}
                  </a>
                ))}
              </div>
            </div>
          </div>
        </div>

        <div className="flex flex-col justify-between gap-5 pt-7 text-[11px] text-(--p-776f6a) sm:flex-row sm:items-center">
          <p>{business.footer.copyright}</p>

          <div className="flex flex-wrap gap-x-5 gap-y-2">
            {business.legalLinks.map((item) => (
              <a
                key={item.label}
                href={item.href}
                className="transition-colors duration-200 hover:text-(--p-bcb1aa)"
              >
                {item.label}
              </a>
            ))}
          </div>
        </div>

        <div className="mt-7 border-t border-white/10 pt-7">
          <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
            <p className="text-[10px] uppercase tracking-[0.2em] text-(--p-625b57)">
              {business.footer.credit}
            </p>

            <p className="text-[10px] uppercase tracking-[0.18em] text-(--p-4f4945)">
              Model 01 · Appointments
            </p>
          </div>
        </div>
      </div>
    </footer>
  );
}

function TallerFooter() {
  const phoneHref = `tel:${business.contact.phoneHref}`;
  const emailHref = `mailto:${business.contact.email}`;

  return (
    <>
      <section
        className="home-footer-cta"
        data-header-ink="light"
        style={{ backgroundImage: `url(${business.assets.openGraph})` }}
      >
        <div className="home-footer-cta-shade" aria-hidden="true" />
        <div className="home-footer-cta-copy">
          <h2>
            {business.hero.title} {business.hero.highlightedTitle}
          </h2>
          <a className="home-cta-fill" href={business.hero.primaryCta.href}>
            {business.hero.primaryCta.label}
          </a>
        </div>
      </section>

      <footer
        data-header-ink="light"
        className="home-footer relative bg-(--inverse) text-white"
      >
        <Noise id="noise-footer" />
        <div className="relative mx-auto w-full max-w-[1440px] px-5 pt-14 pb-8 sm:px-8 lg:px-12 xl:px-16">
          <div className="home-footer-grid">
            <div>
              <a
                href="#inicio"
                aria-label={`${business.brand.name} - Inicio`}
                className="home-footer-brand"
              >
                <span className="home-footer-mark">{business.brand.monogram}</span>
                <span>
                  <span className="home-footer-name">
                    {business.brand.shortName.toUpperCase()}
                  </span>
                  <span className="home-footer-suffix">{business.brandSuffix}</span>
                </span>
              </a>
              <p className="home-footer-desc">{business.brand.description}</p>
              <p className="home-footer-address">
                {business.contact.addressLine1}
                <br />
                {business.contact.addressLine2}
              </p>
            </div>

            <div>
              <p className="home-footer-label">Navegación</p>
              <nav aria-label="Navegación del pie de página" className="home-footer-links">
                {business.navigation.map((item) => (
                  <a key={item.href} href={item.href}>
                    {item.label}
                  </a>
                ))}
              </nav>
            </div>

            <div>
              <p className="home-footer-label">Contacto</p>
              <ul className="home-footer-contact">
                <li>
                  <PhoneIcon />
                  <a href={phoneHref}>{business.contact.phoneDisplay}</a>
                </li>
                <li>
                  <MailIcon />
                  <a href={emailHref}>{business.contact.email}</a>
                </li>
                <li>
                  <ClockIcon />
                  <span>
                    {business.openingHours.map((row) => (
                      <span key={row.days} className="home-footer-hour">
                        {row.days}
                        <span>{row.hours}</span>
                      </span>
                    ))}
                  </span>
                </li>
              </ul>
            </div>

            <div>
              <p className="home-footer-label">Síguenos</p>
              <div className="home-footer-social">
                {business.socialLinks.map((item) => (
                  <a key={item.name} href={item.href} aria-label={item.name}>
                    {item.name === "TikTok" ? <TikTokIcon /> : <InstagramIcon />}
                  </a>
                ))}
              </div>
            </div>
          </div>
        </div>

        <p className="home-wordmark" aria-hidden="true">
          {business.brand.shortName.toUpperCase()}
        </p>

        <div className="home-footer-bottom">
          <p>{business.footer.copyright}</p>
          <nav aria-label="Enlaces legales" className="home-footer-legal">
            {business.legalLinks.map((item) => (
              <a key={item.label} href={item.href}>
                {item.label}
              </a>
            ))}
          </nav>
          <p className="home-footer-credit">
            Digital experience by{" "}
            <a href="https://wacds.com" rel="noreferrer">
              WAC D.S.
            </a>
          </p>
        </div>
      </footer>
      <BackToTop />
    </>
  );
}

function PhoneIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="home-footer-icon"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <path d="M8 4h3l1.5 3.5-2 1.2a12 12 0 0 0 5 5l1.2-2L20 13v3a2 2 0 0 1-2.2 2A16 16 0 0 1 4 6.2 2 2 0 0 1 6 4z" />
    </svg>
  );
}

function MailIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="home-footer-icon"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <rect x="3.5" y="5.5" width="17" height="13" rx="2" />
      <path d="m4 7 8 6 8-6" />
    </svg>
  );
}

function ClockIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="home-footer-icon"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <circle cx="12" cy="12" r="8" />
      <path d="M12 8v4.5l3 2" />
    </svg>
  );
}

function InstagramIcon() {
  return (
    <svg
      aria-hidden="true"
      viewBox="0 0 24 24"
      className="h-4 w-4"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
    >
      <rect x="4" y="4" width="16" height="16" rx="4" />
      <circle cx="12" cy="12" r="3.5" />
      <circle cx="17.2" cy="6.8" r="0.8" fill="currentColor" stroke="none" />
    </svg>
  );
}

function TikTokIcon() {
  return (
    <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4" fill="currentColor">
      <path d="M14 6c.6 2.2 2.1 3.6 4.2 4v2.4c-1.5 0-2.9-.5-4.2-1.3v5.6a5.2 5.2 0 1 1-5.2-5.2c.3 0 .6 0 .9.1v2.6a2.6 2.6 0 1 0 1.8 2.5V6H14z" />
    </svg>
  );
}

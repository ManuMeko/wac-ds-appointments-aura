// apps/web/app/page.tsx

import type { Metadata } from "next";

import { ContactSection } from "@/components/home/ContactSection";
import { ExperienceSection } from "@/components/home/ExperienceSection";
import { HeroSection } from "@/components/home/HeroSection";
import { LiveSlots } from "@/components/home/LiveSlots";
import { OwnerControlSection } from "@/components/home/OwnerControlSection";
import { PhilosophySection } from "@/components/home/PhilosophySection";
import { ReviewsSection } from "@/components/home/ReviewsSection";
import { ScrollSequence } from "@/components/home/ScrollSequence";
import { ServicesSection } from "@/components/home/ServicesSection";
import { StatBand } from "@/components/home/StatBand";
import { TeamSection } from "@/components/home/TeamSection";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { JsonLd } from "@/components/seo/JsonLd";
import { business } from "@/config/business";

export const metadata: Metadata = {
  title: { absolute: business.seo.defaultTitle },
  description: business.seo.description,
  keywords: [...business.seo.keywords],
  alternates: { canonical: "/" },
  openGraph: {
    type: "website",
    locale: business.seo.locale,
    url: "/",
    siteName: business.seo.siteName,
    title: business.seo.defaultTitle,
    description: business.seo.description,
    images: [
      {
        url: business.seo.ogImage,
        alt: `${business.brand.name} · ${business.brand.tagline}`,
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: business.seo.defaultTitle,
    description: business.seo.description,
    images: [business.seo.ogImage],
  },
  robots: {
    index: business.seo.robots.index,
    follow: business.seo.robots.follow,
  },
};

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Home Page
========================================================= */

export default function Home() {
  const sequence = business.scrollSequence;
  const firstFrame = sequence
    ? `${sequence.basePath.replace(/\/$/, "")}/frame_0001.${sequence.fileExtension}`
    : null;

  return (
    <div className="min-h-screen overflow-x-hidden bg-(--canvas) text-(--inverse)">
      <JsonLd />
      {firstFrame ? (
        <link rel="preload" as="image" href={firstFrame} fetchPriority="high" />
      ) : null}
      <SiteHeader />

      <main>
        {sequence ? <ScrollSequence sequence={sequence} /> : <HeroSection />}
        <PhilosophySection />
        <ServicesSection />
        <LiveSlots />
        <ExperienceSection />
        <StatBand />
        <TeamSection />
        <ReviewsSection />
        <OwnerControlSection />
        <ContactSection />
      </main>

      <SiteFooter />
    </div>
  );
}
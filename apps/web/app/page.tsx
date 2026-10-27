import type { Metadata } from "next";

import { ContactSection } from "@/components/home/ContactSection";
import { ExperienceSection } from "@/components/home/ExperienceSection";
import { HomeOpening } from "@/components/home/HomeOpening";
import { PhilosophySection } from "@/components/home/PhilosophySection";
import { ReviewsSection } from "@/components/home/ReviewsSection";
import { ServicesSection } from "@/components/home/ServicesSection";
import { StatBand } from "@/components/home/StatBand";
import { TeamSection } from "@/components/home/TeamSection";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { business } from "@/config/business";
import { scrollSequenceReady } from "@/lib/scrollFrames";

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

export default function Home() {
  const sequence = business.scrollSequence;
  const ready = scrollSequenceReady(sequence);
  const firstFrame =
    ready && sequence
      ? `${sequence.basePath.replace(/\/$/, "")}/frame_0001.${sequence.fileExtension}`
      : null;

  return (
    <div className="min-h-screen overflow-x-hidden bg-(--canvas) text-(--inverse)">
      {firstFrame ? (
        <link rel="preload" as="image" href={firstFrame} fetchPriority="high" />
      ) : null}
      <SiteHeader overSequence={ready} />

      <main>
        <HomeOpening
          ready={ready}
          sequence={sequence}
          skin={business.theme}
        />
        <PhilosophySection />
        <ServicesSection />
        <ExperienceSection />
        <StatBand />
        <TeamSection />
        <ReviewsSection />
        <ContactSection />
      </main>

      <SiteFooter />
    </div>
  );
}

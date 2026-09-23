// apps/web/app/page.tsx

import { ContactSection } from "@/components/home/ContactSection";
import { ExperienceSection } from "@/components/home/ExperienceSection";
import { HeroSection } from "@/components/home/HeroSection";
import { PhilosophySection } from "@/components/home/PhilosophySection";
import { ReviewsSection } from "@/components/home/ReviewsSection";
import { ServicesSection } from "@/components/home/ServicesSection";
import { TeamSection } from "@/components/home/TeamSection";
import { SiteFooter } from "@/components/layout/SiteFooter";
import { SiteHeader } from "@/components/layout/SiteHeader";

/* =========================================================
   WAC DS · MODEL 01 · APPOINTMENTS
   Aura Studio · Home Page
========================================================= */

export default function Home() {
  return (
    <div className="min-h-screen overflow-x-hidden bg-[#f8f5f0] text-[#25211e]">
      <SiteHeader />

      <main>
        <HeroSection />
        <PhilosophySection />
        <ServicesSection />
        <ExperienceSection />
        <TeamSection />
        <ReviewsSection />
        <ContactSection />
      </main>

      <SiteFooter />
    </div>
  );
}
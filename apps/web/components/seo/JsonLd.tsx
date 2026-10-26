import { business } from "@/config/business";
import { buildLocalBusiness } from "@/lib/jsonld";

export function JsonLd() {
  const data = buildLocalBusiness(business);
  if (!data) return null;

  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{
        __html: JSON.stringify(data).replace(/</g, "\\u003c"),
      }}
    />
  );
}

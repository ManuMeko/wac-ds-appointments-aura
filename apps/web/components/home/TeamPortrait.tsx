"use client";

import Image from "next/image";
import { useState } from "react";

export function TeamPortrait({
  src,
  alt,
  initials,
}: {
  src: string;
  alt: string;
  initials: string;
}) {
  const [failed, setFailed] = useState(!src);

  if (failed) {
    return (
      <div className="flex h-full w-full items-center justify-center bg-[linear-gradient(160deg,#44403c,#1c1917)]">
        <span className="font-serif text-6xl text-white">{initials}</span>
      </div>
    );
  }

  return (
    <Image
      src={src}
      alt={alt}
      fill
      quality={85}
      sizes="(min-width: 768px) 33vw, 82vw"
      className="object-cover object-center transition-transform duration-[600ms] ease-out group-hover:scale-105"
      onError={() => setFailed(true)}
    />
  );
}

import { ImageResponse } from "next/og";

export const size = { width: 180, height: 180 };
export const contentType = "image/png";

export default function AppleIcon() {
  const taller = process.env.NEXT_PUBLIC_DEMO?.trim() === "taller";

  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: taller ? "#1c1917" : "#f8f5f0",
          color: taller ? "#f97316" : "#8b6d59",
          fontSize: 96,
          fontWeight: 700,
        }}
      >
        {taller ? "M" : "A"}
      </div>
    ),
    { ...size },
  );
}

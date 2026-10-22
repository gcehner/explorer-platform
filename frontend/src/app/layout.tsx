import type { Metadata } from "next";
import "maplibre-gl/dist/maplibre-gl.css";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "Explorer Platform",
    template: "%s · Explorer",
  },
  description: "MTB ture in zgodbe s poti.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="sl">
      <body>{children}</body>
    </html>
  );
}

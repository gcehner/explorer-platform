import type { Metadata } from "next";
import "maplibre-gl/dist/maplibre-gl.css";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "MTB 'n' SKI",
    template: "%s · MTB 'n' SKI",
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

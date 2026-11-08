import type { Metadata } from "next";
import "maplibre-gl/dist/maplibre-gl.css";
import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "MTB",
    template: "%s · MTB",
  },
  description: "Kurirana zbirka MTB tur, GPX sledi in tehničnih informacij.",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="sl">
      <body>{children}</body>
    </html>
  );
}

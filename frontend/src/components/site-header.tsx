"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useRef, useState } from "react";

import { ExplorerBrand } from "@/components/explorer-brand";

const navigation = [
  { href: "/", label: "Domov" },
  { href: "/mtb", label: "MTB" },
  { href: "/ski-touring", label: "Turno smučanje" },
  { href: "/about", label: "O meni" },
] as const;

function isActive(pathname: string, href: (typeof navigation)[number]["href"]) {
  if (href === "/") {
    return pathname === href;
  }

  return pathname === href || pathname.startsWith(`${href}/`);
}

export function SiteHeader() {
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);
  const menuButtonRef = useRef<HTMLButtonElement>(null);

  function handleKeyDown(event: React.KeyboardEvent<HTMLElement>) {
    if (event.key === "Escape" && menuOpen) {
      setMenuOpen(false);
      menuButtonRef.current?.focus();
    }
  }

  return (
    <header
      className="sticky top-0 z-40 border-b border-[#dfe4dc] bg-[#f8f9f6]/95 text-[var(--color-foreground)] backdrop-blur-sm"
      onKeyDown={handleKeyDown}
    >
      <div className="flex h-[var(--site-header-height)] w-full items-center justify-between gap-8 px-5">
        <ExplorerBrand />

        <nav
          className="hidden h-full items-center gap-8 md:flex"
          aria-label="Glavna navigacija"
        >
          {navigation.map((item) => {
            const active = isActive(pathname, item.href);

            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={`rounded-md px-3 py-2 text-sm font-medium transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-mtb-dark)] ${
                  active
                    ? "bg-[#edf1e9] text-[var(--color-mtb-dark)]"
                    : "text-[#3d473f] hover:bg-[#f0f2ee] hover:text-[var(--color-mtb-dark)]"
                }`}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <button
          ref={menuButtonRef}
          type="button"
          className="grid size-10 place-items-center rounded-md border border-[#cbd2c8] text-[var(--color-foreground)] transition hover:border-[var(--color-mtb-dark)] hover:text-[var(--color-mtb-dark)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-mtb-dark)] md:hidden"
          aria-controls="mobile-primary-navigation"
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((open) => !open)}
        >
          <span className="sr-only">
            {menuOpen ? "Zapri glavni meni" : "Odpri glavni meni"}
          </span>
          <svg
            viewBox="0 0 24 24"
            className="size-6"
            aria-hidden="true"
            fill="none"
            stroke="currentColor"
            strokeLinecap="round"
            strokeWidth="2"
          >
            {menuOpen ? (
              <>
                <path d="M5 5l14 14" />
                <path d="M19 5L5 19" />
              </>
            ) : (
              <>
                <path d="M4 7h16" />
                <path d="M4 12h16" />
                <path d="M4 17h16" />
              </>
            )}
          </svg>
        </button>
      </div>

      {menuOpen ? (
        <nav
          id="mobile-primary-navigation"
          className="border-t border-[#dfe4dc] bg-[#f8f9f6] pb-3 md:hidden"
          aria-label="Mobilna glavna navigacija"
        >
          <div className="grid w-full px-5 py-2">
            {navigation.map((item) => {
              const active = isActive(pathname, item.href);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  onClick={() => setMenuOpen(false)}
                  className={`rounded-md px-4 py-3 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-mtb-dark)] ${
                    active
                      ? "bg-[#edf1e9] text-[var(--color-mtb-dark)]"
                      : "text-[#3d473f] hover:bg-[#f0f2ee] hover:text-[var(--color-mtb-dark)]"
                  }`}
                >
                  {item.label}
                </Link>
              );
            })}
          </div>
        </nav>
      ) : null}
    </header>
  );
}

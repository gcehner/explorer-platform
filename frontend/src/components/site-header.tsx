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
      className="sticky top-0 z-40 border-b border-white/10 bg-[var(--color-header)] text-white"
      onKeyDown={handleKeyDown}
    >
      <div className="site-container flex h-[var(--site-header-height)] items-center gap-8">
        <ExplorerBrand />

        <nav
          className="ml-auto hidden h-full items-center gap-8 md:flex"
          aria-label="Glavna navigacija"
        >
          {navigation.map((item) => {
            const active = isActive(pathname, item.href);

            return (
              <Link
                key={item.href}
                href={item.href}
                aria-current={active ? "page" : undefined}
                className={`flex h-full items-center border-b-2 px-1 text-sm font-extrabold transition-colors focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#8fbd53] ${
                  active
                    ? "border-[#8fbd53] text-[#8fbd53]"
                    : "border-transparent text-[#e7ebe7] hover:text-[#8fbd53]"
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
          className="ml-auto grid size-11 place-items-center rounded-lg border border-white/20 text-white transition hover:border-[#8fbd53] hover:text-[#8fbd53] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#8fbd53] md:hidden"
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
          className="border-t border-white/10 bg-[var(--color-header)] px-[4vw] pb-4 md:hidden"
          aria-label="Mobilna glavna navigacija"
        >
          <div className="site-container grid py-2">
            {navigation.map((item) => {
              const active = isActive(pathname, item.href);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  aria-current={active ? "page" : undefined}
                  onClick={() => setMenuOpen(false)}
                  className={`rounded-lg border-l-2 px-4 py-3 text-sm font-extrabold focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#8fbd53] ${
                    active
                      ? "border-[#8fbd53] bg-white/5 text-[#8fbd53]"
                      : "border-transparent text-[#e7ebe7] hover:bg-white/5 hover:text-[#8fbd53]"
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

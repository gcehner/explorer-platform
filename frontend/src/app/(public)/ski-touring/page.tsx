import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Turno smučanje",
  description: "MTB 'n' SKI turno smučarske ture – vsebina je v pripravi.",
};

export default function SkiTouringPage() {
  return (
    <main className="site-container flex min-h-[60vh] items-center py-16 sm:py-24">
      <section className="surface-card w-full overflow-hidden">
        <div className="h-2 bg-[var(--color-ski)]" />
        <div className="max-w-3xl p-8 sm:p-12">
          <p className="text-sm font-black tracking-[0.12em] text-[var(--color-ski)] uppercase">
            V pripravi
          </p>
          <h1 className="mt-4 text-4xl leading-tight font-black tracking-[-0.035em] sm:text-6xl">
            Turno smučanje
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-[var(--color-muted)]">
            Zbirka turno smučarskih tur še nastaja. Stran bo uporabljala enako
            natančne sledi, višinske profile in tehnične podatke kot MTB vodnik.
          </p>
        </div>
      </section>
    </main>
  );
}

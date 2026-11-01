import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "MTB ture",
  description: "MTB 'n' SKI zbirka objavljenih MTB tur.",
};

export default function MtbPage() {
  return (
    <main className="site-container flex min-h-[60vh] items-center py-16 sm:py-24">
      <section className="surface-card w-full overflow-hidden">
        <div className="h-2 bg-[var(--color-mtb)]" />
        <div className="max-w-3xl p-8 sm:p-12">
          <p className="text-sm font-black tracking-[0.12em] text-[var(--color-mtb)] uppercase">
            MTB
          </p>
          <h1 className="mt-4 text-4xl leading-tight font-black tracking-[-0.035em] sm:text-6xl">
            MTB ture
          </h1>
          <p className="mt-5 max-w-2xl text-lg leading-8 text-[var(--color-muted)]">
            Javni seznam tur je v pripravi. Objavljene ture bodo tukaj prikazane
            neposredno iz javnega API-ja.
          </p>
        </div>
      </section>
    </main>
  );
}

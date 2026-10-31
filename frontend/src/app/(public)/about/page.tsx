import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "O meni",
  description: "O avtorju projekta Explorer.",
};

function Placeholder({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <p className="mt-4 rounded-lg border border-dashed border-[var(--color-border)] bg-[var(--color-surface-soft)] p-4 leading-7 text-[var(--color-muted)]">
      <strong className="block text-sm tracking-[0.08em] text-[var(--color-mtb-dark)] uppercase">
        Vsebinski zaznamek
      </strong>
      {children}
    </p>
  );
}

export default function AboutPage() {
  return (
    <main className="site-container py-16 sm:py-24">
      <div className="max-w-3xl">
        <p className="text-sm font-black tracking-[0.12em] text-[var(--color-mtb)] uppercase">
          Explorer
        </p>
        <h1 className="mt-4 text-4xl leading-tight font-black tracking-[-0.035em] sm:text-6xl">
          O meni
        </h1>
        <p className="mt-5 text-lg leading-8 text-[var(--color-muted)]">
          Ta stran bo predstavila avtorja osebnega vodnika Explorer in način,
          kako nastajajo ture.
        </p>
      </div>

      <div className="mt-10 grid gap-6 md:grid-cols-2">
        <section className="surface-card p-7 sm:p-8">
          <h2 className="text-2xl font-bold">Predstavitev</h2>
          <Placeholder>
            Tukaj dodajte potrjeno osebno biografijo, izkušnje in namen projekta.
          </Placeholder>
        </section>

        <section className="surface-card p-7 sm:p-8">
          <h2 className="text-2xl font-bold">Kontakt</h2>
          <Placeholder>
            Tukaj dodajte želeni javni kontaktni kanal. Kontaktni podatki niso
            bili izmišljeni ali objavljeni.
          </Placeholder>
        </section>
      </div>
    </main>
  );
}

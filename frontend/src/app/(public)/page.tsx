export default function Home() {
  return (
    <main className="flex min-h-[calc(100svh-var(--site-header-height))] items-center justify-center px-6 py-16">
      <section className="w-full max-w-2xl rounded-2xl border border-stone-200 bg-white p-10 shadow-[0_18px_50px_rgba(20,30,23,0.08)]">
        <p className="text-sm font-extrabold tracking-[0.16em] text-[#4d7e20] uppercase">
          Explorer Platform
        </p>
        <h1 className="mt-4 text-4xl font-bold tracking-tight text-[#17201a]">
          MTB ture, zapisane s terena.
        </h1>
        <p className="mt-5 max-w-xl text-lg leading-8 text-stone-600">
          Javni del osebnega vodnika po MTB in turno smučarskih turah je v
          pripravi.
        </p>
      </section>
    </main>
  );
}

import Link from "next/link";

export default function Home() {
  const previewEnabled = process.env.NODE_ENV !== "production";

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f4f5f2] px-6">
      <section className="w-full max-w-2xl rounded-2xl border border-stone-200 bg-white p-10 shadow-[0_18px_50px_rgba(20,30,23,0.08)]">
        <p className="text-sm font-extrabold tracking-[0.16em] text-[#4d7e20] uppercase">
          Explorer Platform
        </p>
        <h1 className="mt-4 text-4xl font-bold tracking-tight text-[#17201a]">
          MTB ture, zapisane s terena.
        </h1>
        <p className="mt-5 max-w-xl text-lg leading-8 text-stone-600">
          Stran s podrobnostmi izbrane ture je trenutno na voljo kot lokalni
          management predogled. Tura 34 je razvojni primer.
        </p>
        {previewEnabled ? (
          <Link
            href="/management/tours/34"
            className="mt-8 inline-flex rounded-lg bg-[#4d7e20] px-5 py-3 font-bold text-white transition hover:bg-[#315714]"
          >
            Odpri razvojni primer: tura 34
          </Link>
        ) : null}
      </section>
    </main>
  );
}

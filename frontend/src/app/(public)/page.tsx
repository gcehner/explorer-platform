import Link from "next/link";

import { HomeHeroImage } from "@/components/home-hero-image";
import { HomeMap } from "@/components/home-map";

const DEFAULT_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty";

const quickStatistics = [
  { icon: "△", label: "Objavljenih tur" },
  { icon: "⌁", label: "Skupna razdalja" },
  { icon: "↗", label: "Skupaj vzpona" },
  { icon: "◫", label: "Aktivno" },
  { icon: "◎", label: "Držav" },
] as const;

export default function Home() {
  const mapStyleUrl =
    process.env.NEXT_PUBLIC_MAP_STYLE_URL?.trim() || DEFAULT_MAP_STYLE_URL;

  return (
    <main>
      <section className="home-hero-layout" aria-label="Uvod">
        <HomeHeroImage />

        <HomeMap styleUrl={mapStyleUrl} />
      </section>

      <section className="home-content">
        <div className="home-quick-strip" aria-label="Hiter pregled zbirke">
          {quickStatistics.map((statistic) => (
            <div className="home-quick-item" key={statistic.label}>
              <span className="home-quick-icon" aria-hidden="true">
                {statistic.icon}
              </span>
              <span>
                <strong aria-hidden="true">—</strong>
                <span className="sr-only">
                  Podatek trenutno ni na voljo.
                </span>
                <small>{statistic.label}</small>
              </span>
            </div>
          ))}
        </div>

        <div className="home-main-grid">
          <section className="home-panel">
            <div className="home-panel-head">
              <h2>NOVE TURE</h2>
              <Link href="/mtb">VSE TURE →</Link>
            </div>

            <div className="home-tour-list">
              <div className="home-tour-empty">
                <span className="home-tour-empty-icon" aria-hidden="true">
                  △
                </span>
                <div>
                  <h3>Objavljene ture bodo kmalu na voljo</h3>
                  <p>
                    Javni seznam tur še ni vzpostavljen. Ture bodo prikazane,
                    ko bodo preverjene in objavljene.
                  </p>
                </div>
              </div>
            </div>
          </section>

          <aside className="home-panel home-side">
            <section className="home-side-section">
              <h2>KATEGORIJE</h2>
              <Link className="home-category-row" href="/mtb">
                <span className="home-category-icon home-category-icon-mtb" aria-hidden="true">
                  ◉
                </span>
                <span>MTB TURE</span>
                <span className="home-row-arrow" aria-hidden="true">→</span>
              </Link>
            </section>

            <section className="home-side-section">
              <h2>PRILJUBLJENA OBMOČJA</h2>
              <p className="home-unavailable">
                Podatki o priljubljenosti bodo na voljo skupaj z javnim
                seznamom tur.
              </p>
            </section>

            <section className="home-side-section">
              <h2>RAZISKUJ</h2>
              <Link className="home-popular-row" href="/mtb">
                <span aria-hidden="true">↗</span>
                <span>Vse MTB ture</span>
                <span className="home-row-arrow" aria-hidden="true">→</span>
              </Link>
              <Link className="home-popular-row" href="/about">
                <span aria-hidden="true">◎</span>
                <span>O projektu MTB</span>
                <span className="home-row-arrow" aria-hidden="true">→</span>
              </Link>
            </section>
          </aside>
        </div>
      </section>
    </main>
  );
}

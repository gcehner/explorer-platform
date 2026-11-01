import Image from "next/image";

interface HomeHeroImageProps {
  src?: string;
  alt?: string;
}

export function HomeHeroImage({ src, alt = "" }: HomeHeroImageProps) {
  if (!src) {
    return <div className="home-hero-media" aria-hidden="true" />;
  }

  return (
    <div className="home-hero-media">
      <Image
        src={src}
        alt={alt}
        fill
        priority
        sizes="(min-width: 1051px) 47vw, 100vw"
        className="object-cover"
      />
    </div>
  );
}

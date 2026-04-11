import type { Metadata } from "next";
import { getTranslations, setRequestLocale } from "next-intl/server";
import { Link } from "@/i18n/navigation";

type Props = { params: Promise<{ locale: string }> };

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { locale } = await params;
  const t = await getTranslations({ locale, namespace: "home" });
  return { title: t("pageTitle") };
}

export default async function ManifestoPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const t = await getTranslations("home");

  return (
    <div className="flex h-dvh flex-col">
      <section className="flex flex-1 flex-col items-center justify-center bg-white px-4">
        <h1 className="text-center text-[clamp(1.5rem,6vw,4rem)] font-black uppercase leading-none tracking-tight text-black">
          {t("headline")}
        </h1>
        <p className="mt-2 text-center text-[clamp(3rem,15vw,10rem)] font-black uppercase leading-none text-red-600">
          {t("keyword")}
        </p>
      </section>

      <section className="flex flex-col items-center justify-center bg-red-600 px-4 py-6 md:py-8">
        <p className="text-center text-[clamp(1.25rem,4vw,2.25rem)] font-black uppercase text-white">
          {t("callout")}
        </p>
        <p className="mt-2 text-center text-[clamp(0.6rem,1.2vw,0.875rem)] uppercase tracking-[0.3em] text-white/80">
          {t("references")}
        </p>
      </section>

      <section className="flex items-center justify-center bg-white px-4 py-4 md:py-6">
        <Link
          href="/dashboard"
          className="text-[clamp(0.75rem,1.5vw,1rem)] font-bold uppercase tracking-[0.2em] text-black transition-colors hover:text-red-600"
        >
          {t("evidence")}
        </Link>
      </section>
    </div>
  );
}

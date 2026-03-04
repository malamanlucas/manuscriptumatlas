import { useTranslations } from "next-intl";
import type { SourceClaimDTO } from "@/types";

export function SourceProvenancePanel({ claims }: { claims: SourceClaimDTO[] }) {
  const t = useTranslations("councils");

  if (!claims.length) {
    return <p className="text-sm text-muted-foreground">{t("noSources")}</p>;
  }

  return (
    <div className="overflow-x-auto rounded-lg border border-border">
      <table className="min-w-full text-sm">
        <thead className="bg-muted/50 text-xs uppercase">
          <tr>
            <th className="px-3 py-2 text-left">{t("source")}</th>
            <th className="px-3 py-2 text-left">{t("sourceLevel")}</th>
            <th className="px-3 py-2 text-left">{t("claimedYear")}</th>
            <th className="px-3 py-2 text-left">{t("claimedLocation")}</th>
            <th className="px-3 py-2 text-left">{t("participants")}</th>
          </tr>
        </thead>
        <tbody>
          {claims.map((claim, idx) => (
            <tr key={`${claim.sourceDisplayName}-${idx}`} className="border-t border-border">
              <td className="px-3 py-2">{claim.sourceDisplayName}</td>
              <td className="px-3 py-2">{claim.sourceLevel}</td>
              <td className="px-3 py-2">{claim.claimedYear ?? "—"}</td>
              <td className="px-3 py-2">{claim.claimedLocation ?? "—"}</td>
              <td className="px-3 py-2">{claim.claimedParticipants ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

"use client";

import "leaflet/dist/leaflet.css";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import L from "leaflet";
import type { CouncilMapPointDTO } from "@/types";
import { Link } from "@/i18n/navigation";
import { useTranslations } from "next-intl";

const markerIcon = L.icon({
  iconUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  shadowUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
  iconSize: [25, 41],
  iconAnchor: [12, 41],
});

export function CouncilMapView({
  points,
  height = 420,
}: {
  points: CouncilMapPointDTO[];
  height?: number;
}) {
  const t = useTranslations("councils");
  const center: [number, number] = points.length
    ? [points[0].latitude, points[0].longitude]
    : [41.0, 29.0];

  return (
    <div className="overflow-hidden rounded-xl border border-border" style={{ height }}>
      <MapContainer center={center} zoom={3} style={{ height: "100%", width: "100%" }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        {points.map((point) => (
          <Marker
            key={point.id}
            position={[point.latitude, point.longitude]}
            icon={markerIcon}
          >
            <Popup>
              <div className="space-y-1 text-sm">
                <p className="font-semibold">{point.displayName}</p>
                <p>{point.year}</p>
                <Link className="text-blue-500 underline" href={`/councils/${point.slug}`}>
                  {t("viewDetails")}
                </Link>
              </div>
            </Popup>
          </Marker>
        ))}
      </MapContainer>
    </div>
  );
}

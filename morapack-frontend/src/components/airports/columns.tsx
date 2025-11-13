"use client";

import { Airport } from "@/types/airport";
import { Badge } from "@/components/ui/badge";
import { MapPin, Globe } from "lucide-react";

export interface Column<T> {
	id: string;
	header: string;
	accessor?: keyof T;
	cell?: (row: T) => React.ReactNode;
	className?: string;
	headerClassName?: string;
}

export const airportColumns: Column<Airport>[] = [
	{
		id: "code",
		header: "Código",
		accessor: "code",
		headerClassName: "pl-6",
		className: "pl-6",
		cell: (row) => (
			<div className="font-mono text-sm font-medium">{row.code || "-"}</div>
		),
	},
	{
		id: "city",
		header: "Ciudad",
		accessor: "city",
		cell: (row) => (
			<div className="flex items-center gap-1.5">
				<MapPin className="h-3.5 w-3.5 text-muted-foreground" />
				<span>{row.city || "-"}</span>
			</div>
		),
	},
	{
		id: "country",
		header: "País",
		accessor: "country",
		cell: (row) => (
			<div className="flex items-center gap-1.5">
				<Globe className="h-3.5 w-3.5 text-muted-foreground" />
				<span>{row.country || "-"}</span>
			</div>
		),
	},
	{
		id: "continent",
		header: "Continente",
		accessor: "continent",
		cell: (row) => <div>{row.continent || "-"}</div>,
	},
	{
		id: "cityAcronym",
		header: "Acrónimo",
		accessor: "cityAcronym", // Cambié de city_acronym a cityAcronym
		cell: (row) => {
			return (
				<div className="font-mono text-sm">
					{row.cityAcronym || "-"}
				</div>
			);
		},
	},
	{
		id: "gmt",
		header: "GMT",
		cell: (row) => {
			const gmt = row.gmt;
			if (gmt === undefined || gmt === null) return "-";
			return (
				<div className="tabular-nums">
					{gmt > 0 ? `+${gmt}` : gmt}
				</div>
			);
		},
		className: "text-center",
		headerClassName: "text-center",
	},
	{
		id: "capacity",
		header: "Capacidad",
		cell: (row) => {
			const capacity = row.capacity;
			if (capacity === undefined || capacity === null) return "-";
			return (
				<div className="tabular-nums">
					{capacity.toLocaleString()}
				</div>
			);
		},
		className: "text-right",
		headerClassName: "text-right",
	},
	{
		id: "isHub",
		header: "Sede",
		cell: (row) => {
			return row.isHub ? (
				<Badge variant="default">Sí</Badge>
			) : (
				<Badge variant="secondary">No</Badge>
			);
		},
		className: "text-center",
		headerClassName: "text-center",
	},
];
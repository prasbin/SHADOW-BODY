package com.shadowbody.app.domain.haircut

/**
 * Feature D: haircut catalog + preference-based advisor.
 *
 * HONEST CAPABILITY STATE: SHADOW BODY bundles no haircut imagery and no
 * licensed remote image source is currently available, so every
 * recommendation carries `hasVisualExample = false` and an explicit honest
 * state in the UI ("no example image available — use the barber text").
 * No face-shape / hairline / photo analysis exists anywhere in the app; the
 * advisor only reorders fixed catalog entries by the user's stated
 * preferences.
 */
object HaircutCatalog {

    data class Haircut(
        val id: String,
        val name: String,
        /** SHORT / MEDIUM / LONG. */
        val length: String,
        /** LOW / MEDIUM / HIGH. */
        val maintenance: String,
        /** Typical styling time from the barber perspective to hand to the user. */
        val stylingMinutes: Int,
        /** Fits: set of look keys — PROFESSIONAL / CASUAL / VERSATILE. */
        val looks: Set<String>,
        val description: String,
        val maintenanceGuidance: String,
        /** Concrete instructions the user can literally show to a barber. */
        val barberInstructions: String,
        /** Always false in v2.0: documented limitation, never fabricated. */
        val hasVisualExample: Boolean = false,
        val visualExampleNote: String =
            "No example image is available in the app. The written instructions below are the reliable reference — show them to your barber.",
    )

    val ALL: List<Haircut> = listOf(
        Haircut(
            id = "crew_cut",
            name = "Classic Crew Cut",
            length = "SHORT",
            maintenance = "LOW",
            stylingMinutes = 3,
            looks = setOf("PROFESSIONAL", "CASUAL", "VERSATILE"),
            description = "Short, uniform sides and back with a slightly longer top — clean and low-effort.",
            maintenanceGuidance = "Trim every 3–5 weeks. Minimal styling product needed; a light matte paste on top is optional.",
            barberInstructions = "Ask for a crew cut: about 2 on the sides/back, top left around 1.5–2 inches scissor-cut, natural taper at the neckline, no hard lines.",
        ),
        Haircut(
            id = "buzz_cut",
            name = "Buzz Cut (Uniform)",
            length = "SHORT",
            maintenance = "LOW",
            stylingMinutes = 1,
            looks = setOf("PROFESSIONAL", "CASUAL", "VERSATILE"),
            description = "One guard length all over. Zero styling, wash-and-go scale simplicity.",
            maintenanceGuidance = "Trim every 2–4 weeks for the sharp look; otherwise nothing. The simplest possible daily routine.",
            barberInstructions = "Ask for a uniform buzz all over — the barber will confirm guard length (commonly 2 or 3) with clean neckline taper.",
        ),
        Haircut(
            id = "textured_crop",
            name = "Textured Crop",
            length = "SHORT",
            maintenance = "MEDIUM",
            stylingMinutes = 6,
            looks = setOf("CASUAL", "VERSATILE"),
            description = "Choppy, textured top with short faded sides — modern, softens the forehead line.",
            maintenanceGuidance = "Trim every 3–4 weeks to keep the fade. Style with a light matte clay; scrunch, do not comb.",
            barberInstructions = "Ask for a textured crop: sides faded to about a 1 with a mid taper, top point-cut for texture, leave 2 inches of length up top.",
        ),
        Haircut(
            id = "side_part",
            name = "Side Part",
            length = "MEDIUM",
            maintenance = "MEDIUM",
            stylingMinutes = 8,
            looks = setOf("PROFESSIONAL", "VERSATILE"),
            description = "Tapered sides with a clean part — the classic professional silhouette.",
            maintenanceGuidance = "Trim every 4 weeks. Needs a comb and a light-to-medium-hold product; reapply after workouts.",
            barberInstructions = "Ask for a classic side part: tapered sides (clipper-over-comb), around 3 inches on top, a defined part line, natural finish at the neck.",
        ),
        Haircut(
            id = "french_crop",
            name = "French Crop",
            length = "SHORT",
            maintenance = "MEDIUM",
            stylingMinutes = 5,
            looks = setOf("CASUAL", "PROFESSIONAL"),
            description = "Forward-cut fringe with blunt texture and short sides — structured and forgiving.",
            maintenanceGuidance = "Trim every 3–4 weeks. A little matte product pushed forward daily; forgiving to bedhead.",
            barberInstructions = "Ask for a French crop: sides a 1.5 or 2, top around 2 inches cut bluntly, fringe pushed forward, soft neckline taper.",
        ),
        Haircut(
            id = "ivy_league",
            name = "Ivy League",
            length = "MEDIUM",
            maintenance = "MEDIUM",
            stylingMinutes = 8,
            looks = setOf("PROFESSIONAL", "VERSATILE"),
            description = "The grown-out crew cut with a part — smart, tidy, still sporty.",
            maintenanceGuidance = "Trim every 4 weeks. Style damp with a light pomade; can be combed or finger-scattered.",
            barberInstructions = "Ask for an Ivy League: tapered sides, top about 2.5 inches, enough to part, tapered neckline, no harsh sideburn cut-off.",
        ),
        Haircut(
            id = "curtain",
            name = "Curtain / Middle Part",
            length = "LONG",
            maintenance = "MEDIUM",
            stylingMinutes = 10,
            looks = setOf("CASUAL", "VERSATILE"),
            description = "Mid-length hair falling either side of the face in a soft middle part.",
            maintenanceGuidance = "Trim every 6–8 weeks. Needs a blow-dry or mousse and a light frizz-tamer; keep ends clean.",
            barberInstructions = "Ask for a middle-part curtain cut: keep 5–7 inches of length, light face-framing layering with scissors, minimal thinning, texturised ends only.",
        ),
        Haircut(
            id = "pompadour",
            name = "Pompadour",
            length = "MEDIUM",
            maintenance = "HIGH",
            stylingMinutes = 15,
            looks = setOf("PROFESSIONAL", "CASUAL"),
            description = "Volume swept up and back from the forehead, close sides — the statement style.",
            maintenanceGuidance = "Trim every 3 weeks to hold shape. Daily blow-dry plus product and a comb. Highest effort here; choose only if you enjoy styling.",
            barberInstructions = "Ask for a pompadour: sides a 1 skip 2 or scissor taper, top 4–5 inches for volume, weight removed on the sides, strong silhouette from the front.",
        ),
    )

    fun byId(id: String): Haircut? = ALL.firstOrNull { it.id == id }
}

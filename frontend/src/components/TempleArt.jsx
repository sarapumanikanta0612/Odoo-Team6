/**
 * Line drawings based on a South Indian gopuram (temple gateway tower).
 * Both use `currentColor`, so set the colour through CSS. They are decorative
 * and hidden from assistive technology.
 */

/** Compact brand mark for headers and the sidebar. */
export function TempleMark({ size = 32, className }) {
  return (
    <svg
      className={className}
      width={size}
      height={size}
      viewBox="0 0 64 64"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.4"
      strokeLinejoin="round"
      strokeLinecap="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M12 54h40" />
      <path d="M16 54V42h32v12" />
      <path d="M28 54v-6a4 4 0 0 1 8 0v6" />
      <path d="M17 42l2.5-8h25l2.5 8" />
      <path d="M21 34l2-7h18l2 7" />
      <path d="M24.5 27l1.5-5.5h12L39.5 27" />
      <path d="M26 21.5v-3h12v3" />
      <path d="M26 18.5a6 4.5 0 0 1 12 0" />
      <path d="M32 14v-4" />
      <circle cx="32" cy="8.5" r="1.6" fill="currentColor" stroke="none" />
    </svg>
  )
}

const CX = 120

// Each tier narrows towards the crown: bottom/top y, bottom/top half-width, sculpture niches.
const TIERS = [
  { yb: 208, yt: 184, hb: 82, ht: 76, niches: 7 },
  { yb: 184, yt: 162, hb: 72, ht: 66, niches: 6 },
  { yb: 162, yt: 142, hb: 62, ht: 56, niches: 5 },
  { yb: 142, yt: 124, hb: 52, ht: 46, niches: 4 },
  { yb: 124, yt: 108, hb: 42, ht: 36, niches: 3 },
]

// Barrel-vault crown (shala) and its kalasam finials.
const VAULT = { halfWidth: 34, base: 108, spring: 96, rise: 18 }
const FINIAL_OFFSETS = [-22, -11, 0, 11, 22]

function nichePositions(tier) {
  const span = (tier.ht - 10) * 2
  const step = tier.niches > 1 ? span / (tier.niches - 1) : 0
  return Array.from({ length: tier.niches }, (_, index) => CX - span / 2 + index * step)
}

function vaultHeightAt(offset) {
  const ratio = offset / VAULT.halfWidth
  return VAULT.spring - VAULT.rise * Math.sqrt(1 - ratio * ratio)
}

/** Large gopuram illustration for the landing and sign-in pages. */
export function GopuramIllustration({ className }) {
  const { halfWidth, base, spring, rise } = VAULT
  return (
    <svg
      className={className}
      viewBox="0 0 240 300"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinejoin="round"
      strokeLinecap="round"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx={CX} cy={112} r={74} strokeOpacity="0.28" />
      <circle cx={CX} cy={112} r={82} strokeOpacity="0.14" strokeDasharray="1 5" />

      {/* Plinth and steps */}
      <path d="M10 294h220" />
      <rect x="18" y="276" width="204" height="12" fill="currentColor" fillOpacity="0.05" />

      {/* Gateway body with doorway and pilasters */}
      <rect x="34" y="208" width="172" height="68" fill="currentColor" fillOpacity="0.04" />
      <path d="M104 276v-34a16 16 0 0 1 32 0v34" />
      <path d="M110 276v-32a10 10 0 0 1 20 0v32" strokeOpacity="0.55" />
      {[50, 66, 174, 190].map((x) => (
        <path key={x} d={`M${x} 216v54`} strokeOpacity="0.6" />
      ))}
      <rect x="76" y="222" width="14" height="30" rx="7" strokeOpacity="0.6" />
      <rect x="150" y="222" width="14" height="30" rx="7" strokeOpacity="0.6" />

      {/* Tapering tiers */}
      {TIERS.map((tier) => (
        <g key={tier.yb}>
          <path
            d={`M${CX - tier.hb} ${tier.yb}L${CX - tier.ht} ${tier.yt}H${CX + tier.ht}L${CX + tier.hb} ${tier.yb}Z`}
            fill="currentColor"
            fillOpacity="0.05"
          />
          <path d={`M${CX - tier.ht - 4} ${tier.yt}H${CX + tier.ht + 4}`} />
          {nichePositions(tier).map((x) => (
            <rect
              key={x}
              x={x - 3}
              y={tier.yt + (tier.yb - tier.yt) * 0.28}
              width="6"
              height={(tier.yb - tier.yt) * 0.5}
              rx="3"
              strokeOpacity="0.6"
            />
          ))}
        </g>
      ))}

      {/* Crown */}
      <path
        d={`M${CX - halfWidth} ${base}V${spring}A${halfWidth} ${rise} 0 0 1 ${CX + halfWidth} ${spring}V${base}`}
        fill="currentColor"
        fillOpacity="0.06"
      />
      <ellipse cx={CX} cy={spring + 2} rx="9" ry="6" strokeOpacity="0.6" />
      {FINIAL_OFFSETS.map((offset) => {
        const x = CX + offset
        const y = vaultHeightAt(offset)
        return (
          <g key={offset}>
            <path d={`M${x} ${y}v-6`} />
            <circle cx={x} cy={y - 8.5} r="2.4" fill="currentColor" fillOpacity="0.25" />
            <path d={`M${x} ${y - 11}v-3.5`} />
          </g>
        )
      })}
    </svg>
  )
}

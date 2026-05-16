/**
 * Converts decimal hours to a friendly "Xh Ym" string.
 * e.g. 24.5 → "24h 30m", 3.5 → "3h 30m", 7.0 → "7h"
 */
export function formatHours(decimalHours: number): string {
  const totalMinutes = Math.round(decimalHours * 60)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  if (minutes === 0) return `${hours}h`
  return `${hours}h ${minutes}m`
}

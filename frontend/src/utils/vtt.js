export function parseVttTimestamp(value) {
  const normalized = String(value || '').trim().replace(',', '.')
  if (!normalized) return Number.NaN
  const parts = normalized.split(':')
  if (parts.length < 2 || parts.length > 3) return Number.NaN
  const seconds = Number(parts.pop())
  const minutes = Number(parts.pop())
  const hours = parts.length ? Number(parts.pop()) : 0
  if (![hours, minutes, seconds].every(Number.isFinite)) return Number.NaN
  return hours * 3600 + minutes * 60 + seconds
}

export function parseVtt(content) {
  const normalized = String(content || '')
    .replace(/^\uFEFF/, '')
    .replace(/\r\n?/g, '\n')
    .trim()
  if (!normalized) return []

  const cues = []
  for (const block of normalized.split(/\n{2,}/)) {
    const lines = block.split('\n').map((line) => line.trimEnd())
    if (!lines.length || /^WEBVTT(?:\s|$)/.test(lines[0]) || /^NOTE(?:\s|$)/.test(lines[0])) {
      continue
    }
    const timingIndex = lines.findIndex((line) => line.includes('-->'))
    if (timingIndex < 0) continue
    const [startPart, endPart] = lines[timingIndex].split('-->')
    const start = parseVttTimestamp(startPart?.trim().split(/\s+/)[0])
    const end = parseVttTimestamp(endPart?.trim().split(/\s+/)[0])
    const text = lines.slice(timingIndex + 1).join(' ').trim()
    if (!text || !Number.isFinite(start) || !Number.isFinite(end) || end <= start) continue
    cues.push({ start, end, text })
  }
  return cues.sort((left, right) => left.start - right.start)
}

export function findCueIndex(cues, currentTime) {
  if (!Array.isArray(cues) || !Number.isFinite(currentTime)) return -1
  return cues.findIndex((cue) => currentTime >= cue.start && currentTime < cue.end)
}

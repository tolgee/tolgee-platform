import type { Express, Request, Response } from 'express'
import { store } from '../store'

type SetEmojiRequest = {
  translationId?: unknown
  emoji?: unknown
}

export const registerEmojiRoute = (app: Express): void => {
  app.put('/api/emoji', handleSetEmoji)
}

const handleSetEmoji = (req: Request, res: Response): void => {
  const parsed = parseRequest(req.body)
  if (!parsed.ok) {
    res.status(400).json({ error: parsed.error })
    return
  }
  store.setEmoji(parsed.translationId, parsed.emoji)
  res.json({ ok: true })
}

type ParsedSetEmoji =
  | { ok: true; translationId: number; emoji: string | null }
  | { ok: false; error: string }

const parseRequest = (body: unknown): ParsedSetEmoji => {
  const b = (body ?? {}) as SetEmojiRequest
  if (typeof b.translationId !== 'number') {
    return { ok: false, error: 'translationId must be a number' }
  }
  if (b.emoji != null && typeof b.emoji !== 'string') {
    return { ok: false, error: 'emoji must be a string or null' }
  }
  return {
    ok: true,
    translationId: b.translationId,
    emoji: typeof b.emoji === 'string' ? b.emoji : null,
  }
}

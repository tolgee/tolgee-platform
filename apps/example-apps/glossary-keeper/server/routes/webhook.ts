import express, { type Express, type Request, type Response } from 'express'
import {
  onWebhook,
  verifyWebhookSignature,
  type AppWebhookPayload,
  type WebhookPayloadFor,
} from '@tolgee/apps-sdk/server'
import { WEBHOOK_SECRET } from '../config'
import {
  detectSuspicious,
  isAnthropicConfigured,
  suggestGlossaryEntry,
} from '../anthropic'
import { suggestionId, upsertSuggestion } from '../store'
import { fetchKeyContext, getProjectLanguages, tolgeeReady } from '../tolgeeApi'

export const registerWebhookRoute = (app: Express): void => {
  // express.text keeps the body verbatim — the SDK verifier needs the raw bytes Tolgee signed.
  app.post(
    '/webhook',
    express.text({ type: 'application/json', limit: '5mb' }),
    handleWebhook
  )
}

const handleWebhook = async (req: Request, res: Response): Promise<void> => {
  const raw = typeof req.body === 'string' ? req.body : ''
  if (!(await verifyOrAllow(req, raw))) {
    res.status(401).json({ error: 'invalid signature' })
    return
  }
  const payload = parsePayload(raw)
  if (!payload) {
    res.status(400).json({ error: 'invalid json' })
    return
  }

  onWebhook(payload, 'SET_TRANSLATIONS', (typed) => {
    void handleSetTranslations(typed).catch((err) => {
      console.error('[webhook] SET_TRANSLATIONS handler failed', err)
    })
  })

  // Always 204 — Tolgee shouldn't retry while the (slow) LLM analysis runs.
  res.status(204).end()
}

type SetTranslationsPayload = WebhookPayloadFor<'SET_TRANSLATIONS'>

type ModifiedTranslation = {
  languageId?: number
  keyId?: number
  text?: string
}

const handleSetTranslations = async (
  payload: SetTranslationsPayload
): Promise<void> => {
  if (!tolgeeReady()) {
    console.warn('[webhook] install record missing — run `npm run register`.')
    return
  }
  if (!isAnthropicConfigured()) {
    console.warn('[webhook] ANTHROPIC_API_KEY not set — skipping analysis.')
    return
  }
  const data = payload.activityData
  if (!data) return
  // Activity payloads carry projectId on the wrapper, not on each entity.
  const projectId = (data as { projectId?: number }).projectId
  if (typeof projectId !== 'number') return
  const mods = (data.modifiedEntities?.Translation ?? []) as ModifiedTranslation[]
  if (mods.length === 0) return

  const langs = await getProjectLanguages(projectId)

  for (const m of mods) {
    if (typeof m.keyId !== 'number' || typeof m.languageId !== 'number') continue
    const tag = langs.byId.get(m.languageId)
    // Only inspect non-base translations (the term is judged against the base text).
    if (!tag || tag === langs.baseLang) continue
    if (typeof m.text !== 'string' || m.text.length === 0) continue
    await analyseOne(projectId, m.keyId, tag, m.text, langs.baseLang).catch((err) => {
      console.error(`[webhook] analysis failed for key ${m.keyId}/${tag}`, err)
    })
  }
}

const analyseOne = async (
  projectId: number,
  keyId: number,
  languageTag: string,
  text: string,
  baseLang: string
): Promise<void> => {
  const ctx = await fetchKeyContext(projectId, keyId, baseLang, languageTag)
  if (!ctx.baseText) return

  const detection = await detectSuspicious({
    baseLang,
    baseText: ctx.baseText,
    languageTag,
    text,
  })
  if (!detection.suspicious) return

  const entry = await suggestGlossaryEntry({
    baseLang,
    baseText: ctx.baseText,
    languageTag,
    text,
    candidateTerm: detection.candidateTerm,
  })

  upsertSuggestion({
    id: suggestionId(keyId, languageTag),
    projectId,
    keyId,
    keyName: ctx.keyName,
    languageTag,
    term: entry.term,
    translation: entry.translation,
    description: entry.description,
    sourceText: text,
    createdAt: new Date().toISOString(),
  })
}

const verifyOrAllow = async (req: Request, raw: string): Promise<boolean> => {
  if (!WEBHOOK_SECRET) {
    console.warn(
      'TOLGEE_WEBHOOK_SECRET is not set — accepting webhook without verification.'
    )
    return true
  }
  return verifyWebhookSignature({
    header: req.header('Tolgee-Signature'),
    rawBody: raw,
    secret: WEBHOOK_SECRET,
  })
}

const parsePayload = (raw: string): AppWebhookPayload | null => {
  try {
    return JSON.parse(raw) as AppWebhookPayload
  } catch {
    return null
  }
}

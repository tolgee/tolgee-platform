import express, { type Express, type Request, type Response } from 'express'
import {
  onWebhook,
  verifyWebhookSignature,
  type AppWebhookPayload,
  type WebhookPayloadFor,
} from '@tolgee/apps-sdk/server'
import { WEBHOOK_SECRET } from '../config'
import { analyse, isAnthropicConfigured } from '../backTranslation'
import {
  getAnalysis,
  invalidateKey,
  upsertAnalysis,
} from '../store'
import { fetchKeyTranslations, getProjectLanguages, tolgeeReady } from '../tolgeeApi'

export const registerWebhookRoute = (app: Express): void => {
  // express.text parses the body verbatim — the SDK verifier needs the
  // raw bytes Tolgee signed.
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

  // Always 204 — Tolgee shouldn't retry while a slow re-analysis runs.
  res.status(204).end()
}

type SetTranslationsPayload = WebhookPayloadFor<'SET_TRANSLATIONS'>

type ModifiedTranslation = {
  id?: number
  languageId?: number
  keyId?: number
  text?: string
}

const handleSetTranslations = async (
  payload: SetTranslationsPayload
): Promise<void> => {
  if (!tolgeeReady()) {
    console.warn(
      '[webhook] TOLGEE_PAT not set — skipping back-translation refresh.'
    )
    return
  }
  if (!isAnthropicConfigured()) {
    console.warn(
      '[webhook] ANTHROPIC_API_KEY not set — skipping back-translation refresh.'
    )
    return
  }
  const data = payload.activityData
  if (!data) return
  // Activity payloads carry projectId on the wrapper, not on each entity.
  const projectId = (data as { projectId?: number }).projectId
  if (typeof projectId !== 'number') {
    console.warn('[webhook] SET_TRANSLATIONS missing projectId')
    return
  }
  const mods = (data.modifiedEntities?.Translation ??
    []) as ModifiedTranslation[]
  if (mods.length === 0) return

  const langs = await getProjectLanguages(projectId)

  // Bucket the changed entries by keyId, separating base-language
  // edits (which invalidate every analysis for the key) from non-base
  // edits (which trigger a fresh re-analysis for that specific cell).
  const baseEdits = new Set<number>()
  const reanalyse: Array<{
    keyId: number
    languageTag: string
    text: string
  }> = []

  for (const m of mods) {
    if (typeof m.keyId !== 'number' || typeof m.languageId !== 'number') continue
    const tag = langs.byId.get(m.languageId)
    if (!tag) continue
    if (tag === langs.baseLang) {
      baseEdits.add(m.keyId)
      continue
    }
    if (typeof m.text !== 'string' || m.text.length === 0) continue
    reanalyse.push({ keyId: m.keyId, languageTag: tag, text: m.text })
  }

  // Apply base-language invalidations first so re-analyses overwrite
  // cleanly.
  for (const keyId of baseEdits) invalidateKey(keyId)

  for (const job of reanalyse) {
    // Skip if the stored text already matches — Tolgee sometimes re-fires
    // SET_TRANSLATIONS for no-op saves.
    const existing = getAnalysis(job.keyId, job.languageTag)
    if (existing && existing.text === job.text) continue

    try {
      const fetched = await fetchKeyTranslations(
        projectId,
        job.keyId,
        langs.baseLang,
        [job.languageTag]
      )
      const baseText = fetched.baseText
      if (!baseText) continue

      const analysis = await analyse({
        text: job.text,
        sourceLang: job.languageTag,
        baseText,
        baseLang: langs.baseLang,
      })
      upsertAnalysis({
        ...analysis,
        keyId: job.keyId,
        languageTag: job.languageTag,
        baseLang: langs.baseLang,
        baseText,
        text: job.text,
        analysedAt: new Date().toISOString(),
      })
    } catch (err) {
      console.error(
        `[webhook] back-translation failed for key ${job.keyId}/${job.languageTag}`,
        err
      )
    }
  }
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

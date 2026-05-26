import express, { type Express, type Request, type Response } from 'express'
import { verifySignature } from '../signature'
import { store } from '../store'
import {
  onWebhook,
  type AppWebhookPayload,
  type WebhookPayloadFor,
} from '../webhookHandler'

export const registerWebhookRoute = (app: Express): void => {
  // express.text parses the body verbatim — needed because the HMAC
  // signature is computed over the raw bytes Tolgee sent.
  app.post(
    '/webhook',
    express.text({ type: 'application/json', limit: '5mb' }),
    handleWebhook
  )
}

const handleWebhook = (req: Request, res: Response): void => {
  const raw = typeof req.body === 'string' ? req.body : ''
  if (!verifySignature(req, raw)) {
    res.status(401).json({ error: 'invalid signature' })
    return
  }
  const payload = parsePayload(raw)
  if (!payload) {
    res.status(400).json({ error: 'invalid json' })
    return
  }
  onWebhook(payload, 'SET_TRANSLATIONS', recordTranslationActivity)
  res.status(204).end()
}

const parsePayload = (raw: string): AppWebhookPayload | null => {
  try {
    return JSON.parse(raw) as AppWebhookPayload
  } catch {
    return null
  }
}

const recordTranslationActivity = (
  payload: WebhookPayloadFor<'SET_TRANSLATIONS'>
): void => {
  const occurredAt = isoTimestamp(payload.activityData?.timestamp)
  const translations = payload.activityData?.modifiedEntities?.Translation ?? []
  store.recordTranslationEdits(translations, occurredAt)
}

const isoTimestamp = (ts: number | undefined): string =>
  typeof ts === 'number'
    ? new Date(ts).toISOString()
    : new Date().toISOString()

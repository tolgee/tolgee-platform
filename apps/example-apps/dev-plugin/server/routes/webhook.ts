import express, { type Express, type Request, type Response } from 'express'
import {
  onWebhook,
  verifyWebhookSignature,
  type AppWebhookPayload,
  type WebhookPayloadFor,
} from '@tolgee/apps-sdk/server'
import { WEBHOOK_SECRET } from '../config'
import { store } from '../store'

export const registerWebhookRoute = (app: Express): void => {
  // express.text parses the body verbatim — needed because the HMAC
  // signature is computed over the raw bytes Tolgee sent.
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
  onWebhook(payload, 'SET_TRANSLATIONS', recordTranslationActivity)
  res.status(204).end()
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

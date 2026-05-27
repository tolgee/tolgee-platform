import express, { type Express, type Request, type Response } from 'express'
import {
  onWebhook,
  verifyWebhookSignature,
  type AppWebhookPayload,
} from '@tolgee/apps-sdk/server'
import { WEBHOOK_SECRET } from '../config'

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

  // {{webhookHandlers}}

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

// Silence the unused-import warning when the wizard generated no
// handlers — `onWebhook` is referenced only inside `{{webhookHandlers}}`.
void onWebhook

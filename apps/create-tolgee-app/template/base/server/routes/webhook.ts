import express, { type Express, type Request, type Response } from 'express'
import { onWebhook, receiveWebhook } from '@tolgee/apps-sdk/server'
import { WEBHOOK_SECRET } from '../config'

export const registerWebhookRoute = (app: Express): void => {
  // express.text keeps the body verbatim — the SDK verifier needs the raw
  // bytes Tolgee signed, not a parsed object.
  app.post(
    '/webhook',
    express.text({ type: 'application/json', limit: '5mb' }),
    handleWebhook
  )
}

const handleWebhook = async (req: Request, res: Response): Promise<void> => {
  const raw = typeof req.body === 'string' ? req.body : ''
  const result = await receiveWebhook({
    rawBody: raw,
    signatureHeader: req.header('Tolgee-Signature'),
    secret: WEBHOOK_SECRET,
  })
  if (!result.ok) {
    res.status(result.status).json({ error: result.error })
    return
  }
  const payload = result.payload

  // {{webhookHandlers}}

  res.status(204).end()
}

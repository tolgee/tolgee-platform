import type { Express, Request, Response } from 'express'
import { store } from '../store'

export const registerStateRoute = (app: Express): void => {
  app.get('/api/state', handleGetState)
}

const handleGetState = (req: Request, res: Response): void => {
  const ids = parseIds(req.query.ids)
  res.json(store.getState(ids))
}

const parseIds = (raw: unknown): string[] =>
  String(raw ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)

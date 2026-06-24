import type { Express, Request, Response } from 'express'
import { getManifest } from '../manifest'

export const registerManifestRoute = (app: Express): void => {
  app.get('/manifest.json', (_req: Request, res: Response) => {
    res.type('application/json').send(getManifest())
  })
}

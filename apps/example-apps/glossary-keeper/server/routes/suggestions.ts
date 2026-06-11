import type { Express } from 'express'
import { listByProject } from '../store'

/** GET /suggestions?projectId= — pending glossary suggestions for the dashboard. */
export const registerSuggestionsRoute = (app: Express): void => {
  app.get('/suggestions', (req, res) => {
    const projectId = Number(req.query.projectId)
    if (!Number.isFinite(projectId)) {
      res.status(400).json({ error: 'projectId query param is required' })
      return
    }
    res.json({ suggestions: listByProject(projectId) })
  })
}

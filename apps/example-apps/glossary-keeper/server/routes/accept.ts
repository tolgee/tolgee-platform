import type { Express } from 'express'
import {
  getSuggestion,
  listByProject,
  removeSuggestion,
  type Suggestion,
} from '../store'
import { createGlossaryTerm, setGlossaryTranslation } from '../tolgeeApi'

/**
 * Writes a suggestion into the chosen glossary: creates the term (base-language text) and
 * sets its translation in the changed language, then drops it from the pending store. Uses
 * the install's `glossary.edit` org scope (org-level app authorization).
 */
const acceptOne = async (
  suggestion: Suggestion,
  glossaryId: number
): Promise<void> => {
  const termId = await createGlossaryTerm(
    glossaryId,
    suggestion.term,
    suggestion.description
  )
  await setGlossaryTranslation(
    glossaryId,
    termId,
    suggestion.languageTag,
    suggestion.translation
  )
  removeSuggestion(suggestion.id)
}

export const registerAcceptRoutes = (app: Express): void => {
  app.post('/accept', async (req, res) => {
    const body = req.body as { suggestionId?: unknown; glossaryId?: unknown }
    if (typeof body.suggestionId !== 'string' || typeof body.glossaryId !== 'number') {
      res.status(400).json({ error: 'suggestionId (string) and glossaryId (number) required' })
      return
    }
    const suggestion = getSuggestion(body.suggestionId)
    if (!suggestion) {
      res.status(404).json({ error: 'suggestion not found' })
      return
    }
    try {
      await acceptOne(suggestion, body.glossaryId)
      res.json({ accepted: suggestion.id })
    } catch (err) {
      res.status(502).json({ error: 'glossary write failed', message: String(err) })
    }
  })

  app.post('/accept-all', async (req, res) => {
    const body = req.body as { projectId?: unknown; glossaryId?: unknown }
    if (typeof body.projectId !== 'number' || typeof body.glossaryId !== 'number') {
      res.status(400).json({ error: 'projectId (number) and glossaryId (number) required' })
      return
    }
    const accepted: string[] = []
    const failed: Array<{ id: string; message: string }> = []
    for (const suggestion of listByProject(body.projectId)) {
      try {
        await acceptOne(suggestion, body.glossaryId)
        accepted.push(suggestion.id)
      } catch (err) {
        failed.push({ id: suggestion.id, message: String(err) })
      }
    }
    res.json({ accepted, failed })
  })
}

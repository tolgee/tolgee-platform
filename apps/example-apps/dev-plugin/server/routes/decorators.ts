import type { Express, Request, Response } from 'express'

type DecoratorRequest = {
  projectId?: number
  keyIds?: number[]
  languageTags?: string[]
}

type DecoratorItem = {
  keyId: number
  languageTag?: string
  actionKey: string
  url?: string
  count?: number
  visibility?: 'always' | 'on-hover'
}

export const registerDecoratorsRoute = (app: Express): void => {
  app.post('/decorators', handleDecorators)
}

const handleDecorators = (req: Request, res: Response): void => {
  const { keyIds, languageTags } = parseRequest(req.body)
  res.json({ items: buildItems(keyIds, languageTags) })
}

const parseRequest = (
  body: unknown
): { keyIds: number[]; languageTags: string[] } => {
  const b = (body ?? {}) as DecoratorRequest
  return {
    keyIds: Array.isArray(b.keyIds)
      ? b.keyIds.filter((id): id is number => typeof id === 'number')
      : [],
    languageTags: Array.isArray(b.languageTags)
      ? b.languageTags.filter((t): t is string => typeof t === 'string')
      : [],
  }
}

const buildItems = (
  keyIds: number[],
  languageTags: string[]
): DecoratorItem[] => {
  const items: DecoratorItem[] = []
  for (const keyId of keyIds) {
    items.push(auditItem(keyId))
    for (const tag of languageTags) {
      items.push(activityItem(keyId, tag))
    }
  }
  return items
}

// Demo: every key gets the audit shortcut.
const auditItem = (keyId: number): DecoratorItem => ({
  keyId,
  actionKey: 'open-audit',
})

// Demo: show-activity is emitted for every (keyId, languageTag) pair,
// with a synthetic count varying 0..6. count===0 hides the badge but
// keeps the icon; we also dynamically override the visibility to
// 'on-hover' so quiet cells declutter the row.
const activityItem = (keyId: number, languageTag: string): DecoratorItem => {
  const count = (keyId * 3 + languageTag.charCodeAt(0)) % 7
  const item: DecoratorItem = {
    keyId,
    languageTag,
    actionKey: 'show-activity',
    count,
  }
  if (count === 0) item.visibility = 'on-hover'
  return item
}

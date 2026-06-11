import { describe, expect, it } from 'vitest'
import { extractJson } from './anthropic'

describe('extractJson', () => {
  it('parses a plain JSON object', () => {
    expect(extractJson<{ a: number }>('{"a": 1}')).toEqual({ a: 1 })
  })

  it('strips markdown ```json fences', () => {
    const raw =
      '```json\n{"suspicious": true, "candidateTerm": "Cobalt", "reason": "term"}\n```'
    expect(extractJson(raw)).toEqual({
      suspicious: true,
      candidateTerm: 'Cobalt',
      reason: 'term',
    })
  })

  it('ignores prose around the object', () => {
    const raw =
      'Sure! Here you go:\n{"term":"Cobalt","translation":"Kobalt","description":"a metal"} — hope that helps'
    expect(extractJson(raw)).toEqual({
      term: 'Cobalt',
      translation: 'Kobalt',
      description: 'a metal',
    })
  })

  it('throws when there is no JSON object', () => {
    expect(() => extractJson('no json here')).toThrow()
  })
})

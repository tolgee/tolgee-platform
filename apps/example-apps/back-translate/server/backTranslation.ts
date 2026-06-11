import Anthropic from '@anthropic-ai/sdk'
import { ANTHROPIC_API_KEY, ANTHROPIC_MODEL } from './config'
import type { Analysis } from './store'

export type AnalyseInput = {
  text: string
  sourceLang: string
  baseText: string
  baseLang: string
}

const client = ANTHROPIC_API_KEY
  ? new Anthropic({ apiKey: ANTHROPIC_API_KEY })
  : null

const prompt = (input: AnalyseInput): string =>
  `You evaluate a translation by back-translating it and comparing semantically.

Original (${input.baseLang}): "${input.baseText}"
Translation (${input.sourceLang}): "${input.text}"

Steps:
1. Translate the ${input.sourceLang} text back to ${input.baseLang}.
2. Compare your back-translation to the original ${input.baseLang} text.
   - "match"    — identical meaning, no nuance lost
   - "close"    — same intent, minor word-choice differences
   - "mismatch" — different meaning, lost in translation

Respond ONLY with valid JSON, no preamble, no markdown:
{
  "backTranslation": "...",
  "verdict": "match" | "close" | "mismatch",
  "explanation": "one short sentence"
}`

const extractJson = (raw: string): Analysis => {
  const cleaned = raw.replace(/```(?:json)?/g, '').trim()
  const start = cleaned.indexOf('{')
  const end = cleaned.lastIndexOf('}')
  if (start < 0 || end < 0) throw new Error('No JSON in model response')
  return JSON.parse(cleaned.slice(start, end + 1)) as Analysis
}

export const isAnthropicConfigured = (): boolean => client !== null

export async function analyse(input: AnalyseInput): Promise<Analysis> {
  if (!client) {
    throw new Error('ANTHROPIC_API_KEY is not set in the server environment.')
  }
  const response = await client.messages.create({
    model: ANTHROPIC_MODEL,
    max_tokens: 400,
    messages: [{ role: 'user', content: prompt(input) }],
  })
  const block = response.content[0]
  const rawText = block && block.type === 'text' ? block.text : ''
  return extractJson(rawText)
}

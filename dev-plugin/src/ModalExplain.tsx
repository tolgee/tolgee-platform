import { useEffect, useState } from 'react'
import './App.css'

type InitContext = {
  token?: string
  apiUrl?: string
  organizationId?: number | null
  projectId?: number
  keyId?: number | null
  languageId?: number | null
  languageTag?: string | null
  translationId?: number | null
  selectedKeyIds?: number[]
}

function ModalExplain() {
  const [context, setContext] = useState<InitContext | null>(null)
  const [hostOrigin, setHostOrigin] = useState<string | null>(null)

  useEffect(() => {
    const handler = (event: MessageEvent) => {
      if (!event.data || typeof event.data !== 'object') return
      if (event.data.type !== 'tolgee-app:init') return
      setHostOrigin(event.origin)
      setContext(event.data as InitContext)
    }
    window.addEventListener('message', handler)
    window.parent.postMessage({ type: 'tolgee-app:ready' }, '*')
    return () => window.removeEventListener('message', handler)
  }, [])

  const close = () => {
    if (!hostOrigin) return
    window.parent.postMessage({ type: 'tolgee-app:close' }, hostOrigin)
  }

  return (
    <main className="modal-explain">
      <h2>💡 Plugin modal — init context</h2>
      {!context ? (
        <p>Waiting for host context…</p>
      ) : (
        <table className="kv">
          <tbody>
            {(
              [
                ['projectId', context.projectId],
                ['organizationId', context.organizationId],
                ['keyId', context.keyId],
                ['languageId', context.languageId],
                ['languageTag', context.languageTag],
                ['translationId', context.translationId],
                ['selectedKeyIds', context.selectedKeyIds?.join(', ')],
              ] as Array<[string, unknown]>
            )
              .filter(([, value]) => value !== undefined && value !== null && value !== '')
              .map(([key, value]) => (
                <tr key={key}>
                  <th>{key}</th>
                  <td>
                    <code>{String(value)}</code>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      )}
      <div className="modal-actions">
        <button type="button" onClick={close}>
          Close
        </button>
      </div>
    </main>
  )
}

export default ModalExplain

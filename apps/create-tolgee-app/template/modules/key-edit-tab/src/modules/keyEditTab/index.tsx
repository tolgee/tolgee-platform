import { useEffect, useState } from 'react'
import {
  createTolgeeApp,
  type TolgeeAppSelection,
} from '@tolgee/apps-sdk/browser'

export default function KeyEditTab() {
  const [selection, setSelection] = useState<TolgeeAppSelection>({})

  useEffect(() => {
    const app = createTolgeeApp()
    app.context.then((ctx) => setSelection(ctx.selection))
    const off = app.onSelectionChanged(setSelection)
    return () => {
      off()
      app.dispose()
    }
  }, [])

  return (
    <main>
      <h2>{{name}} key-edit tab</h2>
      {selection.keyId == null ? (
        <p>Open a key to see its plugin tab content.</p>
      ) : (
        <p>
          Inspecting key <code>{selection.keyId}</code>.
        </p>
      )}
    </main>
  )
}

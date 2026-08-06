import type { IncomingMessage, ServerResponse } from 'node:http'
import {
  receiveTolgeeDelivery,
  type DeliveryResult,
  type TolgeeLifecycleOptions,
} from './receiveDelivery'
import { TOLGEE_SIGNATURE_HEADER } from './signature'

/**
 * Paths `mountTolgeeLifecycle` accepts a delivery on.
 *
 * Tolgee POSTs to the manifest's `baseUrl` itself, which is `/` of the app's
 * server — hence the first. The second is for an app that would rather route
 * deliveries somewhere explicit (a proxy, a `baseUrl` it does not own the root
 * of); pass `paths` to use only that one.
 */
export const TOLGEE_LIFECYCLE_PATHS = ['/', '/tolgee/lifecycle']

export type TolgeeLifecycleHandlerOptions = TolgeeLifecycleOptions & {
  /**
   * Called for every delivery that was refused, so an operator sees why. The
   * message never contains secret material.
   */
  onRejected?: (
    rejected: Extract<DeliveryResult, { accepted: false }>
  ) => void
  /** Called when a listener threw; the delivery is answered 500 and Tolgee retries. */
  onError?: (error: unknown) => void
}

/**
 * A Node/Express request handler that receives Tolgee's signed lifecycle
 * deliveries: verifies them, stores the credentials they carry and calls the
 * listeners in `on`.
 *
 *     app.post('/tolgee/lifecycle', createTolgeeLifecycleHandler({
 *       tolgeeUrl: config.tolgeeUrl,
 *       on: { installed: (e) => console.log(`installed ${e.install?.installId}`) },
 *     }))
 *
 * **Mount it before any JSON body parser.** The signature covers the exact
 * bytes Tolgee sent, so the handler reads the raw request itself; a parser that
 * consumed the stream first leaves nothing to verify.
 */
export const createTolgeeLifecycleHandler = (
  options: TolgeeLifecycleHandlerOptions = {}
): ((request: IncomingMessage, response: ServerResponse) => void) => {
  return (request, response) => {
    void handle(request, response, options)
  }
}

/**
 * Anything with an Express-shaped `post()` — an app or a router.
 *
 * Structural on purpose: the SDK stays free of a framework dependency, and any
 * router with the same shape works.
 */
export type TolgeeLifecycleMountTarget = {
  post(
    path: string,
    handler: (request: IncomingMessage, response: ServerResponse) => void
  ): unknown
}

export type MountLifecycleOptions = TolgeeLifecycleHandlerOptions & {
  /** Defaults to `TOLGEE_LIFECYCLE_PATHS`. */
  paths?: string[]
}

/**
 * Wires the lifecycle receiver into an Express app in one call, and returns the
 * paths it now answers on.
 *
 *     mountTolgeeLifecycle(app, { tolgeeUrl: config.tolgeeUrl })
 *
 * Call it before `app.use(express.json())` — see
 * `createTolgeeLifecycleHandler`.
 */
export const mountTolgeeLifecycle = (
  target: TolgeeLifecycleMountTarget,
  options: MountLifecycleOptions = {}
): string[] => {
  const paths = options.paths ?? TOLGEE_LIFECYCLE_PATHS
  const handler = createTolgeeLifecycleHandler(options)
  for (const path of paths) target.post(path, handler)
  return paths
}

const handle = async (
  request: IncomingMessage,
  response: ServerResponse,
  options: TolgeeLifecycleHandlerOptions
): Promise<void> => {
  if (request.method !== undefined && request.method !== 'POST') {
    send(response, 405, { error: 'Lifecycle deliveries are POSTed.' })
    return
  }

  let rawBody: string
  try {
    rawBody = await readRawBody(request)
  } catch (error) {
    options.onError?.(error)
    send(response, 500, {
      error: error instanceof Error ? error.message : 'Could not read the request body.',
    })
    return
  }

  try {
    const result = await receiveTolgeeDelivery({
      ...options,
      rawBody,
      signatureHeader: request.headers[TOLGEE_SIGNATURE_HEADER],
    })
    if (!result.accepted) {
      options.onRejected?.(result)
      send(response, result.status, {
        error: result.message,
        rejection: result.rejection,
      })
      return
    }
    send(response, 200, { received: true, event: result.event.type })
  } catch (error) {
    // A listener failed. Answering non-2xx is what makes Tolgee deliver again.
    options.onError?.(error)
    send(response, 500, { error: 'Handling the delivery failed.' })
  }
}

const send = (
  response: ServerResponse,
  status: number,
  body: Record<string, unknown>
): void => {
  response.statusCode = status
  response.setHeader('Content-Type', 'application/json')
  response.end(JSON.stringify(body))
}

/**
 * The bytes that were signed. A body parser mounted ahead of this handler has
 * already drained the stream, and re-serialising what it produced does not give
 * the same bytes back — so that case is a wiring mistake, reported as one.
 */
const readRawBody = async (request: IncomingMessage): Promise<string> => {
  const parsed = (request as { body?: unknown; rawBody?: unknown }).rawBody ??
    (request as { body?: unknown }).body
  if (typeof parsed === 'string') return parsed
  if (Buffer.isBuffer(parsed)) return parsed.toString('utf8')
  if (typeof parsed === 'object' && parsed !== null) {
    throw new Error(
      'The request body was already parsed, so the exact bytes Tolgee signed are gone. Mount the ' +
        'Tolgee lifecycle route before express.json() (or any other body parser).'
    )
  }

  const chunks: Buffer[] = []
  for await (const chunk of request) {
    chunks.push(typeof chunk === 'string' ? Buffer.from(chunk) : chunk)
  }
  return Buffer.concat(chunks).toString('utf8')
}

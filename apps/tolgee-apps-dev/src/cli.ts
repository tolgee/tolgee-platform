import { loadEnvLocal } from './lib'
import { runDev } from './dev'
import { runRegister } from './register'

const USAGE = `tolgee-app — Tolgee Apps dev toolchain

Usage:
  tolgee-app dev         Boot the tunnel + repoint the install's manifest URL
  tolgee-app register    One-time install against a Tolgee org (browser flow)
                         Pass --pat=tgpat_… for the headless flow.
`

const main = async (): Promise<void> => {
  loadEnvLocal()
  const [command, ...rest] = process.argv.slice(2)

  if (command === 'dev') {
    await runDev()
    return
  }
  if (command === 'register') {
    await runRegister(rest)
    return
  }

  console.log(USAGE)
  process.exit(command ? 1 : 0)
}

main().catch((err) => {
  console.error(err instanceof Error ? err.message : err)
  process.exit(1)
})

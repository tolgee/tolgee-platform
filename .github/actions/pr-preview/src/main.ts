import * as core from '@actions/core'
import {config} from './config'
import {context} from '@actions/github'
import {userWhitelist} from './user-whitelist'

async function run(): Promise<void> {
  try {
    const body: string =
      (context.eventName === 'issue_comment'
        ? // For comments on pull requests
          context.payload?.comment?.body
        : // For the initial pull request description
          context.payload?.pull_request?.body) || ''

    if (!body) {
      core.info('No PR comment body')
      return
    }

    const args = body.split(/\s+/)
    const command = args.shift()

    if (command !== config.command) {
      core.info("That's not the command")
    }

    const isAllowed = userWhitelist.includes(context.actor)

    if (!isAllowed) {
      core.info('User is not allowed to trigger preview deployment')
      throw new Error('User is not allowed to trigger preview deployment')
    }

    core.setOutput('trigger', true)
    core.setOutput(
      'trigger_payload',
      JSON.stringify({
        ref: 'main',
        inputs: {
          pr: context.payload?.pull_request?.number
        }
      })
    )
  } catch (error) {
    if (error instanceof Error) core.setFailed(error.message)
  }
}

run()

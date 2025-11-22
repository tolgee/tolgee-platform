export function showConsoleHello() {
  // eslint-disable-next-line no-console
  console.log(
    '%cWelcome to Tolgee! 🐭%c\n' +
      '\n' +
      'See something to improve? Open an issue or send a PR!\n' +
      '\n' +
      '📚 Docs: https://docs.tolgee.io\n' +
      '🌟 Source code: https://github.com/tolgee/tolgee-platform\n' +
      '🐞 Report an issue: https://github.com/tolgee/tolgee-platform/issues/new/choose\n' +
      '\n' +
      'PSST! 🤫 We’re hiring! https://tolgee.io/career',
    `padding-top: 0.5em; font-size: 2em;`,
    'padding-bottom: 0.5em;'
  );
}

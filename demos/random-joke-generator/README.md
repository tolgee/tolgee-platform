# Random Recipe Generator with Tolgee
[<img src="https://raw.githubusercontent.com/tolgee/documentation/main/tolgee_logo_text.svg" alt="Tolgee" width="100" />](https://tolgee.io)

### [Live Website](https://recipe-generator-tolgee.vercel.app/)
### [Dev.to Blog Link](https://dev.to/sanketshinde/random-recipe-generator-with-tolgee-2di5)
##
Here i used `tolgee` with `next.js` app router.
based on `next14` app folder with `tolgee` and `next-intl` package.

## Setup

1. Clone this repo
2. Run `npm i`
3. Add Gemini Api key in `.env.development.local` file.
4. Run `npm run dev`

## Setup tolgee credentials (optional)

5. Create project in Tolgee platform
6. Add `.env.development.local` file to base folder of this project with an API key to your project

```
NEXT_PUBLIC_TOLGEE_API_URL=https://app.tolgee.io
NEXT_PUBLIC_TOLGEE_API_KEY=<your project API key>
```

7. Re-run `npm run dev`

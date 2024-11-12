# Multilingual Phrasebook using Tolgee
[<img src="https://raw.githubusercontent.com/tolgee/documentation/main/tolgee_logo_text.svg" alt="Tolgee" width="100" />](https://tolgee.io)

## Link
[Live Link](https://phrase-book-five.vercel.app/)
[dev.to](https://dev.to/mayank_mohapatra/phrasebook-with-tolgee-563g)

##
Here i used `tolgee` with `next.js` app router.
based on `next14` app folder with `tolgee` and `next-intl` package.

## Features
Bookmark Feature:
Users can bookmark specific pages for quick access. This feature allows returning to marked phrases instantly. Bookmarked pages will be saved in local storage, enabling persistence even after refreshing or closing the browser. The bookmarked pages can also be displayed in a list view for easy navigation.

Search Functionality:
A built-in search bar allows users to quickly find specific phrases or keywords. As the user types, the app provides instant search results across all phrases and translations in the phrasebook, streamlining navigation within large sets of multilingual content.

Mobile Friendliness:
The layout and UI components are fully responsive, designed to adapt to various screen sizes. Buttons, font sizes, and spacing are optimized for mobile, providing a seamless experience on both smartphones and tablets. Additionally, animations and transitions are performance-optimized to prevent slowdowns on lower-powered devices.

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

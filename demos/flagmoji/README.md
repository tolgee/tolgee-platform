# Flagmoji | Country to Flag Emoji Converter

Flagmoji is a simple app that accepts user inputs and displays the country's flag

## Table of Contents

- [Features](#features)
- [Demo](#demo)
- [Getting Started](#getting-started)
- [Technologies](#technologies)
- [License](#license)
- [Contributing](#contributing)

## Features

- Text Translation

## Demo

You can read the blog article on the project [DEV.to Post](https://dev.to/anni/building-a-country-to-flag-emoji-converter-app-with-vite-typescript-and-tolgee-29e9)

You can view a live demo of Flagmoji [here](https://flagmoji.netlify.app/)

## Screenshots

![App Home Page](https://github.com/user-attachments/assets/ffd118c4-66ab-4711-aad3-0c631a82dda7)
![Selct Language Option](https://github.com/user-attachments/assets/33f7cc6f-3e9a-4c70-8f48-7c6e86f962ae)
![French Translation](https://github.com/user-attachments/assets/5210fa30-3737-4ab4-b26a-47ad70ca030b)
![German Translation](https://github.com/user-attachments/assets/178479c5-77b4-44e8-b418-57980b0782d4)
![App at Work](https://github.com/user-attachments/assets/c8c18aeb-73b4-47b8-bd26-9a66490d5b09)

## Technologies

- [Vite.js](https://vite.dev)
- [Tailwind CSS](https://tailwindcss.com)
- [Tolgee](https://tolgee.io)

## Getting Started

To get started with the project, run the following commands in your terminal:

1. Clone the repository

```bash
git clone https://github.com/AJBrownson/flagmoji.git
cd mini-faq-bot
```

2. Install the dependencies

```bash
npm install
```

3. Create a `.env` file in the root directory and add the following environment variables:

```bash
VITE_APP_TOLGEE_API_URL=https://app.tolgee.io
VITE_APP_TOLGEE_API_KEY=your-api-key
```

> You can get your Tolgee API key by going to [Tolgee](https://app.tolgee.io)

3. Start the development server

```bash
npm run dev
```

4. Open [http://localhost:3000](http://localhost:3000) in your browser to view the app

5. To host on Netlify or Vercel, run the following command:

```bash
npx tolgee login <your-tolgee-api-key>

npx tolgee --project-id <your-project-id-number> pull --path public/i18n
```

Add the following code to your main.tsx file:

```typescript
const tolgee = Tolgee()
  .use(DevTools())
  .use(BackendFetch()) // add this particular line
  .use(FormatSimple())
  .init({
    language: "en",

    // for development
    apiUrl: process.env.VITE_APP_TOLGEE_API_URL,
    apiKey: process.env.VITE_APP_TOLGEE_API_KEY,
  });
```

Now you can go ahead and deploy your app to Netlify or Vercel.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Contributing

Contributions are welcome! Feel free to open an issue or submit a pull request.

If you have any questions, feel free to reach out to me at [Anietie Brownson](https://x.com/TechieAnni).

# FlagPro | Country Flag Quiz

## Overview

FlagPro is an engaging quiz application where users guess the country name based on a displayed flag. The app supports multiple languages through the integration of the Tolgee localization library, allowing for an inclusive experience for users around the globe.

## Table of Contents

- [Features](#features)
- [Demo](#demo)
- [Getting Started](#getting-started)
- [Technologies](#technologies)

## Features

- Flag-based quiz questions
- Support for English, Italian, Japanese, and Korean translations
- Responsive design using Tailwind CSS or NativeWind

## Demo

1. Blog: You can read the blog article on the project [Dev.to Post](https://dev.to/vaishali785/translate-guess-build-a-flag-quiz-with-expo-and-tolgee-4cah)

## Screenshots

<div>
<img src='https://github.com/Vaishali785/FlagPro/blob/main/docs/home.png' alt='Home Page' width='250px' height='500px' /> &nbsp;&nbsp;&nbsp;
<img src='https://github.com/Vaishali785/FlagPro/blob/main/docs/correct_ans.png' alt='Correct Ans Page' width='250px' height='500px'/> &nbsp;&nbsp;&nbsp;
<img src='https://github.com/Vaishali785/FlagPro/blob/main/docs/incorrect_ans.png' alt='Incorrect Ans Page' width='250px' height='500px'/> &nbsp;&nbsp;&nbsp;
  <img src='https://github.com/Vaishali785/FlagPro/blob/main/docs/lang_modal.png' alt='Language Modal' width='250px' height='500px'/> &nbsp;&nbsp;&nbsp;
<img src='https://github.com/Vaishali785/FlagPro/blob/main/docs/translated.png' alt='After Translation Screen' width='250px' height='500px'/> &nbsp;&nbsp;&nbsp;
<img src='https://github.com/Vaishali785/FlagPro/blob/main/docs/result.png' alt='Result Screen' width='250px' height='500px'/> &nbsp;&nbsp;&nbsp;

</div>

## Technologies

- [Expo](https://docs.expo.dev/) for building the app
- [Tailwind CSS](https://tailwindcss.com/) or [NativeWind](https://www.nativewind.dev/) for styling
- [Tolgee](https://tolgee.io/docs) for localization
  For a complete list of dependencies, refer to the package.json file.

## Getting Started

To get started with the project, run the following commands in your terminal:

1. Clone the Repository

```bash
    git clone https://github.com/Vaishali785/FlagPro.git
    cd FlagPro
```

2. Install the dependencies

```bash
npm install
```

3. Create a `.env` file in the root directory and add the following environment variables:

```bash
EXPO_PUBLIC_TOLGEE_API_URL=https://app.tolgee.io
EXPO_PUBLIC_TOLGEE_API_KEY=your-api-key
```

> You can get your Tolgee API key by going to [Tolgee](https://app.tolgee.io)

3. Start the development server

```bash
npx expo start
```

Follow the instructions to run the app on your preferred device or emulator.

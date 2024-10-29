<h1 align="center">🎌 Multilanguage URL Shortener 🎌</h1>
<h2 align="center">Tolgee Demo</h2>

<p align="center">
    <img alt="hero" width="450" src="https://tolgee.io/img/tolgeeLogoDark.svg" />
</p>

> [!NOTE]
>
> Welcome to the **Multilanguage URL Shortener** project! This app demonstrates the usage of **Tolgee** for localization in a Flutter application. With this project, you can shorten URLs and switch between multiple languages seamlessly. 🌐✂️

## 📖 About [Tolgee](https://tolgee.io)

**Tolgee** is a powerful localization platform that allows you to translate your application into any language without modifying your code. It is designed for web applications but can also be used for mobile and desktop apps. 

**Features:**
- **Easy Integration:** No more searching for keys in your source code.
- **Effortless Localization:** No need to edit localization files manually.
- **Simple Data Export:** Easily export data for translators without hassle.

## 🚀 Getting Started

> Follow these steps to set up the project locally:

### Prerequisites

1. **Flutter SDK**: Make sure you have Flutter installed on your machine. [Install Flutter](https://flutter.dev/docs/get-started/install)
2. **Dart SDK**: Included with Flutter.
3. **A Tolgee API Key**: Sign up on [Tolgee](https://tolgee.io) to get your API key.

### Initialization Steps

1. **Clone the Repository:**

   ###### terminal
   ```bash
   git clone https://github.com/ArnavK-09/multilanguage-url-shortner.git
   cd multilanguage-url-shortner
   ```

2. **Install Dependencies:**

   ###### terminal
   ```bash
   flutter pub get
   ```

4. **Set Up Environment Variables:**
   Setup Environment variable for Tolgee API key:
   
   ###### .env
   ```plaintext
   # Required: Your Tolgee API Key
   TOLGEE_API_KEY="<insert>"
   ```
   or 
   ###### terminal
   ```bash
   export TOLGEE_API_KEY="<insert>"
   ```
6. **Fetch Translations from Tolgee:**

   Run the following command to import translations into your project or run the script named [`import_data.sh`](import_data.sh):

   ###### terminal
   ```bash
   curl "https://app.tolgee.io/v2/projects/export?ak=$TOLGEE_API_KEY" --output data.zip && unzip data.zip -d i18n && rm data.zip
   ```

7. **Run the Application:**

   Launch the app in your preferred environment (web or mobile):

   ```bash
   flutter run 
   ```

## 🎨 Features

- **URL Shortening:** Easily shorten any URL using a simple interface.
- **Localization Support:** Switch between multiple languages using Tolgee.
- **User-Friendly Design:** A clean and intuitive user interface.

## 📦 Built With

- **Flutter:** The UI toolkit for building natively compiled applications.
- **Tolgee:** For localization and translation management.

---

<p align="center">
    <strong>If you find this project helpful, please give it a ⭐ on GitHub!</strong>
</p>

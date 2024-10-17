

const { Tolgee, InContextTools, FormatSimple, BackendFetch } =
    window['@tolgee/web'];

const tolgee = Tolgee()
    .use(InContextTools())
    .use(FormatSimple())
    .use(BackendFetch({ prefix: "./i18n/"}))
    .init({
        // ############################################################
        // ## you should never leak your API key                     ##
        // ## remove it in for production publicly accessible site   ##
        // ############################################################
        availableLanguages: ['en', 'hi-IN', 'mr-IN'],
        // apiKey: 'your_api_key',
        // apiUrl: 'https://app.tolgee.io',
        defaultLanguage: 'en',
        observerType: 'text',
        observerOptions: { inputPrefix: '{{', inputSuffix: '}}' },
    });

tolgee.run();

function switchLanguage(language) {
    tolgee.changeLanguage(language);
}



const startButton = document.getElementById('start-button');
const difficultyOptions = document.querySelectorAll('.list-group-item');


startButton.addEventListener('click', () => {
    let flag = 0;
    let activeLis;
    difficultyOptions.forEach(listElement => {
        if (listElement.classList.contains('active')) {
            flag = 1;
            activeLis = listElement;
        }
    })
    if (flag == 1) {
        const numCards = parseInt(activeLis.getAttribute('value'));
        document.getElementById('noCardsInput').value = numCards;
        console.log(numCards)
        document.getElementById('theme').value = body.className;
        document.getElementById('myForm').submit();  // Submit the form
    } else {
        event.preventDefault();  // Prevent form submission if no active list element
        alert('Please select a difficulty level.');
    }
});

difficultyOptions.forEach(option => {
    option.addEventListener('mouseenter', () => {
        option.classList.add('hover');
    });

    option.addEventListener('mouseleave', () => {
        option.classList.remove('hover');
    });

    option.addEventListener('click', () => {
        difficultyOptions.forEach(item => item.classList.remove('active'));
        option.classList.add('active');
    });
});

document.addEventListener('click', (event) => {
    if (!event.target.closest('.list-group-item')) {
        difficultyOptions.forEach(option => {
            option.classList.remove('active');
        });
    }
});


// Different visual modes options:
const lightThemeToggle = document.getElementById('light-theme-toggle');
const darkModeToggle = document.getElementById('dark-mode-toggle');
const purpleThemeToggle = document.getElementById('purple-theme-toggle');
const orangeThemeToggle = document.getElementById('orange-theme-toggle');
const brownThemeToggle = document.getElementById('brown-theme-toggle');
const yellowThemeToggle = document.getElementById('yellow-theme-toggle');
const body = document.body;

function saveTheme(theme) {
    localStorage.setItem('selectedTheme', theme);
    body.classList.remove('purple-theme', 'orange-theme', 'brown-theme', 'yellow-theme');
    body.className = theme;
}

const savedTheme = localStorage.getItem('selectedTheme');
if (savedTheme) {
    body.className = savedTheme;
} else{
    saveTheme("")
}

darkModeToggle.addEventListener('click', () => {
    saveTheme('dark-theme');
});

purpleThemeToggle.addEventListener('click', () => {
    saveTheme('purple-theme');
});

orangeThemeToggle.addEventListener('click', () => {
    saveTheme('orange-theme');
});

brownThemeToggle.addEventListener('click', () => {
    saveTheme('brown-theme');
});

yellowThemeToggle.addEventListener('click', () => {
    saveTheme('yellow-theme');
});

lightThemeToggle.addEventListener('click', () => {
    saveTheme("");
});



const { Tolgee, InContextTools, FormatSimple, BackendFetch } =
    window['@tolgee/web'];

const tolgee = Tolgee()
    .use(InContextTools())
    .use(FormatSimple())
    .use(BackendFetch())
    .init({
        // ############################################################
        // ## you should never leak your API key                     ##
        // ## remove it in for production publicly accessible site   ##
        // ############################################################
        apiKey: 'tgpak_geydkmrul43tgmtjmizgqnlknb3hc3jymfvgg23eozwgcm3qnfxq',
        apiUrl: 'https://app.tolgee.io',
        defaultLanguage: 'en',
        observerType: 'text',
        observerOptions: { inputPrefix: '{{', inputSuffix: '}}' },
    });

tolgee.run();


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

darkModeToggle.addEventListener('click', () => {
    body.classList.remove('purple-theme', 'orange-theme', 'brown-theme', 'yellow-theme');
    body.classList.toggle('dark-mode');
});

purpleThemeToggle.addEventListener('click', () => {
    body.classList.remove('dark-mode', 'orange-theme', 'brown-theme', 'yellow-theme');
    body.classList.toggle('purple-theme');
});

orangeThemeToggle.addEventListener('click', () => {
    body.classList.remove('dark-mode', 'purple-theme', 'brown-theme', 'yellow-theme');
    body.classList.toggle('orange-theme');
});

brownThemeToggle.addEventListener('click', () => {
    body.classList.remove('dark-mode', 'purple-theme', 'orange-theme', 'yellow-theme');
    body.classList.toggle('brown-theme');
});

yellowThemeToggle.addEventListener('click', () => {
    body.classList.remove('dark-mode', 'purple-theme', 'orange-theme', 'brown-theme');
    body.classList.toggle('yellow-theme');
});

lightThemeToggle.addEventListener('click', () => {
    body.classList.remove('dark-mode', 'purple-theme', 'orange-theme', 'brown-theme', 'yellow-theme');
});
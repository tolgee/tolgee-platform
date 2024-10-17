
const revealButton = document.getElementById('reveal-button');
const cardsDeck = document.querySelectorAll('.card-container');

// Reveal button functionality:
let revealsLeft = 2;
revealButton.addEventListener('click', () => {
    if (revealsLeft > 0) {
        cardsDeck.forEach(card => {
            card.classList.add('flip');
        });

        setTimeout(() => {
            cardsDeck.forEach(card => {
                card.classList.remove('flip');
            });
        }, 500);

        revealsLeft--;
    } else {
        alert('You have already revealed the cards twice.');
    }
});
// reveal end

let cardsCount=0;
// Card flip on clicked..
cardsDeck.forEach(container => {
    cardsCount=cardsCount+1;
    container.addEventListener('click', () => flipCard(container));
});


let firstCard = null;
let secondCard = null;
let lockBoard = false;  // To prevent flipping more than 2 cards
let matchedCardsCount = 0;

function flipCard(card) {
    if (lockBoard) return;  // Don't allow flipping when 2 cards are already open
    if (card === firstCard) return;  // Prevent double-clicking the same card
    card.classList.add('flip');
    if (!firstCard) {
        // If this is the first card flipped
        firstCard = card;
    } else {
        // If this is the second card flipped
        secondCard = card;
        // Check for a match
        checkForMatch();
    }
}

function checkForMatch() {
    const firstCardImage = firstCard.querySelector('.card-back').style.backgroundImage;
    const secondCardImage = secondCard.querySelector('.card-back').style.backgroundImage;
    if (firstCardImage === secondCardImage) {
        // Cards match - keep them flipped or hide them
        setTimeout(() => {
            firstCard.style.visibility = 'hidden';
            secondCard.style.visibility = 'hidden';
            matchedCardsCount += 2;
            checkForWin();
            resetBoard();
        }, 500);  // Optional delay before hiding the cards
    } else {
        // Cards don't match - flip them back after a short delay
        lockBoard = true;  // Temporarily lock the board to prevent further clicks
        setTimeout(() => {
            firstCard.classList.remove('flip');
            secondCard.classList.remove('flip');
            resetBoard();
        }, 1000);  // Delay for flipping back
    }
}

function resetBoard() {
    // Reset the first and second cards and unlock the board
    firstCard = null;
    secondCard = null;
    lockBoard = false;
}

function checkForWin() {
    if (matchedCardsCount === cardsCount) {
        setTimeout(() => {
            alert('Congratulations! You won the game!');
        }, 550)
        
        // Redirect to index.html after alert is closed
        setTimeout(() => {
            window.location.href = '../index.html';
        }, 1000);  // Delay before redirecting to allow user to read the alert
    }
}

const images = [
    './static/image1.jpg',
    './static/image2.jpg',
    './static/image3.jpg',
    './static/image4.jpg',
    './static/image5.jpg',
    './static/image6.jpg'
];

// Function to shuffle an array (Fisher-Yates shuffle)
function shuffle(array) {
    for (let i = array.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
}

// Get the number of pairs based on the cardsCount (cardsCount / 2)
const numberOfPairs = cardsCount / 2;

// Select the images to be used (randomly pick numberOfPairs from the images array)
let selectedImages = images.slice(0, numberOfPairs);

// Create pairs of each selected image
let pairedImages = [...selectedImages, ...selectedImages]; // Duplicate each image to create pairs

// Shuffle the paired images
let shuffledImages = shuffle(pairedImages);

// Get all the card-back elements (assuming you have card-back classes on each card)
const cardBacks = document.querySelectorAll('.card-back');

// Loop through each card and assign a random paired image from the shuffled array
cardBacks.forEach((cardBack, index) => {
    if (index < shuffledImages.length) {
        cardBack.style.backgroundImage = `url('${shuffledImages[index]}')`;
        cardBack.style.backgroundSize = 'cover'; // Ensure the image covers the card
        cardBack.style.backgroundPosition = 'center'; // Center the image
    }
});


const body = document.body;
const savedTheme = localStorage.getItem('selectedTheme');
if (savedTheme) {
    body.className = savedTheme;
} else{
    saveTheme("")
}


<?php
$numberOfCards = 0;
if ($_SERVER["REQUEST_METHOD"] == "POST") {
    // Access the submitted data
    $numberOfCards = $_POST['noCards'];
    $themeColor = $_POST['theme'];
}
?>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Mouse Memory Game</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css">
    <link rel="stylesheet" href="GS.css">
</head>
<body class="<?php echo htmlspecialchars($themeColor); ?>">


    <div class="container">
            <div class="coll">
                <h1 class="text-center mb-4">Mouse Memory Game</h1>
            <div class="card-deck">
                <?php
                $cardHTML = '';
                for ($i = 1; $i <= $numberOfCards; $i++) {
                    $cardHTML .= '<div class="card-container">';
                    $cardHTML .= '<div class="card">';
                    $cardHTML .= '<div class="card-front">';
                    $cardHTML .= '<h3>Card '. $i.'</h3>';
                    $cardHTML .= '</div>';
                    $cardHTML .= '<div class="card-back">';
                    $cardHTML .= '</div></div></div>';
                }
                echo $cardHTML;
                ?>
            </div>
                

                <div class="text-center mt-4">
                    <button class="btn btn-primary" id="reveal-button">Reveal Cards</button>
                </div>
            </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
    <script src="GS.js"></script>
</head>
<body>
</html>
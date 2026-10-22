package com.calorielens.app.data.nutrition

import java.util.Locale

/**
 * Bundled, offline nutrition database. Every value is for one typical serving.
 *
 * Recognition labels (e.g. "Chicken tikka masala") are matched against the [FoodEntry.keywords]; the entry
 * with the longest matching keyword wins, so specific dishes beat generic categories.
 */
data class FoodEntry(
    val name: String,
    val keywords: List<String>,
    val nutrition: Nutrition,
)

data class FoodMatch(
    val displayName: String,
    val entry: FoodEntry,
)

object FoodDatabase {

    private fun food(
        name: String, keywords: String,
        kcal: Int, protein: Number, carbs: Number, fat: Number, satFat: Number, fiber: Number, sugar: Number,
        sodium: Int, cholesterol: Int, potassium: Int, calcium: Int, iron: Number, vitA: Int, vitC: Number,
    ) = FoodEntry(
        name = name,
        keywords = keywords.split('|').map { normalize(it) },
        nutrition = Nutrition(
            calories = kcal, protein = protein.toFloat(), carbs = carbs.toFloat(), fat = fat.toFloat(),
            saturatedFat = satFat.toFloat(), fiber = fiber.toFloat(), sugar = sugar.toFloat(), sodiumMg = sodium,
            cholesterolMg = cholesterol, potassiumMg = potassium, calciumMg = calcium, ironMg = iron.toFloat(),
            vitaminAMcg = vitA, vitaminCMg = vitC.toFloat(),
        ),
    )

    val fallback = food("Mixed dish", "mixed dish|dish|meal|food|cuisine|plate|lunch|dinner", 400, 18, 40, 18, 6, 4, 6, 750, 50, 450, 80, 2.5, 120, 8)

    val entries: List<FoodEntry> = listOf(
        // Fruit
        food("Apple", "apple|green apple|red apple", 95, 0.5, 25, 0.3, 0.1, 4.4, 19, 2, 0, 195, 11, 0.2, 5, 8.4),
        food("Banana", "banana|plantain", 105, 1.3, 27, 0.4, 0.1, 3.1, 14, 1, 0, 422, 6, 0.3, 4, 10.3),
        food("Orange", "orange|mandarin|tangerine|clementine|citrus", 62, 1.2, 15.4, 0.2, 0, 3.1, 12, 0, 0, 237, 52, 0.1, 14, 70),
        food("Strawberries", "strawberry|strawberries", 49, 1, 11.7, 0.5, 0, 3, 7.4, 1, 0, 233, 24, 0.6, 2, 89),
        food("Grapes", "grape|grapes", 104, 1.1, 27, 0.2, 0.1, 1.4, 23, 3, 0, 288, 15, 0.5, 5, 4.8),
        food("Watermelon", "watermelon|melon|cantaloupe|honeydew", 92, 1.9, 23, 0.5, 0, 1.2, 19, 3, 0, 341, 22, 0.7, 86, 24),
        food("Mango", "mango", 99, 1.4, 25, 0.6, 0.1, 2.6, 22.5, 2, 0, 277, 18, 0.3, 89, 60),
        food("Pineapple", "pineapple", 82, 0.9, 21.6, 0.2, 0, 2.3, 16, 2, 0, 180, 21, 0.5, 5, 79),
        food("Avocado", "avocado", 160, 2, 8.5, 14.7, 2.1, 6.7, 0.7, 7, 0, 485, 12, 0.6, 7, 10),
        food("Berries", "blueberry|blueberries|raspberry|raspberries|blackberry|blackberries|berries|berry", 84, 1.1, 21, 0.5, 0, 3.6, 15, 1, 0, 114, 9, 0.4, 4, 14),
        food("Fruit salad", "fruit salad|fruit|fruit platter|fruit bowl", 120, 1.5, 30, 0.4, 0, 3.5, 24, 5, 0, 330, 25, 0.5, 40, 55),
        food("Kiwi", "kiwi|kiwifruit", 84, 1.6, 20, 0.7, 0, 4.2, 12, 4, 0, 430, 46, 0.4, 6, 128),
        food("Pear", "pear", 101, 0.6, 27, 0.2, 0, 5.5, 17, 2, 0, 206, 16, 0.3, 2, 7.7),
        food("Peach", "peach|nectarine|apricot|plum", 59, 1.4, 14, 0.4, 0, 2.3, 12.6, 0, 0, 285, 9, 0.4, 24, 9.9),
        food("Cherries", "cherry|cherries", 87, 1.5, 22, 0.3, 0.1, 2.9, 18, 0, 0, 306, 18, 0.5, 4, 9.7),
        food("Pomegranate", "pomegranate", 144, 2.9, 32.5, 2, 0.2, 7, 23.8, 5, 0, 410, 17, 0.5, 0, 17.6),
        food("Papaya", "papaya|guava|dragon fruit|lychee|passion fruit", 62, 0.7, 15.7, 0.4, 0.1, 2.5, 11.3, 12, 0, 264, 29, 0.4, 68, 88),
        food("Dates", "date|dates|dried fruit|raisins|fig|figs", 266, 1.7, 72, 0.2, 0, 6.4, 64, 2, 0, 668, 64, 0.9, 7, 0),

        // Vegetables & salads
        food("Green salad", "salad|green salad|garden salad|side salad|mixed greens", 120, 3, 10, 8, 1.2, 3.5, 4, 250, 0, 420, 60, 1.4, 280, 25),
        food("Caesar salad", "caesar salad|caesar", 360, 10, 15, 29, 6, 2.5, 3, 800, 30, 330, 180, 1.5, 240, 15),
        food("Greek salad", "greek salad|fattoush|horiatiki", 250, 6, 12, 20, 7, 3, 7, 700, 25, 400, 180, 1, 80, 35),
        food("Chicken salad", "chicken salad|tuna salad|egg salad|cobb salad|chef salad", 400, 30, 8, 28, 5, 2, 4, 700, 90, 400, 50, 1.5, 150, 10),
        food("Potato salad", "potato salad|macaroni salad", 350, 6, 28, 24, 4, 3, 4, 800, 100, 500, 30, 1.2, 50, 15),
        food("Caprese", "caprese|mozzarella salad", 300, 18, 8, 22, 10, 1, 5, 450, 50, 350, 450, 0.8, 150, 15),
        food("Coleslaw", "coleslaw|slaw", 190, 1.3, 15, 14.5, 2.2, 2, 12, 270, 10, 200, 40, 0.5, 80, 30),
        food("Broccoli", "broccoli", 55, 3.7, 11, 0.6, 0.1, 5.1, 2.2, 64, 0, 457, 62, 1, 120, 101),
        food("Carrots", "carrot|carrots", 50, 1.1, 12, 0.3, 0, 3.4, 5.8, 85, 0, 390, 40, 0.4, 1020, 7),
        food("Corn", "corn|corn on the cob|sweetcorn|elote", 90, 3.3, 19, 1.4, 0.2, 2, 6.4, 15, 0, 240, 2, 0.5, 10, 6.8),
        food("Baked potato", "potato|baked potato|jacket potato|roast potato|potatoes|hash brown|rosti", 160, 4.3, 37, 0.2, 0, 3.8, 2, 17, 0, 926, 26, 1.9, 2, 17),
        food("Mashed potatoes", "mashed potato|mashed potatoes|mash", 237, 4, 35, 9, 4.5, 3, 3, 700, 25, 680, 50, 0.5, 90, 13),
        food("French fries", "french fries|fries|chips and|potato wedges|wedges|frites", 365, 4, 48, 17, 2.3, 4.4, 0.3, 246, 0, 668, 20, 0.9, 0, 4.7),
        food("Sweet potato", "sweet potato|yam", 112, 2, 26, 0.1, 0, 3.9, 5.4, 72, 0, 438, 39, 0.8, 961, 3),
        food("Mushrooms", "mushroom|mushrooms", 40, 3, 5, 1.5, 0.2, 1.5, 2, 10, 0, 320, 4, 0.4, 0, 2),
        food("Spinach", "spinach|kale|greens|saag", 41, 5.3, 6.8, 0.5, 0.1, 4.3, 0.8, 126, 0, 839, 245, 6.4, 943, 17.6),
        food("Vegetables", "vegetable|vegetables|veggie|veggies|asparagus|zucchini|cauliflower|green beans|peas|cabbage|eggplant|okra|brussels sprouts|cucumber|tomato|pepper|beet", 80, 3.5, 14, 1.5, 0.2, 5, 5, 150, 0, 450, 60, 1.2, 300, 40),
        food("Vegetable stir fry", "stir fry|stir fried|vegetable stir fry|chop suey", 180, 5, 18, 10, 1.5, 4.5, 8, 650, 0, 500, 60, 1.5, 350, 45),
        food("Edamame", "edamame|soybean", 190, 17, 14, 8, 1, 8, 3, 9, 0, 670, 100, 3.5, 15, 10),
        food("Beans", "beans|baked beans|refried beans|black beans|bean", 260, 12, 50, 1, 0.3, 10, 15, 870, 0, 700, 120, 3, 10, 5),
        food("Kimchi", "kimchi|sauerkraut|pickles|pickle", 25, 1.5, 4, 0.5, 0, 2, 1.5, 600, 0, 200, 40, 0.5, 60, 8),

        // Soups
        food("Soup", "soup|broth|bisque|consomme|gazpacho|minestrone", 150, 7, 18, 5, 1.5, 2.5, 4, 850, 10, 400, 40, 1.2, 150, 8),
        food("Tomato soup", "tomato soup", 160, 3.5, 22, 6, 2, 3, 13, 950, 5, 560, 60, 1.5, 90, 25),
        food("Chicken soup", "chicken soup|chicken noodle soup", 150, 10, 15, 5, 1.5, 1, 1.5, 900, 25, 300, 25, 1, 120, 1),
        food("Clam chowder", "chowder|clam chowder|cream soup", 330, 12, 25, 20, 10, 1.5, 4, 1100, 60, 450, 150, 2, 100, 5),
        food("Miso soup", "miso|miso soup", 60, 4, 7, 2, 0.3, 1.5, 2, 900, 0, 200, 40, 0.8, 15, 0),
        food("Lentil dal", "dal|dhal|daal|lentil|lentils|lentil soup|sambar|rasam", 230, 13, 34, 5, 1, 11, 3, 550, 0, 600, 40, 3.6, 40, 4),
        food("Ramen", "ramen|tonkotsu", 450, 18, 60, 16, 5, 3, 4, 1800, 50, 350, 40, 3, 40, 3),
        food("Pho", "pho|bun bo hue|noodle soup|laksa", 420, 28, 55, 9, 3, 2, 4, 1500, 60, 600, 60, 4, 60, 10),

        // Pasta & noodles
        food("Pasta", "pasta|spaghetti|penne|fettuccine|linguine|rigatoni|fusilli|ravioli|tortellini|gnocchi|macaroni|noodle pasta", 420, 15, 72, 8, 2, 5, 9, 600, 10, 600, 60, 3.3, 70, 15),
        food("Spaghetti bolognese", "bolognese|spaghetti bolognese|spag bol|meat sauce|spaghetti and meatballs|ragu", 550, 28, 65, 18, 7, 5, 10, 800, 60, 750, 80, 4.5, 100, 15),
        food("Lasagna", "lasagna|lasagne|baked ziti|cannelloni", 520, 28, 40, 27, 13, 3, 8, 980, 80, 600, 350, 3, 150, 10),
        food("Mac and cheese", "mac and cheese|macaroni and cheese|mac n cheese", 500, 18, 50, 25, 13, 2, 6, 900, 60, 250, 350, 2, 180, 0),
        food("Carbonara", "carbonara", 600, 25, 65, 26, 11, 3, 3, 900, 180, 300, 200, 2.5, 100, 0),
        food("Fettuccine alfredo", "alfredo|cream pasta", 700, 22, 70, 37, 20, 3, 4, 1000, 140, 300, 350, 2.5, 250, 1),
        food("Pesto pasta", "pesto", 560, 15, 65, 27, 5, 4, 3, 500, 15, 250, 200, 3, 80, 5),
        food("Noodles", "noodle|noodles|soba|vermicelli|rice noodles", 380, 12, 55, 12, 2.5, 3, 5, 900, 30, 250, 30, 2.5, 40, 5),
        food("Chow mein", "chow mein|lo mein|hakka noodles|yakisoba|mee goreng|chowmein", 500, 18, 65, 18, 3, 4, 8, 1500, 40, 350, 50, 3, 150, 10),
        food("Pad thai", "pad thai|pad see ew", 600, 24, 80, 22, 4, 4, 20, 1400, 120, 400, 80, 3, 100, 10),
        food("Udon", "udon", 400, 12, 70, 6, 1, 3, 5, 1600, 10, 200, 40, 2, 10, 1),

        // Pizza, burgers, sandwiches
        food("Pizza", "pizza|margherita|calzone|flatbread pizza", 570, 24, 71, 21, 9, 4, 7, 1280, 45, 340, 400, 5, 140, 4),
        food("Pepperoni pizza", "pepperoni|pepperoni pizza|meat pizza", 620, 26, 70, 26, 11, 4, 7, 1450, 60, 350, 380, 5, 130, 3),
        food("Burger", "burger|hamburger|beef burger|veggie burger|slider", 540, 30, 40, 29, 11, 2, 8, 950, 90, 450, 150, 4.5, 30, 2),
        food("Cheeseburger", "cheeseburger|cheese burger|double burger", 600, 33, 42, 33, 15, 2, 9, 1150, 105, 450, 280, 4.5, 90, 2),
        food("Hot dog", "hot dog|hotdog|corn dog|frankfurter", 300, 11, 24, 18, 6.5, 1, 4, 800, 35, 170, 60, 2, 0, 0),
        food("Sandwich", "sandwich|sub|panini|hoagie|blt|toastie|banh mi|po boy|sandwiches", 400, 20, 42, 16, 5, 4, 6, 950, 40, 350, 150, 3, 60, 6),
        food("Club sandwich", "club sandwich|reuben|philly cheesesteak|cheesesteak", 550, 32, 40, 28, 8, 3, 6, 1300, 80, 450, 150, 3, 60, 8),
        food("Grilled cheese", "grilled cheese|croque monsieur", 440, 16, 32, 27, 15, 2, 4, 900, 70, 150, 400, 2, 220, 0),
        food("Wrap", "wrap|burrito|chimichanga", 600, 26, 70, 22, 9, 9, 4, 1300, 55, 600, 250, 5, 100, 8),
        food("Tacos", "taco|tacos|al pastor|carnitas|birria", 400, 20, 34, 20, 7, 5, 3, 700, 55, 400, 180, 2.5, 80, 6),
        food("Quesadilla", "quesadilla", 520, 24, 40, 29, 15, 3, 3, 1100, 75, 250, 450, 3, 180, 3),
        food("Nachos", "nachos|tortilla chips", 550, 14, 55, 30, 11, 6, 4, 900, 40, 300, 280, 2, 90, 3),
        food("Enchiladas", "enchilada|enchiladas|tamale|tamales", 550, 25, 45, 30, 14, 6, 5, 1200, 80, 500, 350, 3, 180, 8),
        food("Fajitas", "fajita|fajitas", 450, 30, 35, 20, 6, 5, 6, 1000, 80, 600, 100, 3, 150, 40),
        food("Burrito bowl", "burrito bowl|taco salad", 650, 35, 70, 24, 8, 13, 5, 1400, 80, 900, 250, 4, 150, 20),
        food("Guacamole", "guacamole", 180, 2.3, 10, 16, 2.3, 7, 1, 350, 0, 550, 15, 0.6, 10, 12),
        food("Salsa", "salsa|pico de gallo", 35, 1.5, 7, 0.2, 0, 2, 4, 450, 0, 250, 25, 0.5, 40, 15),
        food("Empanada", "empanada|empanadas|patty|pasty", 350, 12, 30, 20, 7, 2, 3, 500, 40, 250, 40, 2, 20, 2),

        // Chicken & meat
        food("Chicken", "chicken|roast chicken|rotisserie chicken|chicken breast|chicken thigh|poultry", 335, 38, 2, 19, 5, 0, 0, 450, 140, 370, 20, 1.8, 60, 1),
        food("Grilled chicken", "grilled chicken|chicken breast grilled|chicken skewer|yakitori|satay", 285, 53, 0, 6.2, 1.7, 0, 0, 120, 145, 440, 15, 1.6, 15, 0),
        food("Fried chicken", "fried chicken|chicken fried|karaage|chicken tenders|chicken strips|popcorn chicken|chicken katsu", 490, 38, 16, 30, 8, 1, 0, 1100, 165, 400, 40, 2, 40, 0),
        food("Chicken wings", "wings|chicken wings|buffalo wings|hot wings", 480, 40, 6, 33, 9, 0, 3, 1400, 190, 300, 40, 2, 120, 1),
        food("Chicken nuggets", "nuggets|chicken nuggets", 280, 15, 17, 17, 3, 1, 0, 540, 45, 250, 15, 1, 0, 0),
        food("Turkey", "turkey|roast turkey", 300, 45, 0, 12, 3.5, 0, 0, 400, 110, 450, 30, 2, 10, 0),
        food("Steak", "steak|ribeye|sirloin|filet mignon|t bone|tenderloin|roast beef|beef steak", 600, 60, 0, 40, 16, 0, 0, 130, 180, 800, 30, 5, 0, 0),
        food("Beef", "beef|brisket|meat|pot roast|beef stew", 450, 40, 3, 30, 12, 0, 1, 400, 130, 600, 30, 4, 0, 1),
        food("Pork", "pork|pork chop|pulled pork|pork belly|ham|char siu|lechon|tonkatsu", 370, 40, 0, 22, 8, 0, 0, 120, 120, 600, 30, 1, 5, 1),
        food("Ribs", "ribs|spare ribs|baby back ribs|bbq ribs", 650, 45, 15, 45, 17, 0, 13, 1100, 170, 550, 60, 3, 10, 1),
        food("Bacon", "bacon", 130, 9, 0.5, 10, 3.3, 0, 0, 550, 30, 170, 5, 0.3, 4, 0),
        food("Sausage", "sausage|sausages|bratwurst|chorizo|salami|kielbasa", 300, 12, 2, 27, 9, 0, 1, 800, 60, 250, 15, 1, 0, 0),
        food("Meatballs", "meatball|meatballs|kofta|kofte|polpette", 400, 25, 15, 26, 10, 1, 4, 800, 100, 400, 80, 3, 50, 4),
        food("Lamb", "lamb|lamb chop|mutton chops|rack of lamb", 450, 35, 0, 33, 14, 0, 0, 100, 130, 450, 25, 3, 0, 0),
        food("Kebab", "kebab|kabob|seekh|shish|tikka kebab|chapli|shami", 350, 25, 6, 25, 9, 1, 1, 800, 90, 400, 40, 3, 40, 2),
        food("Shawarma", "shawarma|gyro|doner|kebab wrap", 600, 35, 50, 28, 7, 4, 5, 1300, 100, 550, 100, 4, 80, 8),
        food("Korean BBQ", "bulgogi|galbi|korean bbq|samgyeopsal", 450, 30, 20, 28, 10, 1, 15, 1000, 90, 500, 30, 3, 10, 2),
        food("Teriyaki", "teriyaki", 480, 35, 40, 18, 5, 1, 25, 1500, 110, 450, 30, 2, 30, 3),
        food("Orange chicken", "orange chicken|sweet and sour|general tso|sesame chicken|honey chicken", 600, 30, 50, 30, 5, 3, 25, 1300, 90, 500, 50, 2.5, 60, 15),
        food("Kung pao chicken", "kung pao|szechuan|mapo|dongpo", 480, 32, 22, 30, 5, 3, 8, 1200, 80, 600, 50, 2.5, 80, 15),
        food("Stew", "stew|goulash|casserole|ragout|hotpot|hot pot|bourguignon", 400, 30, 25, 20, 8, 4, 6, 1000, 90, 800, 50, 4, 300, 15),
        food("Chili", "chili|chilli|chili con carne", 350, 22, 30, 15, 6, 9, 7, 1000, 60, 800, 80, 4, 100, 10),
        food("Pot pie", "pot pie|meat pie|shepherd s pie|cottage pie|pie meat", 600, 22, 45, 36, 14, 3, 6, 1200, 70, 400, 60, 3, 600, 5),

        // Seafood
        food("Fish", "fish|grilled fish|cod|tilapia|sea bass|halibut|trout|mackerel|sardine|seafood|snapper", 250, 35, 0, 11, 2, 0, 0, 120, 90, 600, 20, 0.6, 50, 2),
        food("Salmon", "salmon|smoked salmon|lox", 370, 40, 0, 22, 4.5, 0, 0, 110, 110, 700, 20, 0.8, 60, 0),
        food("Tuna", "tuna|ahi", 200, 40, 0, 3, 1, 0, 0, 400, 60, 400, 20, 1.5, 40, 0),
        food("Fish and chips", "fish and chips|fried fish|fish fry|fish fingers|fish sticks", 840, 32, 85, 41, 7, 6, 1, 1100, 80, 1100, 60, 3, 30, 10),
        food("Shrimp", "shrimp|prawn|prawns|scampi|shrimps", 200, 24, 2, 10, 2, 0, 0, 800, 250, 250, 90, 0.6, 90, 2),
        food("Tempura", "tempura|fried shrimp|coconut shrimp|fritto misto|calamari|fried squid", 450, 20, 35, 25, 4, 2, 2, 900, 150, 250, 60, 2, 40, 2),
        food("Crab", "crab|crab cake|crab legs|king crab", 180, 30, 0, 3, 0.5, 0, 0, 800, 100, 400, 100, 1, 10, 8),
        food("Lobster", "lobster|lobster roll|langoustine", 200, 35, 2, 5, 1, 0, 0, 900, 200, 400, 100, 0.5, 10, 0),
        food("Shellfish", "mussels|oysters|clams|scallops|shellfish|oyster|clam|scallop|octopus|squid", 220, 30, 8, 6, 1.5, 0, 0, 600, 60, 700, 50, 7, 250, 15),
        food("Sushi", "sushi|maki|nigiri|california roll|roll sushi|temaki|uramaki", 350, 14, 62, 4, 1, 2, 9, 800, 15, 300, 30, 1, 80, 4),
        food("Sashimi", "sashimi|poke|crudo|ceviche|tartare", 200, 35, 0, 6, 1.5, 0, 0, 90, 60, 550, 15, 1, 60, 2),
        food("Poke bowl", "poke bowl|chirashi|donburi|sushi bowl", 550, 30, 65, 18, 3, 6, 10, 1100, 60, 600, 50, 2.5, 100, 10),

        // Rice & grains
        food("Rice", "rice|white rice|steamed rice|plain rice|jasmine rice|basmati", 205, 4.3, 45, 0.4, 0.1, 0.6, 0, 2, 0, 55, 16, 1.9, 0, 0),
        food("Brown rice", "brown rice|wild rice", 218, 4.5, 46, 1.6, 0.3, 3.5, 0.7, 10, 0, 154, 20, 1, 0, 0),
        food("Fried rice", "fried rice|nasi goreng|egg fried rice|yangzhou|khao pad", 470, 14, 70, 14, 3, 3, 3, 1200, 90, 250, 40, 2.5, 150, 5),
        food("Risotto", "risotto", 450, 12, 60, 17, 8, 2, 2, 900, 35, 300, 150, 1.5, 100, 3),
        food("Paella", "paella|jambalaya|jollof", 500, 28, 60, 15, 3, 3, 3, 1100, 120, 500, 70, 3, 100, 12),
        food("Bibimbap", "bibimbap|rice bowl|gimbap|kimbap", 550, 25, 75, 16, 4, 5, 8, 900, 200, 600, 80, 4, 400, 15),
        food("Onigiri", "onigiri|rice ball|musubi", 200, 4, 42, 1, 0.2, 1, 1, 500, 0, 80, 10, 0.5, 0, 0),
        food("Quinoa bowl", "quinoa|buddha bowl|grain bowl|salad bowl", 450, 15, 60, 16, 2, 9, 6, 600, 0, 700, 70, 4, 150, 20),
        food("Couscous", "couscous|bulgur|freekeh", 220, 6, 45, 1, 0.2, 3, 0, 250, 0, 100, 15, 0.7, 0, 0),
        food("Katsu curry", "katsu curry|japanese curry|curry rice", 650, 32, 60, 30, 8, 3, 8, 1100, 120, 450, 40, 2.5, 30, 5),

        // South Asian
        food("Biryani", "biryani|biriyani|briyani|biriani|dum biryani", 600, 26, 78, 20, 7, 3, 4, 1100, 80, 450, 80, 3, 90, 4),
        food("Pulao", "pulao|pilaf|pilau|plov|pulav", 400, 8, 68, 10, 3, 2, 2, 700, 10, 200, 30, 1.5, 40, 3),
        food("Curry", "curry|vegetable curry|masala|vindaloo|madras|jalfrezi", 350, 9, 35, 19, 7, 7, 7, 800, 5, 600, 80, 3, 200, 20),
        food("Chicken curry", "chicken curry|karahi|chicken karahi|chicken masala|chicken korma|handi", 450, 32, 14, 29, 9, 3, 5, 900, 110, 600, 70, 2.5, 150, 10),
        food("Butter chicken", "butter chicken|murgh makhani|tikka masala|chicken tikka masala|makhani", 490, 30, 14, 35, 15, 2, 7, 950, 130, 550, 110, 2.4, 250, 6),
        food("Chicken tikka", "chicken tikka|tikka|chicken 65|malai tikka", 300, 40, 6, 13, 4, 1, 3, 800, 140, 500, 60, 2, 120, 5),
        food("Tandoori chicken", "tandoori|tandoori chicken", 330, 45, 6, 14, 4, 1, 3, 900, 160, 550, 60, 2, 120, 5),
        food("Mutton curry", "korma|rogan josh|nihari|haleem|mutton|lamb curry|goat curry|keema|qorma|paya|mutton karahi", 520, 32, 12, 38, 14, 2, 5, 900, 110, 600, 70, 3.5, 120, 6),
        food("Fish curry", "fish curry|machli|fish masala", 380, 30, 12, 24, 10, 2, 4, 900, 80, 700, 80, 2, 150, 10),
        food("Prawn curry", "prawn curry|shrimp curry|prawn masala", 350, 28, 12, 21, 10, 2, 4, 1000, 200, 450, 100, 2, 150, 10),
        food("Chana masala", "chana|chole|chickpea curry|chickpeas|chana masala", 300, 12, 40, 10, 1.5, 11, 7, 750, 0, 550, 90, 4, 60, 10),
        food("Rajma", "rajma|kidney beans|lobia", 330, 15, 45, 10, 2, 12, 4, 700, 0, 800, 90, 4.5, 50, 6),
        food("Paneer curry", "paneer|palak paneer|shahi paneer|kadai paneer|matar paneer|paneer tikka", 400, 18, 14, 30, 15, 3, 6, 800, 70, 350, 450, 1.5, 250, 15),
        food("Vegetable sabzi", "aloo|gobi|aloo gobi|sabzi|subzi|bhindi|baingan|bharta|aloo matar|dum aloo|mixed veg", 220, 5, 25, 12, 2, 6, 6, 600, 0, 700, 50, 2, 80, 50),
        food("Samosa", "samosa|samosas|kachori", 520, 8, 52, 32, 6, 4, 2, 700, 5, 350, 30, 2, 30, 6),
        food("Pakora", "pakora|pakoda|bhaji|onion bhaji|bhajji|fritter|fritters", 350, 8, 30, 22, 3, 4, 2, 600, 0, 350, 40, 2, 50, 8),
        food("Naan", "naan|nan|garlic naan|kulcha", 260, 9, 45, 5, 1.5, 2, 3, 450, 5, 100, 70, 3, 0, 0),
        food("Roti", "roti|chapati|chapatti|phulka|tortilla|flatbread|pita", 120, 3.5, 20, 3.5, 0.5, 3, 0.5, 200, 0, 100, 10, 1.5, 0, 0),
        food("Paratha", "paratha|parotta|aloo paratha|lachha", 300, 6, 40, 13, 5, 4, 1, 400, 15, 150, 20, 2, 50, 0),
        food("Chole bhature", "chole bhature|bhatura|bhature|puri|poori", 650, 16, 75, 32, 6, 10, 5, 1000, 0, 600, 90, 5, 50, 8),
        food("Pav bhaji", "pav bhaji|vada pav|misal pav|pav", 550, 12, 70, 24, 10, 8, 10, 1200, 30, 700, 80, 3.5, 250, 40),
        food("Chaat", "chaat|bhel|bhel puri|pani puri|golgappa|dahi puri|sev puri|papdi chaat|dahi vada", 300, 8, 45, 10, 2, 6, 8, 700, 0, 400, 50, 3, 60, 12),
        food("Dosa", "dosa|masala dosa|uttapam|appam", 330, 8, 50, 10, 3, 3, 1, 600, 5, 250, 40, 1.5, 30, 2),
        food("Idli", "idli|idly", 180, 6, 36, 1, 0.2, 2, 0, 450, 0, 100, 15, 0.8, 0, 0),
        food("Vada", "vada|medu vada|wada", 300, 8, 30, 17, 2, 5, 1, 500, 0, 250, 40, 2, 10, 2),
        food("Poha", "poha|upma|sooji", 250, 6, 40, 7, 1, 3, 2, 500, 0, 200, 20, 3, 30, 5),
        food("Khichdi", "khichdi|khichri|kitchari", 300, 10, 50, 6, 2, 5, 1, 500, 5, 350, 40, 2.5, 30, 3),
        food("Halwa", "halwa|halva|gajar halwa|sheera|kesari", 400, 5, 55, 18, 10, 2, 40, 100, 40, 200, 80, 1, 100, 1),
        food("Gulab jamun", "gulab jamun|rasgulla|rasmalai|rasogolla|sandesh|barfi|ladoo|laddu|mithai|peda", 300, 4, 45, 12, 6, 0.5, 38, 80, 15, 100, 80, 0.5, 40, 0),
        food("Jalebi", "jalebi|imarti", 450, 3, 70, 18, 4, 1, 50, 30, 0, 40, 15, 1, 0, 0),
        food("Kheer", "kheer|payasam|firni|rice pudding|sheer khurma", 290, 8, 45, 8, 5, 0.5, 35, 100, 30, 350, 250, 0.5, 80, 1),
        food("Lassi", "lassi|mango lassi|buttermilk|chaas", 260, 9, 40, 7, 4.5, 0, 38, 120, 25, 450, 300, 0.2, 60, 2),
        food("Masala chai", "chai|masala chai|tea|milk tea|karak", 120, 4, 18, 4, 2.5, 0, 16, 60, 12, 200, 140, 0.2, 40, 0),

        // Middle Eastern & Mediterranean
        food("Falafel", "falafel", 330, 13, 32, 18, 2.4, 5, 1, 600, 0, 580, 55, 3.4, 1, 1.6),
        food("Hummus", "hummus|houmous|baba ganoush|mutabal|dip", 210, 8, 18, 12, 1.5, 7, 0.5, 480, 0, 280, 60, 3, 0, 0),
        food("Tabbouleh", "tabbouleh|tabouli", 180, 3, 20, 11, 1.5, 4, 2, 400, 0, 300, 40, 1.5, 100, 30),
        food("Shakshuka", "shakshuka|menemen|huevos rancheros", 330, 18, 16, 22, 6, 4, 9, 900, 370, 700, 120, 4, 250, 50),
        food("Moussaka", "moussaka|eggplant parmesan|parmigiana", 450, 22, 22, 30, 12, 5, 8, 800, 110, 700, 200, 2.5, 200, 10),
        food("Bruschetta", "bruschetta|crostini", 200, 5, 25, 9, 1.5, 2, 3, 350, 0, 250, 40, 1.5, 40, 10),

        // Asian
        food("Dumplings", "dumpling|dumplings|dim sum|gyoza|momo|momos|bao|baozi|wonton|wontons|siu mai|har gow|xiaolongbao|mandu|potsticker", 400, 18, 45, 16, 5, 2, 3, 900, 50, 350, 40, 2.5, 50, 4),
        food("Spring rolls", "spring roll|spring rolls|egg roll|egg rolls|summer roll|lumpia", 450, 10, 45, 25, 5, 3, 5, 900, 15, 250, 40, 2, 150, 6),
        food("Thai curry", "green curry|red curry|thai curry|massaman|panang|yellow curry|tom yum|tom kha", 500, 25, 15, 38, 26, 3, 6, 1100, 70, 600, 60, 3, 100, 10),
        food("Tofu", "tofu|bean curd|fried tofu|agedashi", 250, 18, 10, 15, 2.5, 2, 3, 600, 0, 350, 350, 3, 10, 3),
        food("Katsu", "katsu|tonkatsu|schnitzel|milanesa|cutlet", 650, 32, 60, 30, 8, 3, 8, 1100, 120, 450, 40, 2.5, 30, 5),
        food("Mochi", "mochi|rice cake|tteokbokki|daifuku", 200, 3, 45, 0.5, 0, 1, 15, 20, 0, 20, 5, 0.3, 0, 0),

        // Breakfast
        food("Eggs", "egg|eggs|boiled egg|fried egg|poached egg|deviled eggs|sunny side up|egg benedict|eggs benedict", 155, 12.6, 1.1, 10.6, 3.3, 0, 1.1, 124, 373, 126, 50, 1.2, 149, 0),
        food("Omelette", "omelette|omelet|frittata|quiche|tortilla espanola|spanish omelette", 330, 22, 4, 25, 8, 1, 2, 700, 560, 350, 150, 2.5, 350, 8),
        food("Scrambled eggs", "scrambled eggs|scrambled egg|egg bhurji|anda bhurji", 300, 20, 3, 23, 9, 0, 2, 600, 560, 250, 120, 2, 350, 0),
        food("Pancakes", "pancake|pancakes|hotcakes|flapjack", 520, 13, 75, 18, 6, 2, 22, 1000, 95, 300, 300, 3.5, 100, 0),
        food("Waffles", "waffle|waffles", 450, 11, 55, 21, 6, 2, 15, 800, 90, 250, 250, 3.5, 150, 0),
        food("French toast", "french toast", 450, 15, 50, 20, 6, 2, 15, 700, 250, 300, 200, 3, 200, 0),
        food("Crepe", "crepe|crepes|crêpe|galette", 300, 8, 40, 12, 6, 1, 15, 300, 90, 200, 120, 1.5, 100, 1),
        food("Cereal", "cereal|cornflakes|muesli|corn flakes", 300, 10, 55, 5, 1.5, 4, 22, 350, 10, 450, 250, 10, 400, 10),
        food("Oatmeal", "oatmeal|porridge|oats|overnight oats", 250, 8, 45, 5, 1, 6, 12, 150, 5, 350, 150, 3, 10, 1),
        food("Yogurt", "yogurt|yoghurt|parfait|greek yogurt|acai bowl|acai|raita|curd|dahi", 200, 10, 30, 5, 3, 2, 22, 100, 15, 450, 300, 0.5, 50, 8),
        food("Granola", "granola|granola bar|protein bar|energy bar", 300, 8, 40, 13, 2, 5, 15, 20, 0, 300, 40, 2, 0, 1),
        food("Toast", "toast|bread|baguette|loaf|bun|roll|sourdough|garlic bread|focaccia|ciabatta|brioche", 160, 6, 30, 2, 0.4, 2, 3, 300, 0, 80, 60, 2, 0, 0),
        food("Avocado toast", "avocado toast", 350, 9, 35, 20, 3, 9, 3, 450, 0, 600, 60, 2.5, 15, 10),
        food("Bagel", "bagel|bagels", 450, 13, 60, 17, 9, 2, 8, 650, 50, 180, 100, 4, 180, 0),
        food("Croissant", "croissant|pain au chocolat|danish|danish pastry", 400, 8, 45, 21, 12, 2.5, 11, 450, 65, 120, 40, 2, 300, 0),
        food("Pretzel", "pretzel|pretzels", 380, 10, 80, 3, 0.5, 3, 3, 1200, 0, 120, 25, 4.5, 0, 0),
        food("Full breakfast", "full breakfast|english breakfast|fry up|breakfast plate|breakfast", 850, 40, 50, 55, 18, 6, 8, 2000, 450, 900, 120, 6, 200, 10),

        // Desserts & snacks
        food("Cake", "cake|sponge cake|pound cake|red velvet|carrot cake|fruit cake|birthday cake|layer cake|dessert|sweet|sweets", 420, 5, 58, 19, 6, 1.5, 40, 350, 60, 150, 60, 1.5, 80, 0),
        food("Chocolate cake", "chocolate cake|black forest|lava cake|fudge cake", 450, 5, 60, 22, 9, 2.5, 45, 380, 60, 250, 50, 2.5, 40, 0),
        food("Cheesecake", "cheesecake", 400, 7, 32, 28, 15, 0.5, 25, 350, 100, 150, 80, 0.8, 250, 1),
        food("Cupcake", "cupcake|cupcakes|muffin|muffins", 420, 6, 55, 20, 4, 1.5, 30, 400, 50, 150, 60, 1.5, 50, 1),
        food("Donut", "donut|doughnut|donuts|doughnuts|beignet|cruller", 300, 4, 35, 16, 7, 1, 16, 300, 20, 80, 40, 1.5, 10, 0),
        food("Cookies", "cookie|cookies|biscuit|biscuits|shortbread|biscotti", 220, 2.5, 30, 10, 5, 1, 18, 150, 15, 90, 15, 1, 30, 0),
        food("Brownie", "brownie|brownies|blondie", 400, 5, 50, 20, 7, 2, 35, 200, 50, 150, 30, 2, 40, 0),
        food("Pie", "pie|apple pie|tart|cobbler|crumble|pecan pie|pumpkin pie|strudel", 410, 3.7, 58, 19, 5, 2, 26, 330, 0, 120, 20, 1.7, 40, 3),
        food("Ice cream", "ice cream|gelato|sundae|frozen yogurt|sorbet|kulfi|popsicle", 270, 4.6, 32, 14, 9, 1, 28, 105, 58, 262, 168, 0.2, 155, 1),
        food("Chocolate", "chocolate|chocolate bar|truffle|truffles|candy|fudge|praline", 230, 3, 25, 13, 8, 3, 22, 30, 8, 250, 50, 2.5, 10, 0),
        food("Pudding", "pudding|custard|flan|creme brulee|panna cotta|mousse|trifle", 250, 6, 35, 9, 5, 0, 28, 250, 80, 300, 200, 0.5, 100, 1),
        food("Tiramisu", "tiramisu", 450, 7, 40, 28, 16, 1, 28, 100, 170, 180, 90, 1, 200, 0),
        food("Pastry", "pastry|macaron|macarons|eclair|cannoli|profiterole|cream puff|mille feuille|scone", 350, 5, 45, 17, 8, 1, 28, 200, 40, 100, 40, 1, 100, 0),
        food("Baklava", "baklava|kunafa|knafeh|basbousa", 330, 5, 30, 22, 8, 2, 18, 250, 30, 150, 30, 1.5, 50, 0),
        food("Churros", "churro|churros|funnel cake", 400, 4, 45, 22, 4, 2, 15, 300, 10, 100, 10, 1.5, 0, 0),
        food("Jelly", "jelly|jello|gelatin", 80, 1.6, 19, 0, 0, 0, 18, 90, 0, 1, 3, 0, 0, 0),
        food("Popcorn", "popcorn", 150, 3, 18, 8, 1, 3, 0, 250, 0, 80, 3, 0.8, 5, 0),
        food("Potato chips", "potato chips|crisps|chips|snack", 300, 3.5, 30, 19, 2, 2.5, 0.3, 300, 0, 700, 12, 0.9, 0, 10),
        food("Nuts", "nuts|almonds|cashews|peanuts|walnuts|pistachios|trail mix|mixed nuts", 270, 8, 10, 24, 3, 3, 2, 5, 0, 250, 40, 1.5, 0, 0),
        food("Cheese", "cheese|cheese plate|cheese board|charcuterie|brie|cheddar", 400, 24, 4, 32, 20, 0, 1, 700, 100, 100, 700, 0.5, 300, 0),
        food("Fried snack", "fried|deep fried|fried snack|croquette|croquettes|arancini|onion rings|mozzarella sticks|tater tots", 400, 10, 35, 24, 4, 2, 2, 600, 30, 300, 30, 2, 20, 2),
        food("Poutine", "poutine|loaded fries|cheese fries|chili fries", 740, 25, 70, 40, 17, 6, 1, 1400, 70, 1100, 400, 2, 150, 10),
        food("Pierogi", "pierogi|pierogies|varenyky|pelmeni", 380, 10, 50, 15, 6, 2, 3, 600, 40, 300, 50, 2, 40, 3),

        // Drinks
        food("Latte", "latte|cappuccino|flat white|mocha|macchiato|frappuccino|iced coffee", 190, 10, 15, 10, 5, 0, 14, 140, 30, 400, 350, 0.3, 120, 0),
        food("Coffee", "coffee|espresso|americano|black coffee|cold brew", 5, 0.3, 0, 0, 0, 0, 0, 5, 0, 115, 5, 0, 0, 0),
        food("Hot chocolate", "hot chocolate|cocoa|hot cocoa", 300, 10, 40, 12, 7, 3, 35, 200, 30, 450, 300, 1.5, 80, 0),
        food("Juice", "juice|orange juice|apple juice|lemonade|fresh juice", 112, 1.7, 26, 0.5, 0, 0.5, 21, 2, 0, 496, 27, 0.5, 25, 124),
        food("Smoothie", "smoothie|smoothies|shake protein|protein shake", 250, 6, 50, 3, 1, 5, 38, 60, 5, 700, 200, 1, 100, 60),
        food("Milkshake", "milkshake|shake|malt|thick shake|falooda", 550, 12, 80, 20, 12, 1, 70, 300, 80, 600, 400, 1, 200, 2),
        food("Bubble tea", "bubble tea|boba|boba tea|pearl milk tea|thai tea", 350, 2, 70, 8, 6, 1, 50, 50, 5, 150, 80, 0.5, 0, 0),
        food("Soft drink", "soda|cola|soft drink|pop|coke|sprite|fizzy drink|energy drink", 140, 0, 39, 0, 0, 0, 39, 45, 0, 0, 0, 0, 0, 0),
        food("Milk", "milk|glass of milk", 150, 8, 12, 8, 4.6, 0, 12, 105, 24, 322, 276, 0.1, 112, 0),
        food("Beer", "beer|lager|ale|stout", 150, 1.6, 13, 0, 0, 0, 0, 14, 0, 96, 14, 0, 0, 0),
        food("Wine", "wine|red wine|white wine|champagne|prosecco", 125, 0.1, 4, 0, 0, 0, 1, 6, 0, 190, 12, 0.6, 0, 0),
        food("Cocktail", "cocktail|mojito|margarita|martini|sangria|pina colada", 220, 0, 20, 0, 0, 0, 18, 10, 0, 50, 5, 0, 0, 5),

        fallback,
    )

    private val index: List<Pair<String, FoodEntry>> = entries.flatMap { e -> e.keywords.map { it to e } }

    /** Finds the best nutrition entry for a recognition label. Never fails: unknown dishes use [fallback]. */
    fun match(label: String): FoodMatch {
        val normalized = " ${normalize(label)} "
        var best: FoodEntry? = null
        var bestLength = 0
        for ((keyword, entry) in index) {
            if (keyword.length <= bestLength) continue
            if (normalized.contains(" $keyword ") ||
                normalized.contains(" ${keyword}s ") ||
                normalized.contains(" ${keyword}es ")
            ) {
                best = entry
                bestLength = keyword.length
            }
        }
        return FoodMatch(displayName = prettify(label), entry = best ?: fallback)
    }

    /** True when the label maps to something more specific than the generic fallback. */
    fun isKnownFood(label: String): Boolean = match(label).entry !== fallback

    fun normalize(text: String): String =
        text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun prettify(label: String): String =
        label.replace('_', ' ')
            .trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
}

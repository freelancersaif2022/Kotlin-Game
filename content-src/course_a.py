from dsl import C, H, L, LI, M, Q, T, TIP

# ---------------------------------------------------------------------------
# Module 1 — Getting Started
# ---------------------------------------------------------------------------
m1 = M("m01", 1, "Getting Started", "What Python is, your first program, input and syntax", "module_01_basics", [
    L("m01l1", "What is Python?",
      "Meet the most popular programming language in the world and discover why developers, scientists and companies love it.", [
        T("**Python** is a high-level, general-purpose programming language created by **Guido van Rossum** and first released in 1991. "
          "It was designed to be easy to read — Python code often looks almost like plain English."),
        H("Why learn Python?"),
        LI("**Beginner friendly** — simple, clean syntax with no curly braces or semicolons.",
           "**Huge community** — millions of developers and thousands of free libraries.",
           "**Versatile** — web apps, data science, AI, automation, games, scripting and more.",
           "**In demand** — one of the most requested skills in developer job listings."),
        H("Where is Python used?"),
        LI("**Web development** — Django, Flask and FastAPI power sites like Instagram and Pinterest.",
           "**Data science & AI** — pandas, NumPy, TensorFlow and PyTorch.",
           "**Automation** — scripts that rename files, send emails or scrape websites.",
           "**Education & research** — the language of choice in many universities."),
        T("Python is an **interpreted** language: an interpreter reads your code line by line and runs it immediately. "
          "That's exactly what happens when you tap **Run** in the PyMaster terminal."),
        C('''
            import sys
            print("Hello from Python!")
            print("Python version:", sys.version.split()[0])
        ''', "Your phone is running a real Python interpreter:"),
        TIP("Python is named after the comedy group **Monty Python**, not the snake — but the snake logo stuck!"),
    ], keys=["Python is a high-level, interpreted, general-purpose language.",
             "It is used for web, data science, AI, automation and much more.",
             "Readable syntax makes it perfect for beginners and professionals."],
      practice="Open the terminal and run `print(\"I am learning Python!\")`.", minutes=5),

    L("m01l2", "Your first program: print()",
      "The `print()` function displays text and values on the screen. It's the first tool every Python developer uses.", [
        T("A **function** is a reusable piece of code that performs a task. `print()` is a built-in function that shows output. "
          "You put what you want to display inside the parentheses."),
        C('''
            print("Hello, World!")
            print('Single quotes work too')
            print(42)
            print(3.14)
        '''),
        H("Printing several values"),
        T("Separate values with commas. `print()` puts a space between them automatically."),
        C('''
            print("I am", 25, "years old")
            print("Python", "is", "fun", sep="-")
            print("No new line here", end=" ... ")
            print("same line!")
        '''),
        T("`sep` changes the separator between values and `end` changes what is printed at the end (a new line by default)."),
        H("Empty lines"),
        C('''
            print("Line 1")
            print()
            print("Line 3")
        '''),
        TIP("Text inside quotes is called a **string**. Without quotes, Python thinks it's a name of a variable or function."),
    ], keys=["`print()` displays values on the screen.",
             "Use commas to print several values separated by spaces.",
             "`sep` sets the separator, `end` sets the line ending."],
      practice="Print your name, age and city on one line separated by ` | ` using the `sep` parameter."),

    L("m01l3", "Comments & clean code",
      "Comments explain your code to humans. Python ignores them completely.", [
        T("A **comment** starts with `#`. Everything after `#` on that line is ignored by Python. "
          "Good comments explain **why** the code does something."),
        C('''
            # This program greets the user
            print("Welcome!")  # comments can follow code too

            # print("This line will not run")
        '''),
        H("Multi-line notes (docstrings)"),
        T("Triple-quoted strings are often used as multi-line documentation, called **docstrings**."),
        C('''
            """
            Program: Greeter
            Author:  PyMaster student
            """
            print("Docstrings document your code")
        '''),
        H("PEP 8 — Python's style guide"),
        LI("Use **4 spaces** for indentation.",
           "Name variables in `snake_case`, e.g. `total_price`.",
           "Keep lines reasonably short (about 79–99 characters).",
           "Put spaces around operators: `x = a + b`, not `x=a+b`."),
        TIP("Code is read far more often than it is written. Clean, commented code makes you look like a professional."),
    ], keys=["`#` starts a single-line comment.",
             "Triple quotes create docstrings for documentation.",
             "Follow PEP 8: 4-space indents and snake_case names."],
      practice="Write a 3-line program and add a comment above each line explaining what it does.", minutes=5),

    L("m01l4", "Getting input from the user",
      "`input()` pauses the program, lets the user type something and returns it as a string.", [
        T("Programs become interactive when they ask questions. `input()` shows an optional prompt and waits for the user to press enter."),
        C('''
            name = input("What is your name? ")
            print("Nice to meet you,", name)
        ''', stdin=["Saif"]),
        H("input() always returns text"),
        T("Even if the user types a number, `input()` gives you a **string**. Convert it with `int()` or `float()` to do maths."),
        C('''
            age = input("Enter your age: ")
            print(type(age))
            age = int(age)
            print("Next year you will be", age + 1)
        ''', stdin=["24"]),
        TIP("In the PyMaster terminal, when a program calls `input()`, a yellow input bar appears under the console. Type and press enter."),
    ], keys=["`input(prompt)` reads a line of text from the user.",
             "It always returns a string.",
             "Use `int()` or `float()` to convert input to numbers."],
      practice="Ask for two numbers with `input()`, convert them with `int()` and print their sum."),

    L("m01l5", "Syntax, indentation & errors",
      "Python uses indentation to group code. Learn the rules and how to read error messages.", [
        T("Many languages use `{ }` braces to group code. Python uses **indentation** — the spaces at the start of a line. "
          "Lines in the same block must have the same indentation (4 spaces is standard)."),
        C('''
            temperature = 30
            if temperature > 25:
                print("It's hot today")
                print("Drink water!")
            print("This always runs")
        '''),
        H("Reading error messages"),
        T("When something goes wrong Python shows a **traceback**. The last line tells you the error type and message; "
          "the lines above tell you where it happened."),
        C('''
            print("Start")
            print(undefined_name)
        ''', "A NameError — using a name that doesn't exist:"),
        LI("`SyntaxError` — code that breaks Python's grammar (missing `:` or bracket).",
           "`IndentationError` — wrong or inconsistent indentation.",
           "`NameError` — a variable or function name that doesn't exist.",
           "`TypeError` — an operation on the wrong type, like `\"5\" + 5`."),
        TIP("Errors are normal! Professional developers read error messages carefully — they usually tell you exactly what to fix."),
    ], keys=["Indentation (4 spaces) defines code blocks.",
             "Tracebacks show where and why an error happened.",
             "Read the last line of an error first."],
      practice="In the terminal, remove the indentation from a line inside an `if` block and read the error you get."),
], [
    Q("Who created Python?", ["Guido van Rossum", "James Gosling", "Dennis Ritchie", "Brendan Eich"], 0,
      "Guido van Rossum released Python in 1991."),
    Q("What does this code print?", ["Hi-there", "Hi there", "Hi,there", "Hithere"], 0,
      "`sep=\"-\"` replaces the default space separator.", code='print("Hi", "there", sep="-")'),
    Q("Which symbol starts a comment in Python?", ["`//`", "`#`", "`--`", "`/*`"], 1, "`#` starts a comment."),
    Q("What type does `input()` return?", ["int", "float", "str", "It depends on what the user types"], 2,
      "`input()` always returns a string."),
    Q("How many spaces does PEP 8 recommend for indentation?", ["2", "4", "8", "Tabs only"], 1, "4 spaces per level."),
    Q("Which error do you get when you use a variable that was never created?", ["TypeError", "SyntaxError", "NameError", "ValueError"], 2,
      "A `NameError` means Python doesn't know that name."),
])

# ---------------------------------------------------------------------------
# Module 2 — Variables & Data Types
# ---------------------------------------------------------------------------
m2 = M("m02", 2, "Variables & Data Types", "Store data with variables: numbers, text, booleans and None", "module_02_variables", [
    L("m02l1", "Variables & naming rules",
      "A variable is a name that refers to a value stored in memory.", [
        T("Create a variable with the **assignment operator** `=`. The name goes on the left, the value on the right."),
        C('''
            city = "Dhaka"
            population = 23_000_000
            is_capital = True
            print(city, population, is_capital)
        '''),
        T("Variables can change. Python is **dynamically typed**, so a variable can even hold a different type later."),
        C('''
            score = 10
            score = score + 5
            print(score)
            score = "fifteen"
            print(score)
        '''),
        H("Naming rules"),
        LI("Must start with a letter or underscore: `name`, `_count`.",
           "Can contain letters, numbers and underscores: `player2_score`.",
           "Cannot start with a number: `2player` is invalid.",
           "Case-sensitive: `Age` and `age` are different variables.",
           "Don't use keywords like `if`, `for`, `class` as names."),
        H("Multiple assignment"),
        C('''
            x, y, z = 1, 2, 3
            a = b = 0
            print(x, y, z, a, b)
            x, y = y, x   # swap!
            print(x, y)
        '''),
        TIP("Choose descriptive names: `total_price` is much clearer than `tp`."),
    ], keys=["`=` assigns a value to a variable.",
             "Names are case-sensitive and use snake_case.",
             "`a, b = b, a` swaps two values in one line."],
      practice="Create variables for your name, age and favourite language, then print a sentence using all three."),

    L("m02l2", "Numbers: int, float & complex",
      "Python has three numeric types and handles very large numbers easily.", [
        LI("`int` — whole numbers: `7`, `-3`, `1_000_000`.",
           "`float` — decimal numbers: `3.14`, `-0.5`, `2.0`.",
           "`complex` — numbers with an imaginary part: `2 + 3j` (used in science)."),
        C('''
            a = 10
            b = 3.5
            c = 2 + 3j
            print(type(a), type(b), type(c))
            print(a + b)
            print(2 ** 100)
        '''),
        T("Integers in Python have **unlimited size** — `2 ** 100` works perfectly."),
        H("Float precision"),
        T("Floats are stored in binary, so some decimals can't be represented exactly."),
        C('''
            print(0.1 + 0.2)
            print(round(0.1 + 0.2, 2))
            print(round(3.14159, 3))
        '''),
        H("Useful number functions"),
        C('''
            print(abs(-7))
            print(max(4, 9, 2), min(4, 9, 2))
            print(pow(2, 5))
            print(int(9.99), float(4))
        '''),
        TIP("Use `round()` when displaying money or measurements, or the `decimal` module for exact financial maths."),
    ], keys=["`int` for whole numbers, `float` for decimals.",
             "Python integers can be arbitrarily large.",
             "Floats can have tiny rounding errors — use `round()` for display."],
      practice="Compute the area of a circle with radius 7 using `3.14159 * r ** 2` and round it to 2 decimals."),

    L("m02l3", "Strings basics",
      "Strings store text. They are one of the most used data types.", [
        T("Create strings with single `'...'`, double `\"...\"` or triple quotes for multiple lines."),
        C("""
            first = "Ada"
            last = 'Lovelace'
            full = first + " " + last
            print(full)
            print(len(full))
            print("ha" * 3)
        """),
        T("`+` joins (concatenates) strings and `*` repeats them. `len()` returns the number of characters."),
        H("Indexing"),
        T("Each character has a position (index) starting at **0**. Negative indexes count from the end."),
        C('''
            word = "Python"
            print(word[0], word[1], word[-1])
        '''),
        H("f-strings"),
        T("An **f-string** lets you put variables directly inside text using `{ }`. This is the modern, preferred way to format text."),
        C('''
            name = "Sara"
            score = 92.456
            print(f"{name} scored {score:.1f} points")
            print(f"2 + 3 = {2 + 3}")
        '''),
        TIP("Strings are **immutable** — you can't change a character in place, but you can create a new string."),
    ], keys=["Strings can use single, double or triple quotes.",
             "Indexes start at 0; `-1` is the last character.",
             "f-strings: `f\"Hello {name}\"` embed values in text."],
      practice="Ask the user for their name and print it in uppercase along with its length using an f-string."),

    L("m02l4", "Booleans & None",
      "Booleans represent True or False. None represents 'no value'.", [
        T("A **boolean** (`bool`) has only two values: `True` and `False`. They are the result of comparisons and drive decisions in code."),
        C('''
            is_logged_in = True
            has_paid = False
            print(type(is_logged_in))
            print(10 > 5, 3 == 4)
        '''),
        H("Truthy and falsy values"),
        T("Every value can be tested as a boolean. Empty or zero values are **falsy**; everything else is **truthy**."),
        C('''
            print(bool(0), bool(""), bool([]), bool(None))
            print(bool(42), bool("hi"), bool([1, 2]))
        '''),
        H("None"),
        T("`None` means 'nothing' or 'no value yet'. Functions that don't return anything return `None`."),
        C('''
            result = None
            print(result)
            print(result is None)
        '''),
        TIP("Compare with `None` using `is` or `is not`, not `==`."),
    ], keys=["`True` and `False` are booleans.",
             "`0`, `\"\"`, `[]`, `{}` and `None` are falsy.",
             "Use `is None` to check for None."],
      practice="Print `bool()` of five different values and predict each result before running.", minutes=5),

    L("m02l5", "Type checking & conversion",
      "Find out a value's type and convert between types safely.", [
        T("`type()` returns the type of a value. `isinstance()` checks whether a value is of a given type."),
        C('''
            values = [42, 3.5, "text", True, None, [1, 2]]
            for v in values:
                print(repr(v), "->", type(v).__name__)
            print(isinstance(42, int))
        '''),
        H("Converting types (casting)"),
        C('''
            print(int("25") + 5)
            print(float("2.5") * 2)
            print(str(100) + " points")
            print(int(7.9))
            print(list("abc"))
        '''),
        H("Invalid conversions"),
        T("Converting text that isn't a valid number raises a `ValueError`."),
        C('''
            int("hello")
        '''),
        TIP("You'll learn to handle these errors gracefully with `try`/`except` in Module 10."),
    ], keys=["`type(x)` shows the type, `isinstance(x, int)` tests it.",
             "`int()`, `float()`, `str()`, `bool()`, `list()` convert values.",
             "Invalid conversions raise `ValueError`."],
      practice="Ask the user for a price as text, convert it to float, add 15% tax and print the total."),
], [
    Q("Which variable name is valid?", ["`2nd_place`", "`my-score`", "`player_score`", "`class`"], 2,
      "Names can't start with a digit, contain `-`, or be keywords."),
    Q("What is the output?", ["`[1, 2]`", "`2 1`", "`1 2`", "An error"], 1,
      "`a, b = b, a` swaps the values.", code="a, b = 1, 2\na, b = b, a\nprint(a, b)"),
    Q("What does `\"Python\"[-1]` return?", ["`P`", "`n`", "`o`", "An error"], 1, "`-1` is the last character."),
    Q("Which value is falsy?", ["`\"0\"`", "`[0]`", "`0`", "`\" \"`"], 2,
      "The number `0` is falsy. Non-empty strings and lists are truthy."),
    Q("What is `type(3.0)`?", ["int", "float", "str", "complex"], 1, "Any number with a decimal point is a float."),
    Q("What happens with `int(\"12.5\")`?", ["Returns 12", "Returns 13", "Raises ValueError", "Returns 12.5"], 2,
      "`int()` can't parse a decimal string. Use `int(float(\"12.5\"))`."),
])

# ---------------------------------------------------------------------------
# Module 3 — Operators
# ---------------------------------------------------------------------------
m3 = M("m03", 3, "Operators", "Arithmetic, comparison, logical, assignment and more", "module_03_operators", [
    L("m03l1", "Arithmetic operators",
      "Do maths with Python: add, subtract, multiply, divide and more.", [
        C('''
            a, b = 17, 5
            print("a + b  =", a + b)
            print("a - b  =", a - b)
            print("a * b  =", a * b)
            print("a / b  =", a / b)
            print("a // b =", a // b)
            print("a % b  =", a % b)
            print("a ** 2 =", a ** 2)
        '''),
        LI("`/` true division — always returns a float.",
           "`//` floor division — divides and rounds down.",
           "`%` modulo — the remainder after division.",
           "`**` exponent — power."),
        H("Practical uses of % and //"),
        C('''
            total_minutes = 135
            hours = total_minutes // 60
            minutes = total_minutes % 60
            print(f"{hours}h {minutes}m")
            print("Is 14 even?", 14 % 2 == 0)
        '''),
        TIP("`n % 2 == 0` is the classic way to test whether a number is even."),
    ], keys=["`/` returns a float, `//` rounds down.",
             "`%` gives the remainder.",
             "`**` raises to a power."],
      practice="Convert 1000 seconds into minutes and seconds using `//` and `%`.", minutes=5),

    L("m03l2", "Comparison operators",
      "Compare values to get True or False.", [
        C('''
            x = 10
            print(x == 10)
            print(x != 5)
            print(x > 3, x < 3)
            print(x >= 10, x <= 9)
        '''),
        T("Strings are compared alphabetically (by Unicode code). Uppercase letters come before lowercase."),
        C('''
            x = 10
            print("apple" < "banana")
            print("Zebra" < "apple")
            print(1 < x < 20)
        '''),
        T("Python allows **chained comparisons** like `1 < x < 20` — very readable!"),
        TIP("Don't confuse `=` (assignment) with `==` (comparison)."),
    ], keys=["`==` equal, `!=` not equal, `<`, `>`, `<=`, `>=`.",
             "Comparisons return booleans.",
             "Chain comparisons: `0 <= age <= 120`."],
      practice="Ask for an age and print whether it's between 13 and 19 using a chained comparison.", minutes=4),

    L("m03l3", "Logical operators",
      "Combine conditions with and, or and not.", [
        LI("`and` — True only if **both** sides are True.",
           "`or` — True if **at least one** side is True.",
           "`not` — flips True to False and vice versa."),
        C('''
            age = 22
            has_ticket = True
            print(age >= 18 and has_ticket)
            print(age < 12 or age > 65)
            print(not has_ticket)
        '''),
        H("Short-circuit evaluation"),
        T("Python stops evaluating as soon as the result is known. `or` returns the first truthy value — handy for defaults."),
        C('''
            username = ""
            display = username or "Guest"
            print(display)
            print(0 and 5, 3 and 5)
        '''),
        TIP("Precedence: `not` is evaluated first, then `and`, then `or`. Use parentheses to make intent clear."),
    ], keys=["`and`, `or`, `not` combine booleans.",
             "They short-circuit.",
             "`value or default` is a common pattern."],
      practice="Write a condition that is True when a number is positive AND even.", minutes=5),

    L("m03l4", "Assignment operators",
      "Shortcuts to update a variable using its current value.", [
        C('''
            points = 10
            points += 5
            print(points)
            points -= 3
            print(points)
            points *= 2
            print(points)
            points /= 4
            print(points)
            points //= 2
            print(points)
            points **= 3
            print(points)
        '''),
        T("`x += 1` is the same as `x = x + 1`. These work with strings and lists too."),
        C('''
            message = "Hello"
            message += ", World"
            print(message)
        '''),
        TIP("Python has no `++` or `--` operators. Use `count += 1`."),
    ], keys=["`+=`, `-=`, `*=`, `/=`, `//=`, `%=`, `**=` update in place.",
             "There is no `++` in Python."],
      practice="Start with `balance = 100`, add 50, subtract 30, double it and print it after each step.", minutes=4),

    L("m03l5", "Membership, identity & precedence",
      "Check if a value is inside a collection, whether two names point to the same object, and how Python orders operations.", [
        H("Membership: in / not in"),
        C('''
            fruits = ["apple", "mango", "kiwi"]
            print("mango" in fruits)
            print("grape" not in fruits)
            print("py" in "python")
        '''),
        H("Identity: is / is not"),
        T("`is` checks whether two variables refer to the **same object** in memory — not just equal values."),
        C('''
            a = [1, 2, 3]
            b = a
            c = [1, 2, 3]
            print(a == c, a is c)
            print(a is b)
        '''),
        H("Operator precedence"),
        T("From highest to lowest: `**` → unary `-` → `* / // %` → `+ -` → comparisons → `not` → `and` → `or`."),
        C('''
            print(2 + 3 * 4)
            print((2 + 3) * 4)
            print(-2 ** 2)
            print(10 - 4 - 3)
        '''),
        TIP("When in doubt, add parentheses. They make code easier to read."),
    ], keys=["`in` tests membership in strings, lists, dicts…",
             "`is` compares identity, `==` compares value.",
             "`**` binds tighter than unary minus: `-2 ** 2` is `-4`."],
      practice="Check whether the letter `\"z\"` is in your name, and print the result."),
], [
    Q("What is `17 // 5`?", ["3.4", "3", "2", "4"], 1, "Floor division rounds down: 17 / 5 = 3.4 → 3."),
    Q("What is `17 % 5`?", ["2", "3", "3.4", "12"], 0, "17 = 5 × 3 + **2**."),
    Q("What does this print?", ["Guest", "`\"\"`", "False", "None"], 0,
      "An empty string is falsy, so `or` returns the second value.", code='name = ""\nprint(name or "Guest")'),
    Q("What is the result of `2 + 3 * 4 ** 2`?", ["400", "50", "80", "35"], 1, "`4**2=16`, `3*16=48`, `2+48=50`."),
    Q("Which operator checks if two variables refer to the same object?", ["`==`", "`is`", "`in`", "`=`"], 1,
      "`is` compares identity."),
    Q("What does `\"cat\" in \"concatenate\"` return?", ["True", "False", "An error", "\"cat\""], 0,
      "`in` finds the substring \"cat\" inside \"con**cat**enate\"."),
])

# ---------------------------------------------------------------------------
# Module 4 — Conditions
# ---------------------------------------------------------------------------
m4 = M("m04", 4, "Conditions", "Make decisions with if, elif, else and match", "module_04_conditions", [
    L("m04l1", "The if statement",
      "Run code only when a condition is True.", [
        T("An `if` statement checks a condition. If it's True, the indented block runs; otherwise it's skipped. "
          "Don't forget the colon `:` at the end of the line."),
        C('''
            battery = 15
            if battery < 20:
                print("Low battery!")
                print("Please charge your phone.")
            print("Check complete")
        '''),
        TIP("Every line inside the block must be indented by the same amount (4 spaces)."),
    ], keys=["`if condition:` followed by an indented block.",
             "The block runs only when the condition is True."],
      practice="Ask for a temperature and print `\"Wear a jacket\"` if it is below 15.", minutes=4),

    L("m04l2", "if ... else",
      "Choose between two paths.", [
        C('''
            number = int(input("Enter a number: "))
            if number % 2 == 0:
                print(number, "is even")
            else:
                print(number, "is odd")
        ''', stdin=["7"]),
        H("Conditional expression (ternary)"),
        T("For simple choices you can write an if/else on one line."),
        C('''
            age = 20
            status = "adult" if age >= 18 else "minor"
            print(status)
        '''),
    ], keys=["`else:` runs when the `if` condition is False.",
             "`x if condition else y` is a one-line conditional."],
      practice="Ask for a password and print `Access granted` if it equals `\"python123\"`, otherwise `Access denied`.", minutes=4),

    L("m04l3", "elif — multiple conditions",
      "Check several conditions in order; the first True one wins.", [
        C('''
            score = 78
            if score >= 90:
                grade = "A"
            elif score >= 80:
                grade = "B"
            elif score >= 70:
                grade = "C"
            elif score >= 60:
                grade = "D"
            else:
                grade = "F"
            print("Grade:", grade)
        '''),
        T("Python tests each condition from top to bottom and runs only the **first** matching block. Order matters!"),
        TIP("Put the most specific (or highest) condition first."),
    ], keys=["`elif` = 'else if'.",
             "Only the first True branch runs.",
             "`else` catches everything left over."],
      practice="Write a BMI category checker: Underweight (<18.5), Normal (<25), Overweight (<30), Obese.", minutes=5),

    L("m04l4", "Nested & combined conditions",
      "Put conditions inside conditions, or combine them with and/or.", [
        C('''
            age = 25
            has_license = True
            if age >= 18:
                if has_license:
                    print("You can drive")
                else:
                    print("Get a license first")
            else:
                print("Too young to drive")
        '''),
        T("Often the same logic is cleaner with logical operators:"),
        C('''
            age = 25
            has_license = True
            if age >= 18 and has_license:
                print("You can drive")
            elif age >= 18:
                print("Get a license first")
            else:
                print("Too young to drive")
        '''),
        H("Checking for empty values"),
        C('''
            cart = []
            if not cart:
                print("Your cart is empty")
        '''),
        TIP("Avoid deep nesting — flat code with `and`/`or`/`elif` is easier to read."),
    ], keys=["Conditions can be nested.",
             "`and`/`or` often replace nesting.",
             "`if not items:` checks for empty collections."],
      practice="A shop gives 20% off to members who spend over 100, and 10% to everyone else over 100. Code it!"),

    L("m04l5", "match-case (pattern matching)",
      "Python 3.10+ has a powerful match statement, similar to switch in other languages.", [
        C('''
            command = "stop"
            match command:
                case "start":
                    print("Starting engine")
                case "stop":
                    print("Stopping engine")
                case "pause" | "wait":
                    print("Pausing")
                case _:
                    print("Unknown command")
        '''),
        T("`case _:` is the wildcard — it matches anything, like `else`. `|` means 'or'."),
        H("Matching structures"),
        C('''
            point = (0, 5)
            match point:
                case (0, 0):
                    print("Origin")
                case (0, y):
                    print(f"On the Y axis at y={y}")
                case (x, 0):
                    print(f"On the X axis at x={x}")
                case (x, y):
                    print(f"Point at {x}, {y}")
        '''),
        TIP("match-case shines when handling commands, menu options or structured data."),
    ], keys=["`match value:` with `case pattern:` blocks.",
             "`case _:` is the default.",
             "Patterns can capture values like `(0, y)`."],
      practice="Build a tiny menu: match `\"1\"`, `\"2\"`, `\"3\"` and print a different message for each."),
], [
    Q("What is printed?", ["Big", "Medium", "Small", "Nothing"], 1,
      "15 is not > 20, but it is > 10, so the elif runs.",
      code="n = 15\nif n > 20:\n    print(\"Big\")\nelif n > 10:\n    print(\"Medium\")\nelse:\n    print(\"Small\")"),
    Q("Which character must end an `if` line?", ["`;`", "`:`", "`{`", "`.`"], 1, "Python blocks start after a colon."),
    Q("What is `\"yes\" if 0 else \"no\"`?", ["yes", "no", "0", "An error"], 1, "`0` is falsy, so the else value is used."),
    Q("In `match`, which pattern matches anything?", ["`case default:`", "`case *:`", "`case _:`", "`case else:`"], 2,
      "The underscore is the wildcard pattern."),
    Q("How many `elif` blocks can an `if` statement have?", ["Only one", "Up to 3", "As many as you need", "None"], 2,
      "You can chain any number of `elif` blocks."),
    Q("What is the output?", ["A", "B", "A and B", "Nothing"], 0,
      "Only the first matching branch runs.",
      code="x = 100\nif x > 50:\n    print(\"A\")\nelif x > 10:\n    print(\"B\")"),
])

MODULES_A = [m1, m2, m3, m4]

from dsl import C, H, L, LI, M, Q, T, TIP

# ---------------------------------------------------------------------------
# Module 5 — Loops
# ---------------------------------------------------------------------------
m5 = M("m05", 5, "Loops", "Repeat work with for and while loops", "module_05_loops", [
    L("m05l1", "for loops & range()",
      "A for loop repeats code once for every item in a sequence.", [
        C('''
            for i in range(5):
                print("Iteration", i)
        '''),
        T("`range(stop)` produces numbers from 0 up to (but **not including**) `stop`. You can also pass a start and a step."),
        C('''
            print(list(range(1, 6)))
            print(list(range(0, 20, 5)))
            print(list(range(10, 0, -2)))
        '''),
        H("Multiplication table"),
        C('''
            n = 7
            for i in range(1, 11):
                print(f"{n} x {i:>2} = {n * i:>3}")
        '''),
        TIP("`range(1, 11)` gives 1 to 10 — the end value is always excluded."),
    ], keys=["`for item in sequence:` repeats for each item.",
             "`range(start, stop, step)` excludes `stop`.",
             "Use a negative step to count down."],
      practice="Print all even numbers from 2 to 30 using `range()` with a step.", minutes=5),

    L("m05l2", "while loops",
      "A while loop repeats as long as its condition stays True.", [
        C('''
            countdown = 5
            while countdown > 0:
                print(countdown)
                countdown -= 1
            print("Happy New Year! 🎉")
        '''),
        T("Make sure something inside the loop eventually makes the condition False, or you'll create an **infinite loop**."),
        H("Loop until valid input"),
        C('''
            password = ""
            while password != "python":
                password = input("Password: ")
            print("Welcome in!")
        ''', stdin=["java", "1234", "python"]),
        TIP("Stuck in an infinite loop in the terminal? Tap **Stop** to interrupt it."),
    ], keys=["`while condition:` loops while the condition is True.",
             "Update variables inside the loop to avoid infinite loops."],
      practice="Keep asking for numbers and add them up until the user types 0, then print the total.", minutes=5),

    L("m05l3", "break, continue & else",
      "Control the flow of a loop from the inside.", [
        LI("`break` — exit the loop immediately.",
           "`continue` — skip the rest of this iteration and move to the next.",
           "`else` on a loop — runs only if the loop finished **without** `break`."),
        C('''
            for n in range(1, 10):
                if n == 3:
                    continue
                if n == 7:
                    break
                print(n)
        '''),
        H("Search with for ... else"),
        C('''
            numbers = [4, 9, 15, 22]
            for n in numbers:
                if n % 11 == 0:
                    print("Found a multiple of 11:", n)
                    break
            else:
                print("No multiple of 11 found")
        '''),
        H("Infinite loop with break"),
        C('''
            total = 0
            while True:
                value = int(input("Number (0 to stop): "))
                if value == 0:
                    break
                total += value
            print("Total:", total)
        ''', stdin=["5", "10", "0"]),
    ], keys=["`break` exits, `continue` skips to the next iteration.",
             "`while True:` + `break` is a common pattern.",
             "Loop `else` runs when no `break` happened."],
      practice="Print numbers 1–50 but skip multiples of 3 and stop completely at the first multiple of 17."),

    L("m05l4", "Nested loops & patterns",
      "Put a loop inside another loop to work with grids and patterns.", [
        C('''
            for row in range(1, 4):
                for col in range(1, 4):
                    print(f"({row},{col})", end=" ")
                print()
        '''),
        H("Star pyramid"),
        C('''
            height = 5
            for i in range(1, height + 1):
                print(" " * (height - i) + "*" * (2 * i - 1))
        '''),
        TIP("The inner loop runs completely for every single iteration of the outer loop."),
    ], keys=["Nested loops multiply: 3 × 3 = 9 iterations.",
             "Great for grids, tables and patterns."],
      practice="Print a 5×5 multiplication grid with numbers aligned using f-string width formatting.", minutes=5),

    L("m05l5", "Looping like a pro: enumerate & zip",
      "Pythonic tools that make loops cleaner.", [
        H("enumerate() — index and value"),
        C('''
            languages = ["Python", "Kotlin", "Rust"]
            for index, lang in enumerate(languages, start=1):
                print(index, lang)
        '''),
        H("zip() — loop over several lists together"),
        C('''
            names = ["Ali", "Sara", "John"]
            scores = [88, 95, 72]
            for name, score in zip(names, scores):
                print(f"{name:<6}{score}")
        '''),
        H("Looping over a dictionary"),
        C('''
            stock = {"apples": 12, "pears": 0, "plums": 7}
            for item, qty in stock.items():
                status = "in stock" if qty else "sold out"
                print(f"{item}: {status}")
        '''),
        H("Useful helpers"),
        C('''
            nums = [5, 2, 9, 1]
            print(sum(nums), sorted(nums), list(reversed(nums)))
        '''),
    ], keys=["`enumerate()` gives index + value.",
             "`zip()` pairs items from several sequences.",
             "`dict.items()` gives key + value pairs."],
      practice="Use `zip()` to print each student with their grade from two lists."),
], [
    Q("What does `list(range(2, 10, 3))` produce?", ["`[2, 5, 8]`", "`[2, 5, 8, 11]`", "`[3, 6, 9]`", "`[2, 4, 6, 8]`"], 0,
      "Start at 2, step 3, stop before 10."),
    Q("What is printed?", ["0 1 2", "1 2", "0 1", "0 2"], 3, "`continue` skips 1.",
      code="for i in range(3):\n    if i == 1:\n        continue\n    print(i, end=\" \")"),
    Q("When does a loop's `else` block run?", ["Always", "Only when the loop never runs", "When the loop ends without `break`", "When an error happens"], 2,
      "`else` is skipped if the loop was exited with `break`."),
    Q("How many times does the inner print run?", ["3", "4", "7", "12"], 3, "3 × 4 = 12.",
      code="for a in range(3):\n    for b in range(4):\n        print(a, b)"),
    Q("What does `enumerate([\"a\", \"b\"])` give you?", ["Only values", "Only indexes", "Index and value pairs", "A dictionary"], 2,
      "It yields `(0, 'a')`, `(1, 'b')`."),
    Q("What is the risk of `while True:` without `break`?", ["Syntax error", "Infinite loop", "It runs once", "Nothing"], 1,
      "Without a `break`, the loop never ends."),
])

# ---------------------------------------------------------------------------
# Module 6 — Functions
# ---------------------------------------------------------------------------
m6 = M("m06", 6, "Functions", "Write reusable code with def, parameters and return", "module_06_functions", [
    L("m06l1", "Defining & calling functions",
      "Functions package code under a name so you can reuse it.", [
        C('''
            def greet():
                print("Hello!")
                print("Welcome to PyMaster")

            greet()
            greet()
        '''),
        T("`def` defines a function. The code inside doesn't run until you **call** it with parentheses."),
        H("Why functions?"),
        LI("**Reuse** — write once, use many times.",
           "**Organize** — split big programs into small named pieces.",
           "**Test** — small functions are easy to check."),
        TIP("Name functions with verbs in snake_case: `calculate_total`, `send_email`."),
    ], keys=["`def name():` defines a function.",
             "Call it with `name()`.",
             "Functions make code reusable and organized."],
      practice="Write a function `print_banner()` that prints a 3-line welcome banner, then call it twice.", minutes=4),

    L("m06l2", "Parameters & return values",
      "Pass data in with parameters and get results back with return.", [
        C('''
            def add(a, b):
                return a + b

            result = add(3, 4)
            print(result)
            print(add(10, 20) * 2)
        '''),
        T("`return` sends a value back to the caller and ends the function. Without `return`, a function returns `None`."),
        C('''
            def area(width, height):
                return width * height

            def describe(w, h):
                print(f"A {w}x{h} room is {area(w, h)} m²")

            describe(4, 5)
            print(describe(2, 3))
        '''),
        H("Returning several values"),
        C('''
            def min_max(numbers):
                return min(numbers), max(numbers)

            low, high = min_max([7, 2, 9, 4])
            print(low, high)
        '''),
    ], keys=["Parameters receive values (arguments).",
             "`return` sends back a result.",
             "Return several values as a tuple."],
      practice="Write `celsius_to_fahrenheit(c)` that returns `c * 9 / 5 + 32` and print it for 0, 25 and 100."),

    L("m06l3", "Default & keyword arguments",
      "Make functions flexible and easy to call.", [
        C('''
            def make_coffee(size="medium", sugar=1, milk=True):
                milk_text = "with milk" if milk else "black"
                return f"{size} coffee, {sugar} sugar, {milk_text}"

            print(make_coffee())
            print(make_coffee("large"))
            print(make_coffee(sugar=0, milk=False))
        '''),
        T("Default values are used when an argument isn't passed. **Keyword arguments** (`sugar=0`) can be given in any order."),
        TIP("Never use a mutable default like `def f(items=[])` — use `items=None` and create the list inside."),
    ], keys=["Defaults: `def f(x=10):`.",
             "Keyword args: `f(x=5)`, order doesn't matter.",
             "Avoid mutable default arguments."],
      practice="Write `power(base, exp=2)` and call it with and without the exponent.", minutes=5),

    L("m06l4", "*args and **kwargs",
      "Accept any number of positional or keyword arguments.", [
        C('''
            def total(*numbers):
                print("Received:", numbers)
                return sum(numbers)

            print(total(1, 2, 3))
            print(total(10, 20, 30, 40))
        '''),
        T("`*args` collects extra positional arguments into a **tuple**. `**kwargs` collects keyword arguments into a **dictionary**."),
        C('''
            def profile(name, **details):
                print("Name:", name)
                for key, value in details.items():
                    print(f"  {key}: {value}")

            profile("Saif", role="Developer", language="Python")
        '''),
        H("Unpacking when calling"),
        C('''
            def point(x, y, z):
                return f"({x}, {y}, {z})"

            coords = [1, 2, 3]
            print(point(*coords))
            print(point(**{"x": 4, "y": 5, "z": 6}))
        '''),
    ], keys=["`*args` → tuple of extra positional args.",
             "`**kwargs` → dict of extra keyword args.",
             "`*` and `**` also unpack when calling."],
      practice="Write `average(*nums)` that returns the average of any amount of numbers."),

    L("m06l5", "Scope & lambda functions",
      "Understand where variables live, and write tiny anonymous functions.", [
        H("Local vs global scope"),
        C('''
            message = "global"

            def show():
                message = "local"
                print("Inside:", message)

            show()
            print("Outside:", message)
        '''),
        T("Variables created inside a function are **local** — they disappear when the function ends. "
          "Use `global` only when you really need to change a global variable."),
        C('''
            counter = 0

            def increment():
                global counter
                counter += 1

            increment()
            increment()
            print(counter)
        '''),
        H("lambda — small anonymous functions"),
        C('''
            square = lambda x: x * x
            print(square(6))

            people = [("Ali", 31), ("Sara", 24), ("John", 28)]
            people.sort(key=lambda p: p[1])
            print(people)
        '''),
        TIP("lambda is perfect as a `key` for `sorted()`, `max()` and `min()`."),
    ], keys=["Local variables live only inside their function.",
             "`global` lets a function change a global variable.",
             "`lambda args: expression` creates a small function."],
      practice="Sort a list of words by their length using `sorted(words, key=lambda w: len(w))`."),

    L("m06l6", "Recursion",
      "A function that calls itself to solve smaller versions of a problem.", [
        C('''
            def factorial(n):
                if n <= 1:
                    return 1
                return n * factorial(n - 1)

            print(factorial(5))
            print(factorial(10))
        '''),
        T("Every recursive function needs a **base case** that stops the recursion, otherwise it calls itself forever."),
        C('''
            def countdown(n):
                if n == 0:
                    print("Lift off!")
                    return
                print(n)
                countdown(n - 1)

            countdown(3)
        '''),
        C('''
            def fibonacci(n):
                if n < 2:
                    return n
                return fibonacci(n - 1) + fibonacci(n - 2)

            print([fibonacci(i) for i in range(10)])
        '''),
        TIP("Python limits recursion depth (about 1000 calls). For big inputs a loop is often better."),
    ], keys=["A recursive function calls itself.",
             "Always define a base case.",
             "Loops can replace recursion for large inputs."],
      practice="Write a recursive `sum_to(n)` that returns 1 + 2 + … + n."),
], [
    Q("What does a function return if it has no `return` statement?", ["0", "An empty string", "None", "False"], 2,
      "Functions return `None` by default."),
    Q("What is printed?", ["5", "7", "12", "An error"], 2, "`b` uses its default of 10: 2 + 10 = 12.",
      code="def f(a, b=10):\n    return a + b\nprint(f(2))"),
    Q("What type is `args` inside `def f(*args)`?", ["list", "tuple", "dict", "set"], 1, "`*args` is a tuple."),
    Q("What does `(lambda x: x * 3)(4)` evaluate to?", ["7", "12", "43", "An error"], 1, "4 × 3 = 12."),
    Q("What is the base case in recursion?", ["The first call", "The condition that stops recursion", "A global variable", "The return type"], 1,
      "Without a base case, recursion never ends."),
    Q("What is printed?", ["1", "5", "An error", "None"], 0,
      "The function changes only its local `x`.",
      code="x = 1\ndef change():\n    x = 5\nchange()\nprint(x)"),
])

# ---------------------------------------------------------------------------
# Module 7 — Collections
# ---------------------------------------------------------------------------
m7 = M("m07", 7, "Collections", "Lists, tuples, sets, dictionaries and comprehensions", "module_07_collections", [
    L("m07l1", "Lists",
      "Ordered, changeable collections — the workhorse of Python.", [
        C('''
            fruits = ["apple", "banana", "cherry"]
            print(fruits[0], fruits[-1])
            fruits[1] = "blueberry"
            print(fruits)
            print(len(fruits))
        '''),
        H("Adding and removing"),
        C('''
            tasks = ["code"]
            tasks.append("test")
            tasks.insert(0, "plan")
            tasks.extend(["deploy", "relax"])
            print(tasks)
            tasks.remove("relax")
            last = tasks.pop()
            print(last, tasks)
        '''),
        TIP("Lists can hold mixed types: `[1, \"two\", 3.0, True]`, and even other lists."),
    ], keys=["Lists are ordered and mutable: `[a, b, c]`.",
             "`append`, `insert`, `extend` add items.",
             "`remove`, `pop` delete items."],
      practice="Create a shopping list, add 3 items, remove 1 and print the final list with its length."),

    L("m07l2", "List slicing & methods",
      "Slice lists and use built-in methods to sort, search and count.", [
        C('''
            nums = [10, 20, 30, 40, 50, 60]
            print(nums[1:4])
            print(nums[:3])
            print(nums[3:])
            print(nums[::2])
            print(nums[::-1])
        '''),
        T("Slicing uses `[start:stop:step]` — like `range()`, `stop` is excluded."),
        C('''
            scores = [72, 95, 88, 61, 95]
            scores.sort()
            print(scores)
            scores.sort(reverse=True)
            print(scores)
            print(scores.count(95), scores.index(88))
            print(sum(scores) / len(scores))
        '''),
        H("Copying lists"),
        C('''
            original = [1, 2, 3]
            alias = original
            copy = original.copy()
            original.append(4)
            print(alias, copy)
        '''),
        TIP("`b = a` doesn't copy a list — both names point to the same list. Use `.copy()` or `a[:]`."),
    ], keys=["`lst[start:stop:step]` slices.",
             "`lst[::-1]` reverses.",
             "Use `.copy()` to duplicate a list."],
      practice="Given a list of 10 numbers, print the first 3, the last 3 and every second number."),

    L("m07l3", "Tuples",
      "Ordered collections that can't be changed after creation.", [
        C('''
            point = (3, 4)
            rgb = (255, 128, 0)
            print(point[0], rgb[-1])
            print(len(rgb))
        '''),
        T("Tuples are **immutable**: you can't add, remove or change items. They're perfect for fixed data like coordinates or dates."),
        C('''
            colors = ("red", "green")
            colors[0] = "blue"
        ''', "Trying to change a tuple raises an error:"),
        H("Tuple unpacking"),
        C('''
            person = ("Sara", 28, "Engineer")
            name, age, job = person
            print(f"{name} is a {age}-year-old {job}")
            single = (5,)
            print(type(single))
        '''),
        TIP("A one-item tuple needs a trailing comma: `(5,)`."),
    ], keys=["Tuples use `( )` and are immutable.",
             "Unpack with `a, b, c = my_tuple`.",
             "One-item tuple: `(x,)`."],
      practice="Store a date as `(day, month, year)` and print it as `day/month/year` using unpacking.", minutes=5),

    L("m07l4", "Sets",
      "Unordered collections of unique items with powerful maths operations.", [
        C('''
            tags = {"python", "code", "python", "ai"}
            print(len(tags))
            tags.add("web")
            tags.discard("code")
            print("ai" in tags)
        '''),
        T("Sets automatically remove duplicates — a quick way to get unique values from a list."),
        C('''
            emails = ["a@x.com", "b@x.com", "a@x.com"]
            print(sorted(set(emails)))
        '''),
        H("Set operations"),
        C('''
            python_devs = {"Ali", "Sara", "John"}
            kotlin_devs = {"Sara", "Mia"}
            print(sorted(python_devs | kotlin_devs))
            print(python_devs & kotlin_devs)
            print(sorted(python_devs - kotlin_devs))
        '''),
        TIP("Checking `x in my_set` is extremely fast, even for huge sets."),
    ], keys=["Sets hold unique items: `{1, 2, 3}`.",
             "`|` union, `&` intersection, `-` difference.",
             "Use `set()` for an empty set (not `{}`)."],
      practice="Find the common letters in two words using a set intersection.", minutes=5),

    L("m07l5", "Dictionaries",
      "Store data as key → value pairs for fast lookups.", [
        C('''
            student = {"name": "Ali", "age": 21, "major": "CS"}
            print(student["name"])
            student["age"] = 22
            student["gpa"] = 3.8
            print(student)
        '''),
        H("Safe access and methods"),
        C('''
            student = {"name": "Ali", "age": 22}
            print(student.get("email", "not set"))
            print(list(student.keys()))
            print(list(student.values()))
            removed = student.pop("age")
            print(removed, student)
        '''),
        H("Counting with a dictionary"),
        C('''
            text = "banana"
            counts = {}
            for letter in text:
                counts[letter] = counts.get(letter, 0) + 1
            print(counts)
        '''),
        H("Nested data"),
        C('''
            users = {
                "u1": {"name": "Sara", "skills": ["Python", "SQL"]},
                "u2": {"name": "John", "skills": ["Kotlin"]},
            }
            for uid, info in users.items():
                print(uid, info["name"], ", ".join(info["skills"]))
        '''),
        TIP("Dictionary keys must be immutable (strings, numbers, tuples) and unique."),
    ], keys=["`{key: value}` maps keys to values.",
             "`d.get(key, default)` avoids KeyError.",
             "`.keys()`, `.values()`, `.items()` for looping."],
      practice="Build a phone book dictionary with 3 contacts and print each name with its number."),

    L("m07l6", "Comprehensions",
      "Build lists, sets and dictionaries in a single expressive line.", [
        C('''
            squares = [n * n for n in range(1, 8)]
            print(squares)
            evens = [n for n in range(20) if n % 2 == 0]
            print(evens)
        '''),
        T("A **list comprehension** has the form `[expression for item in iterable if condition]`."),
        C('''
            words = ["python", "is", "awesome"]
            print([w.upper() for w in words])
            print({w: len(w) for w in words})
            print({len(w) for w in words})
        '''),
        H("With if-else"),
        C('''
            nums = [1, 2, 3, 4, 5]
            print(["even" if n % 2 == 0 else "odd" for n in nums])
        '''),
        TIP("Keep comprehensions short. If it gets hard to read, use a normal loop."),
    ], keys=["`[expr for x in items if cond]` builds a list.",
             "`{k: v for ...}` builds a dict, `{x for ...}` a set.",
             "Readability first!"],
      practice="Create a list of the cubes of all odd numbers from 1 to 15 with a comprehension."),
], [
    Q("What does `[1, 2, 3, 4, 5][1:3]` return?", ["`[1, 2, 3]`", "`[2, 3]`", "`[2, 3, 4]`", "`[1, 2]`"], 1,
      "Index 1 up to (not including) 3."),
    Q("Which collection does NOT allow duplicates?", ["list", "tuple", "set", "All allow duplicates"], 2, "Sets keep unique items only."),
    Q("What happens with `my_tuple[0] = 5`?", ["It works", "TypeError", "The tuple becomes a list", "Nothing"], 1,
      "Tuples are immutable."),
    Q("What does `d.get(\"x\", 0)` return if `\"x\"` is missing?", ["None", "KeyError", "0", "\"x\""], 2,
      "`get` returns the default you supply."),
    Q("What is the output?", ["`[0, 1, 4, 9]`", "`[1, 4, 9, 16]`", "`[0, 2, 4, 6]`", "`[0, 1, 2, 3]`"], 0,
      "Squares of 0, 1, 2, 3.", code="print([x ** 2 for x in range(4)])"),
    Q("What does `{1, 2, 3} & {2, 3, 4}` give?", ["`{1, 2, 3, 4}`", "`{2, 3}`", "`{1, 4}`", "`{1}`"], 1,
      "`&` is the intersection."),
])

# ---------------------------------------------------------------------------
# Module 8 — Strings deep dive
# ---------------------------------------------------------------------------
m8 = M("m08", 8, "Working with Strings", "String methods, slicing, formatting and text processing", "module_08_strings", [
    L("m08l1", "String methods",
      "Python strings come with dozens of handy built-in methods.", [
        C('''
            text = "  Hello, Python World!  "
            print(text.strip())
            print(text.lower())
            print(text.upper())
            print(text.strip().title())
            print(text.replace("World", "Developers"))
        '''),
        H("Searching"),
        C('''
            s = "learning python is fun"
            print(s.startswith("learn"), s.endswith("!"))
            print(s.find("python"), s.find("java"))
            print(s.count("n"))
        '''),
        H("Checking content"),
        C('''
            print("12345".isdigit(), "abc".isalpha(), "abc123".isalnum())
            print("   ".isspace(), "Hello".istitle())
        '''),
        TIP("String methods return **new** strings — the original stays the same."),
    ], keys=["`strip`, `lower`, `upper`, `title`, `replace` transform text.",
             "`find` returns -1 if not found.",
             "`isdigit`, `isalpha` validate input."],
      practice="Ask for a username, strip spaces, make it lowercase and check that it contains only letters and numbers."),

    L("m08l2", "Split, join & slicing",
      "Break text into parts, put it back together and cut out pieces.", [
        C('''
            sentence = "Python is easy to learn"
            words = sentence.split()
            print(words)
            print(len(words))
            csv_line = "Ali,25,Dhaka"
            print(csv_line.split(","))
        '''),
        C('''
            words = ["Python", "Kotlin", "Swift"]
            print(", ".join(words))
            print(" -> ".join(words))
        '''),
        H("Slicing strings"),
        C('''
            code = "PYM-2026-ABCD"
            print(code[:3])
            print(code[4:8])
            print(code[-4:])
            print("racecar"[::-1])
        '''),
        TIP("`text[::-1]` reverses a string — great for checking palindromes."),
    ], keys=["`split(sep)` turns a string into a list.",
             "`sep.join(list)` turns a list into a string.",
             "Strings slice just like lists."],
      practice="Check whether a word typed by the user is a palindrome."),

    L("m08l3", "f-strings & formatting",
      "Format numbers, align columns and build professional output.", [
        C('''
            name, price, qty = "Laptop", 1299.5, 3
            print(f"{name} costs ${price:,.2f}")
            print(f"Total: {price * qty:,.2f}")
            print(f"{0.4567:.1%}")
            print(f"{42:05d}")
        '''),
        H("Alignment"),
        C('''
            items = [("Coffee", 3.5), ("Sandwich", 7.25), ("Juice", 4)]
            print(f"{'Item':<12}{'Price':>8}")
            print("-" * 20)
            for item, cost in items:
                print(f"{item:<12}{cost:>8.2f}")
        '''),
        LI("`:<10` left-align, `:>10` right-align, `:^10` centre in 10 characters.",
           "`:.2f` two decimal places, `:,` thousands separator, `:%` percentage."),
        H("Debugging with ="),
        C('''
            x = 7
            y = 3
            print(f"{x=}, {y=}, {x * y=}")
        '''),
    ], keys=["`f\"{value:.2f}\"` formats decimals.",
             "`<`, `>`, `^` align text in columns.",
             "`f\"{x=}\"` prints name and value — great for debugging."],
      practice="Print a neat receipt with three items, aligned prices and a total."),

    L("m08l4", "Escape characters & multiline text",
      "Special characters, raw strings and long text.", [
        C(r'''
            print("Line 1\nLine 2")
            print("Name:\tSaif")
            print("She said \"Hi!\"")
            print('It\'s Python')
            print("Back\\slash")
        '''),
        LI("`\\n` new line, `\\t` tab, `\\\\` backslash, `\\\"` quote."),
        H("Multiline strings"),
        C('''
            poem = """Roses are red,
            Python is neat,
            Indentation matters,
            And code is complete."""
            print(poem)
        '''),
        H("Raw strings"),
        C(r'''
            path = r"C:\new\test"
            print(path)
        '''),
        TIP("Use raw strings `r\"...\"` for Windows paths and regular expressions."),
    ], keys=["Escape sequences start with a backslash.",
             "Triple quotes keep line breaks.",
             "Raw strings ignore escapes."],
      practice="Print a small table using `\\t` tabs between columns.", minutes=5),

    L("m08l5", "Text processing in practice",
      "Combine everything to analyse real text.", [
        C('''
            text = """Python is powerful. Python is friendly.
            Python is fast to learn and fun to use."""

            words = text.lower().replace(".", "").split()
            print("Words:", len(words))
            print("Unique:", len(set(words)))

            freq = {}
            for w in words:
                freq[w] = freq.get(w, 0) + 1
            top = sorted(freq.items(), key=lambda kv: kv[1], reverse=True)[:3]
            for word, count in top:
                print(f"{word:<10}{count}")
        '''),
        H("Simple validation"),
        C('''
            def is_valid_email(email):
                email = email.strip()
                return "@" in email and "." in email.split("@")[-1] and " " not in email

            for e in ["saif@mail.com", "bad.email", "a b@x.com"]:
                print(e, "->", is_valid_email(e))
        '''),
        TIP("For complex patterns, learn the `re` (regular expressions) module next."),
    ], keys=["Normalize text with `lower()` and `replace()` before analysing.",
             "Dictionaries are perfect for counting words.",
             "Small functions make validation reusable."],
      practice="Count how many vowels are in a sentence entered by the user."),
], [
    Q("What does `\"  hi  \".strip()` return?", ["`\"hi  \"`", "`\"  hi\"`", "`\"hi\"`", "`\" hi \"`"], 2, "`strip()` removes spaces on both sides."),
    Q("What is `\"-\".join([\"a\", \"b\", \"c\"])`?", ["`\"abc\"`", "`\"a-b-c\"`", "`[\"a-b-c\"]`", "`\"-a-b-c-\"`"], 1, "join puts the separator between items."),
    Q("What does `\"hello\".find(\"z\")` return?", ["0", "None", "-1", "An error"], 2, "`find` returns -1 when not found."),
    Q("What is printed?", ["3.14", "3.142", "3.1416", "3"], 1, "`.3f` = three decimal places.",
      code="pi = 3.14159\nprint(f\"{pi:.3f}\")"),
    Q("What does `\"Python\"[::-1]` give?", ["`\"Python\"`", "`\"nohtyP\"`", "`\"P\"`", "An error"], 1, "A step of -1 reverses the string."),
    Q("What does `\"a,b,c\".split(\",\")` return?", ["`\"abc\"`", "`[\"a\", \"b\", \"c\"]`", "`(\"a\", \"b\", \"c\")`", "`[\"a,b,c\"]`"], 1,
      "split returns a list."),
])

MODULES_B = [m5, m6, m7, m8]

from dsl import P, STEP

PROJECTS = [
    # 1 -------------------------------------------------------------------
    P("calculator", 1, "Smart Calculator", "A menu-driven calculator with history and error handling",
      "project_01_calculator", "Beginner", 30,
      """
      Build a command-line calculator that supports the four basic operations plus power and modulo.
      It validates every input, refuses to divide by zero and keeps a history of all calculations —
      exactly the kind of robust tool professionals write.
      """,
      ["Menu with 6 operations", "Input validation with try/except", "Division-by-zero protection",
       "Calculation history", "Clean formatted output"],
      ["functions", "dictionaries", "while loops", "try/except", "f-strings", "lambda"],
      [
          STEP("Plan the operations", """
              Store each operation in a dictionary that maps a symbol to a function. This avoids a long chain of `if/elif`
              statements and makes adding new operations trivial.""", '''
              operations = {
                  "+": lambda a, b: a + b,
                  "-": lambda a, b: a - b,
                  "*": lambda a, b: a * b,
                  "/": lambda a, b: a / b,
              }
              print(operations["*"](6, 7))
          '''),
          STEP("Read numbers safely", """
              Users make typos. Wrap the conversion in `try/except` and keep asking until the input is valid.""", '''
              def read_number(prompt):
                  while True:
                      try:
                          return float(input(prompt))
                      except ValueError:
                          print("  Please enter a valid number.")
          '''),
          STEP("Calculate with protection", """
              Catch `ZeroDivisionError` so the program never crashes, and return a friendly message instead.""", '''
              def calculate(a, op, b):
                  try:
                      return operations[op](a, b)
                  except ZeroDivisionError:
                      return "Error: division by zero"
          '''),
          STEP("Main loop and history", """
              Show a menu inside `while True`, store every result in a list and print the history when the user quits."""),
          STEP("Test it", """
              Run the full program in the terminal. Try invalid numbers, divide by zero, then quit with `q` to see your history."""),
      ],
      '''
      operations = {
          "+": lambda a, b: a + b,
          "-": lambda a, b: a - b,
          "*": lambda a, b: a * b,
          "/": lambda a, b: a / b,
          "**": lambda a, b: a ** b,
          "%": lambda a, b: a % b,
      }
      history = []


      def read_number(prompt):
          while True:
              try:
                  return float(input(prompt))
              except ValueError:
                  print("  Please enter a valid number.")


      def calculate(a, op, b):
          try:
              return operations[op](a, b)
          except ZeroDivisionError:
              return "Error: division by zero"


      def fmt(n):
          return int(n) if isinstance(n, float) and n.is_integer() else n


      print("=== Smart Calculator ===")
      print("Operations: + - * / ** %   (q to quit)")
      while True:
          op = input("\\nOperation: ").strip()
          if op == "q":
              break
          if op not in operations:
              print("  Unknown operation, try again.")
              continue
          a = read_number("First number: ")
          b = read_number("Second number: ")
          result = calculate(a, op, b)
          line = f"{fmt(a)} {op} {fmt(b)} = {fmt(result)}"
          history.append(line)
          print("  " + line)

      print("\\n--- History ---")
      for i, item in enumerate(history, 1):
          print(f"{i}. {item}")
      print("Goodbye! 👋")
      ''', stdin=["+", "12", "30", "/", "9", "0", "**", "two", "2", "10", "q"]),

    # 2 -------------------------------------------------------------------
    P("todo_list", 2, "To-Do List Manager", "Add, complete, delete and save tasks to a file",
      "project_02_todo", "Beginner", 40,
      """
      A productivity app every developer builds. Tasks are stored as dictionaries in a list, can be marked
      as done or removed, and are saved to a JSON file so they survive between runs.
      """,
      ["Add tasks with priority", "Mark tasks complete", "Delete tasks", "Persistent JSON storage", "Progress summary"],
      ["lists", "dictionaries", "JSON", "file handling", "functions", "match-case"],
      [
          STEP("Model a task", "Each task is a dictionary with a title, priority and done flag.", '''
              task = {"title": "Learn Python", "priority": "high", "done": False}
              print(task["title"], "-", task["priority"])
          '''),
          STEP("Load and save", "Use `json.dump` and `json.load` with a fallback when the file doesn't exist yet.", '''
              import json

              def load_tasks(path="tasks.json"):
                  try:
                      with open(path) as f:
                          return json.load(f)
                  except FileNotFoundError:
                      return []

              def save_tasks(tasks, path="tasks.json"):
                  with open(path, "w") as f:
                      json.dump(tasks, f, indent=2)
          '''),
          STEP("Show the list", "Print each task with its number, a checkbox and priority."),
          STEP("Handle commands", "Use `match` to react to add / done / delete / list / quit commands."),
          STEP("Run and test", "Add a few tasks, complete one, delete one and quit. Run again — your tasks are still there!"),
      ],
      '''
      import json

      PATH = "tasks.json"


      def load_tasks():
          try:
              with open(PATH) as f:
                  return json.load(f)
          except FileNotFoundError:
              return []


      def save_tasks(tasks):
          with open(PATH, "w") as f:
              json.dump(tasks, f, indent=2)


      def show(tasks):
          if not tasks:
              print("  (no tasks yet)")
              return
          for i, t in enumerate(tasks, 1):
              mark = "✔" if t["done"] else " "
              print(f"  {i}. [{mark}] {t['title']}  ({t['priority']})")
          done = sum(t["done"] for t in tasks)
          print(f"  Progress: {done}/{len(tasks)} done")


      def pick(tasks):
          try:
              n = int(input("  Task number: "))
              return tasks[n - 1] if 1 <= n <= len(tasks) else None
          except ValueError:
              return None


      tasks = load_tasks()
      print("=== To-Do Manager ===")
      print("Commands: add, done, delete, list, quit")
      while True:
          cmd = input("\\n> ").strip().lower()
          match cmd:
              case "add":
                  title = input("  Title: ").strip()
                  priority = input("  Priority (low/medium/high): ").strip() or "medium"
                  tasks.append({"title": title, "priority": priority, "done": False})
                  print("  Added ✅")
              case "done":
                  task = pick(tasks)
                  if task:
                      task["done"] = True
                      print("  Completed:", task["title"])
                  else:
                      print("  Invalid number")
              case "delete":
                  task = pick(tasks)
                  if task:
                      tasks.remove(task)
                      print("  Deleted:", task["title"])
                  else:
                      print("  Invalid number")
              case "list":
                  show(tasks)
              case "quit":
                  save_tasks(tasks)
                  print("Saved", len(tasks), "tasks. Bye!")
                  break
              case _:
                  print("  Unknown command")
      ''', stdin=["add", "Finish Python course", "high", "add", "Build portfolio", "medium",
                  "add", "Buy coffee", "low", "done", "1", "delete", "3", "list", "quit"]),

    # 3 -------------------------------------------------------------------
    P("quiz_game", 3, "Python Quiz Game", "A scored multiple-choice quiz with shuffled questions",
      "project_03_quiz_game", "Beginner", 35,
      """
      Build an interactive quiz that shuffles its questions, checks answers, keeps score and finishes
      with a grade. You'll practise data structures, randomness and clean output formatting.
      """,
      ["Question bank as a list of dictionaries", "Shuffled question order", "Instant feedback",
       "Score and percentage", "Grade at the end"],
      ["lists", "dictionaries", "random", "loops", "conditions", "f-strings"],
      [
          STEP("Create the question bank", "Store every question with its options and the correct letter.", '''
              questions = [
                  {"q": "Which keyword defines a function?",
                   "options": ["func", "def", "lambda", "fn"], "answer": "b"},
              ]
              print(len(questions), "question(s) ready")
          '''),
          STEP("Shuffle", "Use `random.shuffle()` so every game is different."),
          STEP("Ask and check", "Print the options with letters a–d, read the answer and compare it with the correct letter."),
          STEP("Grade the player", "Calculate the percentage and choose a grade with `if/elif`."),
      ],
      '''
      import random

      questions = [
          {"q": "Which keyword defines a function?", "options": ["func", "def", "lambda", "fn"], "answer": "b"},
          {"q": "What does len('python') return?", "options": ["5", "6", "7", "Error"], "answer": "b"},
          {"q": "Which type is immutable?", "options": ["list", "dict", "tuple", "set"], "answer": "c"},
          {"q": "What is 2 ** 3?", "options": ["6", "8", "9", "5"], "answer": "b"},
          {"q": "Which method adds to a list?", "options": ["add()", "push()", "append()", "insert_end()"], "answer": "c"},
      ]

      random.seed(42)
      random.shuffle(questions)
      score = 0
      print("=== Python Quiz ===")
      for number, item in enumerate(questions, 1):
          print(f"\\nQ{number}. {item['q']}")
          for letter, option in zip("abcd", item["options"]):
              print(f"   {letter}) {option}")
          answer = input("Your answer: ").strip().lower()
          if answer == item["answer"]:
              score += 1
              print("   ✅ Correct!")
          else:
              correct = item["options"]["abcd".index(item["answer"])]
              print(f"   ❌ Wrong — the answer is {item['answer']}) {correct}")

      percent = score / len(questions) * 100
      if percent >= 80:
          grade = "Excellent 🏆"
      elif percent >= 60:
          grade = "Good 👍"
      else:
          grade = "Keep practising 💪"
      print(f"\\nScore: {score}/{len(questions)} ({percent:.0f}%) — {grade}")
      ''', stdin=["c", "b", "b", "c", "a"]),

    # 4 -------------------------------------------------------------------
    P("password_generator", 4, "Password Generator", "Generate strong passwords and rate their strength",
      "project_04_password_generator", "Beginner", 30,
      """
      Security matters. This tool generates cryptographically secure passwords with the `secrets` module,
      lets the user choose which character sets to include, and rates password strength.
      """,
      ["Custom length", "Choose letters, digits and symbols", "Cryptographically secure randomness",
       "Guaranteed variety of characters", "Strength meter"],
      ["string module", "secrets", "functions", "lists", "conditions"],
      [
          STEP("Character pools", "The `string` module has ready-made pools of characters.", '''
              import string
              print(string.ascii_lowercase)
              print(string.digits)
              print(string.punctuation)
          '''),
          STEP("Secure random choice", "`secrets.choice` is safer than `random.choice` for passwords.", '''
              import secrets, string
              print("".join(secrets.choice(string.ascii_letters) for _ in range(12)))
          '''),
          STEP("Guarantee variety", "Pick at least one character from each selected pool, then fill and shuffle."),
          STEP("Rate strength", "Score length and character variety to label the password Weak, Medium or Strong."),
      ],
      '''
      import secrets
      import string


      def generate(length=16, digits=True, symbols=True):
          pools = [string.ascii_lowercase, string.ascii_uppercase]
          if digits:
              pools.append(string.digits)
          if symbols:
              pools.append("!@#$%^&*?-_")
          chars = [secrets.choice(pool) for pool in pools]
          everything = "".join(pools)
          chars += [secrets.choice(everything) for _ in range(length - len(chars))]
          secrets.SystemRandom().shuffle(chars)
          return "".join(chars)


      def strength(pw):
          score = 0
          score += len(pw) >= 8
          score += len(pw) >= 12
          score += any(c.islower() for c in pw)
          score += any(c.isupper() for c in pw)
          score += any(c.isdigit() for c in pw)
          score += any(not c.isalnum() for c in pw)
          return ["Very weak", "Very weak", "Weak", "Medium", "Good", "Strong", "Very strong"][score]


      print("=== Password Generator ===")
      length = int(input("Length (8-64): ") or 16)
      length = max(8, min(64, length))
      use_digits = input("Include digits? (y/n): ").lower() != "n"
      use_symbols = input("Include symbols? (y/n): ").lower() != "n"
      count = int(input("How many passwords? ") or 3)

      for i in range(count):
          pw = generate(length, use_digits, use_symbols)
          print(f"{i + 1}. {'*' * 4}{pw[4:]}  →  {strength(pw)}  (length {len(pw)})")

      print("\\nCheck your own password:")
      mine = input("Password: ")
      print("Strength:", strength(mine))
      ''', stdin=["16", "y", "y", "3", "hello123"]),

    # 5 -------------------------------------------------------------------
    P("weather_app", 5, "Weather Dashboard", "Parse weather data, convert units and show a forecast",
      "project_05_weather", "Beginner", 40,
      """
      Real apps receive weather as JSON from an API. Here you'll parse a realistic JSON response,
      convert units, compute averages and print a clean 5-day dashboard. The same code works with a
      live API such as OpenWeatherMap using `urllib.request`.
      """,
      ["Parse JSON weather data", "Celsius ↔ Fahrenheit conversion", "Weather icons by condition",
       "Daily min/max and averages", "Simple bar chart in the terminal"],
      ["JSON", "dictionaries", "functions", "loops", "string formatting"],
      [
          STEP("Understand the data", "APIs return JSON. `json.loads` turns it into Python dictionaries and lists.", '''
              import json
              raw = '{"city": "Dhaka", "days": [{"day": "Mon", "min": 24, "max": 31, "condition": "sunny"}]}'
              data = json.loads(raw)
              print(data["city"], data["days"][0]["max"])
          '''),
          STEP("Convert units", "Write small pure functions for conversions.", '''
              def c_to_f(c):
                  return c * 9 / 5 + 32
              print(c_to_f(30))
          '''),
          STEP("Pick icons", "Map each condition to an emoji with a dictionary and `.get()` for unknown values."),
          STEP("Build the dashboard", "Loop over the days, print aligned columns and a bar made of blocks for the max temperature."),
          STEP("Going live (optional)", """
              With internet access you could load real data:
              `urllib.request.urlopen(url).read()` then `json.loads(...)`. Keep your API key secret!"""),
      ],
      '''
      import json

      RAW = """{
        "city": "Dhaka",
        "country": "BD",
        "days": [
          {"day": "Mon", "min": 24, "max": 31, "condition": "sunny", "humidity": 62},
          {"day": "Tue", "min": 25, "max": 33, "condition": "cloudy", "humidity": 70},
          {"day": "Wed", "min": 23, "max": 29, "condition": "rain", "humidity": 88},
          {"day": "Thu", "min": 22, "max": 27, "condition": "storm", "humidity": 91},
          {"day": "Fri", "min": 24, "max": 32, "condition": "sunny", "humidity": 58}
        ]
      }"""

      ICONS = {"sunny": "☀️", "cloudy": "⛅", "rain": "🌧️", "storm": "⛈️"}


      def c_to_f(c):
          return c * 9 / 5 + 32


      data = json.loads(RAW)
      unit = input("Units — C or F? ").strip().upper()
      convert = c_to_f if unit == "F" else (lambda c: c)
      symbol = "°F" if unit == "F" else "°C"

      print(f"\\n5-day forecast for {data['city']}, {data['country']}")
      print(f"{'Day':<5}{'':<3}{'Min':>6}{'Max':>6}  {'Hum':>4}  Chart")
      for d in data["days"]:
          lo, hi = convert(d["min"]), convert(d["max"])
          bar = "█" * int(d["max"] - 20)
          icon = ICONS.get(d["condition"], "❔")
          print(f"{d['day']:<5}{icon:<3}{lo:>6.0f}{hi:>6.0f}  {d['humidity']:>3}%  {bar}")

      maxes = [d["max"] for d in data["days"]]
      hottest = max(data["days"], key=lambda d: d["max"])
      rainy = [d["day"] for d in data["days"] if d["condition"] in ("rain", "storm")]
      print(f"\\nAverage high: {convert(sum(maxes) / len(maxes)):.1f}{symbol}")
      print(f"Hottest day: {hottest['day']} ({convert(hottest['max']):.0f}{symbol})")
      print("Take an umbrella on:", ", ".join(rainy) if rainy else "no days")
      ''', stdin=["C"]),

    # 6 -------------------------------------------------------------------
    P("expense_tracker", 6, "Expense Tracker", "Track spending by category with reports and budgets",
      "project_06_expense_tracker", "Intermediate", 50,
      """
      Manage personal finances like a pro. Record expenses with categories, see totals per category,
      compare against a budget and export everything to CSV for a spreadsheet.
      """,
      ["Add expenses with category and note", "Totals per category", "Budget warnings",
       "Text bar chart report", "CSV export"],
      ["lists", "dictionaries", "csv module", "functions", "sorting", "f-strings"],
      [
          STEP("Store expenses", "A list of dictionaries is perfect for tabular data.", '''
              expenses = [{"amount": 12.5, "category": "food", "note": "lunch"}]
              expenses.append({"amount": 40, "category": "transport", "note": "fuel"})
              print(sum(e["amount"] for e in expenses))
          '''),
          STEP("Group by category", "Use a dictionary to accumulate totals per category.", '''
              totals = {}
              for e in [{"amount": 5, "category": "food"}, {"amount": 7, "category": "food"}]:
                  totals[e["category"]] = totals.get(e["category"], 0) + e["amount"]
              print(totals)
          '''),
          STEP("Draw a report", "Sort categories by amount and draw proportional bars."),
          STEP("Export to CSV", "Use `csv.DictWriter` to save the expenses for Excel or Google Sheets."),
      ],
      '''
      import csv

      BUDGET = 300.0
      expenses = []

      print("=== Expense Tracker ===")
      print("Enter expenses as: amount category note   (blank line to finish)")
      while True:
          line = input("> ").strip()
          if not line:
              break
          parts = line.split(maxsplit=2)
          try:
              amount = float(parts[0])
              category = parts[1].lower() if len(parts) > 1 else "other"
              note = parts[2] if len(parts) > 2 else ""
          except ValueError:
              print("  Format: 12.50 food lunch")
              continue
          expenses.append({"amount": amount, "category": category, "note": note})

      totals = {}
      for e in expenses:
          totals[e["category"]] = totals.get(e["category"], 0) + e["amount"]
      grand = sum(totals.values())

      print("\\n--- Spending by category ---")
      for cat, amount in sorted(totals.items(), key=lambda kv: kv[1], reverse=True):
          share = amount / grand if grand else 0
          print(f"{cat:<12}{amount:>8.2f}  {'■' * round(share * 20):<20} {share:.0%}")
      print(f"{'TOTAL':<12}{grand:>8.2f}")

      left = BUDGET - grand
      if left < 0:
          print(f"⚠️  Over budget by {-left:.2f}!")
      else:
          print(f"✅ {left:.2f} left of your {BUDGET:.0f} budget")

      with open("expenses.csv", "w", newline="") as f:
          writer = csv.DictWriter(f, fieldnames=["amount", "category", "note"])
          writer.writeheader()
          writer.writerows(expenses)
      print(f"Exported {len(expenses)} expenses to expenses.csv")
      ''', stdin=["12.5 food lunch", "45 transport fuel", "120 shopping shoes", "8.75 food coffee & cake",
                  "abc", "60 bills internet", ""]),

    # 7 -------------------------------------------------------------------
    P("contact_book", 7, "Contact Book", "A searchable address book with validation and JSON storage",
      "project_07_contact_book", "Intermediate", 50,
      """
      Create a contact manager with full CRUD (create, read, update, delete). Contacts are validated,
      searchable by any part of the name, sorted alphabetically and saved to a JSON file.
      """,
      ["Add contacts with phone and email", "Input validation", "Search by name", "Update and delete",
       "Alphabetical listing", "JSON persistence"],
      ["classes", "dictionaries", "JSON", "string methods", "validation"],
      [
          STEP("Design a class", "A `ContactBook` class keeps data and behaviour together.", '''
              class ContactBook:
                  def __init__(self):
                      self.contacts = {}

                  def add(self, name, phone, email):
                      self.contacts[name] = {"phone": phone, "email": email}

              book = ContactBook()
              book.add("Ali", "01711-000000", "ali@mail.com")
              print(book.contacts)
          '''),
          STEP("Validate input", "Check phone numbers contain only digits/dashes and emails contain `@` and a dot.", '''
              def valid_phone(p):
                  return p.replace("-", "").replace("+", "").isdigit() and len(p) >= 7

              print(valid_phone("01711-000000"), valid_phone("abc"))
          '''),
          STEP("Search", "Lowercase both sides so search is case-insensitive."),
          STEP("Persist", "Save with `json.dump` after every change and load on start."),
      ],
      '''
      import json


      class ContactBook:
          def __init__(self, path="contacts.json"):
              self.path = path
              try:
                  with open(path) as f:
                      self.contacts = json.load(f)
              except FileNotFoundError:
                  self.contacts = {}

          def save(self):
              with open(self.path, "w") as f:
                  json.dump(self.contacts, f, indent=2)

          @staticmethod
          def valid_phone(phone):
              digits = phone.replace("-", "").replace("+", "").replace(" ", "")
              return digits.isdigit() and len(digits) >= 7

          @staticmethod
          def valid_email(email):
              return "@" in email and "." in email.split("@")[-1]

          def add(self, name, phone, email):
              if not self.valid_phone(phone):
                  return "Invalid phone number"
              if email and not self.valid_email(email):
                  return "Invalid email"
              self.contacts[name.title()] = {"phone": phone, "email": email}
              self.save()
              return f"Saved {name.title()}"

          def search(self, text):
              text = text.lower()
              return {n: c for n, c in self.contacts.items() if text in n.lower()}

          def delete(self, name):
              if self.contacts.pop(name.title(), None) is None:
                  return False
              self.save()
              return True

          def show(self, items=None):
              items = self.contacts if items is None else items
              if not items:
                  print("  (nothing found)")
              for name in sorted(items):
                  c = items[name]
                  print(f"  {name:<15}{c['phone']:<16}{c['email']}")


      book = ContactBook()
      print("=== Contact Book ===  (add / search / delete / list / quit)")
      while True:
          cmd = input("\\n> ").strip().lower()
          if cmd == "add":
              print(" ", book.add(input("  Name: "), input("  Phone: "), input("  Email: ")))
          elif cmd == "search":
              book.show(book.search(input("  Search: ")))
          elif cmd == "delete":
              print("  Deleted" if book.delete(input("  Name: ")) else "  Not found")
          elif cmd == "list":
              book.show()
          elif cmd == "quit":
              print(f"{len(book.contacts)} contacts saved. Bye!")
              break
      ''', stdin=["add", "sara khan", "+880 1711-223344", "sara@mail.com",
                  "add", "john doe", "12ab", "john@mail.com",
                  "add", "ali hasan", "01811-998877", "ali@work.io",
                  "add", "mia", "01911-445566", "",
                  "search", "an", "delete", "mia", "list", "quit"]),

    # 8 -------------------------------------------------------------------
    P("number_guess", 8, "Number Guessing Game", "Guess the secret number with hints and difficulty levels",
      "project_08_number_guess", "Beginner", 25,
      """
      A classic game that teaches loops, randomness and game logic. Choose a difficulty, get "higher/lower"
      hints, see how close you are, and try to beat your best score.
      """,
      ["Three difficulty levels", "Higher / lower hints", "Hot & cold distance hints", "Limited attempts", "Play again"],
      ["random", "while loops", "conditions", "functions", "dictionaries"],
      [
          STEP("Pick a secret number", "`random.randint(a, b)` includes both ends.", '''
              import random
              secret = random.randint(1, 100)
              print("I picked a number!")
          '''),
          STEP("Loop with hints", "Compare each guess with the secret and print a hint."),
          STEP("Add difficulty", "Store the range and attempts for each level in a dictionary."),
          STEP("Play again", "Wrap the game in a function and ask the player whether to play another round."),
      ],
      '''
      import random

      LEVELS = {"easy": (50, 10), "medium": (100, 7), "hard": (500, 9)}


      def hint(guess, secret):
          distance = abs(guess - secret)
          if distance <= 3:
              return "🔥 Very hot!"
          if distance <= 10:
              return "♨️ Warm"
          return "❄️ Cold"


      def play(level, rng):
          top, attempts = LEVELS[level]
          secret = rng.randint(1, top)
          print(f"\\nI'm thinking of a number between 1 and {top}. You have {attempts} tries.")
          for attempt in range(1, attempts + 1):
              try:
                  guess = int(input(f"Guess #{attempt}: "))
              except ValueError:
                  print("  Numbers only!")
                  continue
              if guess == secret:
                  print(f"  🎉 Correct! You got it in {attempt} tries.")
                  return attempt
              direction = "higher ⬆️" if guess < secret else "lower ⬇️"
              print(f"  Go {direction}  {hint(guess, secret)}")
          print(f"  💀 Out of tries! The number was {secret}.")
          return None


      rng = random.Random(2026)
      best = None
      print("=== Number Guessing Game ===")
      while True:
          level = input("Difficulty (easy/medium/hard): ").strip().lower()
          if level not in LEVELS:
              level = "easy"
          result = play(level, rng)
          if result and (best is None or result < best):
              best = result
              print("  🏆 New best score!")
          if input("Play again? (y/n): ").lower() != "y":
              break
      print("Best score:", best if best else "-")
      ''', stdin=["easy", "25", "12", "5", "7", "8", "n"]),

    # 9 -------------------------------------------------------------------
    P("file_organizer", 9, "Smart File Organizer", "Automatically sort files into folders by type",
      "project_09_file_organizer", "Intermediate", 45,
      """
      Automation is one of Python's superpowers. This script scans a messy folder and moves every file
      into a sub-folder based on its extension (Images, Documents, Music…), then prints a report.
      It works on a demo folder it creates itself, so it's safe to run.
      """,
      ["Creates a demo messy folder", "Maps extensions to categories", "Moves files with shutil",
       "Handles duplicate names", "Summary report with counts"],
      ["pathlib", "shutil", "dictionaries", "loops", "automation"],
      [
          STEP("Explore a folder", "`Path.iterdir()` lists everything inside a folder.", '''
              from pathlib import Path
              for item in Path(".").iterdir():
                  print(item.name)
          '''),
          STEP("Map extensions", "A dictionary from category to extensions keeps rules readable.", '''
              CATEGORIES = {"Images": {".jpg", ".png"}, "Documents": {".pdf", ".txt"}}
              def category_for(ext):
                  for name, exts in CATEGORIES.items():
                      if ext.lower() in exts:
                          return name
                  return "Other"
              print(category_for(".PNG"))
          '''),
          STEP("Move files", "`shutil.move()` moves a file. Create the destination with `mkdir(exist_ok=True)`."),
          STEP("Report", "Count how many files went into each folder and print a tidy summary."),
      ],
      '''
      import shutil
      from pathlib import Path

      CATEGORIES = {
          "Images": {".jpg", ".jpeg", ".png", ".gif", ".webp"},
          "Documents": {".pdf", ".docx", ".txt", ".md", ".xlsx"},
          "Music": {".mp3", ".wav", ".flac"},
          "Videos": {".mp4", ".mkv", ".mov"},
          "Code": {".py", ".kt", ".js", ".html", ".css"},
          "Archives": {".zip", ".rar", ".7z"},
      }


      def category_for(path):
          ext = path.suffix.lower()
          for name, extensions in CATEGORIES.items():
              if ext in extensions:
                  return name
          return "Other"


      def make_demo(folder):
          if folder.exists():
              shutil.rmtree(folder)
          folder.mkdir()
          names = ["holiday.jpg", "cv.pdf", "song.mp3", "app.py", "notes.txt", "clip.mp4",
                   "logo.PNG", "backup.zip", "budget.xlsx", "random.xyz", "index.html", "photo.jpeg"]
          for name in names:
              (folder / name).write_text("demo")
          return len(names)


      def organize(folder):
          report = {}
          for item in sorted(folder.iterdir()):
              if item.is_dir():
                  continue
              category = category_for(item)
              target_dir = folder / category
              target_dir.mkdir(exist_ok=True)
              target = target_dir / item.name
              counter = 1
              while target.exists():
                  target = target_dir / f"{item.stem}_{counter}{item.suffix}"
                  counter += 1
              shutil.move(str(item), str(target))
              report.setdefault(category, []).append(item.name)
          return report


      folder = Path("Downloads_demo")
      count = make_demo(folder)
      print(f"Created a messy folder with {count} files")
      input("Press enter to organize it... ")
      report = organize(folder)
      print("\\n=== Organized ===")
      for category in sorted(report):
          files = report[category]
          print(f"📁 {category:<10} {len(files)} file(s): {', '.join(files)}")
      print("\\nFolders now:", ", ".join(sorted(p.name for p in folder.iterdir())))
      ''', stdin=[""]),

    # 10 ------------------------------------------------------------------
    P("web_scraper", 10, "Web Scraper", "Extract headlines and links from HTML with html.parser",
      "project_10_web_scraper", "Intermediate", 50,
      """
      Web scraping turns web pages into data. Using only the standard library's `html.parser`, you'll
      extract article titles, links and prices from an HTML page, then save the results as CSV.
      The demo uses an embedded page so it runs offline; with internet you'd fetch real pages with `urllib`.
      """,
      ["Custom HTMLParser subclass", "Extract headlines, links and prices", "Clean and structure data",
       "Sort and filter results", "Save as CSV"],
      ["classes", "inheritance", "html.parser", "csv", "string methods"],
      [
          STEP("Understand HTML", "Pages are made of tags like `<h2 class=\"title\">`. We look for specific tags and attributes."),
          STEP("Subclass HTMLParser", "Override `handle_starttag`, `handle_data` and `handle_endtag`.", '''
              from html.parser import HTMLParser

              class TitleParser(HTMLParser):
                  def handle_starttag(self, tag, attrs):
                      print("start:", tag, dict(attrs))

              TitleParser().feed('<h2 class="title">Hello</h2>')
          '''),
          STEP("Collect data", "Track when you're inside an interesting tag and store its text."),
          STEP("Save results", "Write the scraped rows into a CSV file."),
          STEP("Real websites (optional)", """
              `urllib.request.urlopen(url).read().decode()` downloads a page. Always respect a site's terms and robots.txt."""),
      ],
      '''
      import csv
      from html.parser import HTMLParser

      PAGE = """
      <html><body>
        <div class="product"><h2 class="title">Mechanical Keyboard</h2>
          <span class="price">$89.99</span><a href="/p/keyboard">View</a></div>
        <div class="product"><h2 class="title">Wireless Mouse</h2>
          <span class="price">$24.50</span><a href="/p/mouse">View</a></div>
        <div class="product"><h2 class="title">4K Monitor</h2>
          <span class="price">$329.00</span><a href="/p/monitor">View</a></div>
        <div class="product"><h2 class="title">USB-C Hub</h2>
          <span class="price">$39.90</span><a href="/p/hub">View</a></div>
      </body></html>
      """


      class ProductParser(HTMLParser):
          def __init__(self):
              super().__init__()
              self.products = []
              self.field = None

          def handle_starttag(self, tag, attrs):
              attrs = dict(attrs)
              if tag == "div" and attrs.get("class") == "product":
                  self.products.append({})
              elif tag == "h2" and attrs.get("class") == "title":
                  self.field = "title"
              elif tag == "span" and attrs.get("class") == "price":
                  self.field = "price"
              elif tag == "a" and self.products:
                  self.products[-1]["link"] = "https://shop.example.com" + attrs.get("href", "")

          def handle_data(self, data):
              if self.field and data.strip():
                  value = data.strip()
                  if self.field == "price":
                      value = float(value.replace("$", ""))
                  self.products[-1][self.field] = value
                  self.field = None


      parser = ProductParser()
      parser.feed(PAGE)
      products = parser.products
      print(f"Scraped {len(products)} products\\n")

      limit = float(input("Show products under price: $") or 1000)
      cheap = sorted((p for p in products if p["price"] <= limit), key=lambda p: p["price"])
      for p in cheap:
          print(f"{p['title']:<22}${p['price']:>8.2f}  {p['link']}")

      with open("products.csv", "w", newline="") as f:
          writer = csv.DictWriter(f, fieldnames=["title", "price", "link"])
          writer.writeheader()
          writer.writerows(products)
      print("\\nSaved all products to products.csv")
      print(f"Average price: ${sum(p['price'] for p in products) / len(products):.2f}")
      ''', stdin=["100"]),

    # 11 ------------------------------------------------------------------
    P("chatbot", 11, "Rule-Based Chatbot", "A friendly assistant that understands intents and remembers you",
      "project_11_chatbot", "Intermediate", 45,
      """
      Before AI models, chatbots used rules — and many support bots still do. You'll build an assistant that
      detects intents from keywords, remembers the user's name, answers Python questions, tells jokes
      and tracks the conversation.
      """,
      ["Keyword-based intent detection", "Remembers the user's name", "Python tips and jokes",
       "Simple maths answers", "Conversation summary on exit"],
      ["dictionaries", "string methods", "functions", "random", "loops"],
      [
          STEP("Define intents", "Map each intent to trigger keywords.", '''
              INTENTS = {"greet": ["hello", "hi", "hey"], "bye": ["bye", "exit"]}
              def detect(text):
                  for intent, words in INTENTS.items():
                      if any(w in text.lower().split() for w in words):
                          return intent
                  return "unknown"
              print(detect("Hey there"))
          '''),
          STEP("Respond", "Pick a random reply for the detected intent so the bot feels natural."),
          STEP("Memory", "Store facts like the user's name in a dictionary and use them in replies."),
          STEP("Main loop", "Read messages until the user says bye, then print a summary."),
      ],
      '''
      import random
      import re

      rng = random.Random(7)
      memory = {"name": None, "messages": 0}

      INTENTS = {
          "greet": ["hello", "hi", "hey", "salam"],
          "name": ["my name is", "i am", "i'm"],
          "python": ["python", "learn", "tip"],
          "joke": ["joke", "funny", "laugh"],
          "math": ["calculate", "what is"],
          "thanks": ["thanks", "thank"],
          "bye": ["bye", "exit", "quit"],
      }
      TIPS = [
          "Use f-strings: f\\"Hello {name}\\" is clean and fast.",
          "List comprehensions make loops shorter: [x*x for x in nums].",
          "Use enumerate() when you need the index and the value.",
      ]
      JOKES = [
          "Why do Python devs wear glasses? Because they can't C. 😄",
          "I told my code a joke… it didn't get it. No sense of humor(), only syntax.",
      ]


      def detect(text):
          lower = text.lower()
          for intent, triggers in INTENTS.items():
              if any(t in lower for t in triggers):
                  return intent
          return "unknown"


      def reply(text):
          intent = detect(text)
          name = memory["name"] or "friend"
          if intent == "greet":
              return rng.choice([f"Hello {name}! 👋", f"Hey {name}, how can I help?"])
          if intent == "name":
              memory["name"] = re.split(r"my name is|i am|i'm", text, flags=re.I)[-1].strip().title()
              return f"Nice to meet you, {memory['name']}!"
          if intent == "math":
              expr = re.sub(r"[^0-9+\\-*/(). ]", "", text)
              try:
                  return f"That equals {eval(expr, {'__builtins__': {}})}"
              except Exception:
                  return "I couldn't calculate that."
          if intent == "python":
              return "💡 " + rng.choice(TIPS)
          if intent == "joke":
              return rng.choice(JOKES)
          if intent == "thanks":
              return f"You're welcome, {name}!"
          if intent == "bye":
              return None
          return "Hmm, I'm still learning. Try asking for a Python tip or a joke!"


      print("🤖 PyBot: Hi! I'm PyBot. Type 'bye' to leave.")
      while True:
          message = input("You: ").strip()
          if not message:
              continue
          memory["messages"] += 1
          answer = reply(message)
          if answer is None:
              print(f"🤖 PyBot: Goodbye {memory['name'] or 'friend'}! We exchanged {memory['messages']} messages.")
              break
          print("🤖 PyBot:", answer)
      ''', stdin=["hello", "my name is saif", "give me a python tip", "tell me a joke",
                  "what is 12 * (3 + 4)", "thanks!", "bye"]),

    # 12 ------------------------------------------------------------------
    P("library_system", 12, "Library Management System", "Manage books, members, loans and late fees with OOP",
      "project_12_library_system", "Advanced", 60,
      """
      A complete object-oriented system: `Book`, `Member` and `Library` classes work together to handle
      borrowing, returning, availability and late fees. This is the kind of design you'll see in real
      back-end projects and job interviews.
      """,
      ["Book and Member classes", "Borrow and return with rules", "Borrow limit per member",
       "Late fee calculation with dates", "Custom exceptions", "Reports"],
      ["classes", "dataclasses", "custom exceptions", "datetime", "dictionaries"],
      [
          STEP("Model the data", "Use `@dataclass` for simple data classes.", '''
              from dataclasses import dataclass

              @dataclass
              class Book:
                  isbn: str
                  title: str
                  author: str
                  available: bool = True

              print(Book("111", "Clean Code", "R. Martin"))
          '''),
          STEP("Custom errors", "Create `LibraryError` so callers can catch library problems specifically."),
          STEP("Borrow & return", "The `Library` class enforces rules: availability, limits, due dates."),
          STEP("Late fees", "Subtract dates with `datetime` to count overdue days and multiply by the daily fee."),
          STEP("Reports", "List available books and each member's loans."),
      ],
      '''
      from dataclasses import dataclass, field
      from datetime import date, timedelta

      DAILY_FEE = 0.5
      LOAN_DAYS = 14
      MAX_LOANS = 2


      class LibraryError(Exception):
          pass


      @dataclass
      class Book:
          isbn: str
          title: str
          author: str
          available: bool = True


      @dataclass
      class Member:
          member_id: int
          name: str
          loans: dict = field(default_factory=dict)  # isbn -> due date
          fees: float = 0.0


      class Library:
          def __init__(self):
              self.books = {}
              self.members = {}

          def add_book(self, book):
              self.books[book.isbn] = book

          def join(self, name):
              member = Member(len(self.members) + 1, name)
              self.members[member.member_id] = member
              return member

          def borrow(self, member_id, isbn, today):
              member, book = self.members[member_id], self.books.get(isbn)
              if book is None:
                  raise LibraryError("No such book")
              if not book.available:
                  raise LibraryError(f"'{book.title}' is already on loan")
              if len(member.loans) >= MAX_LOANS:
                  raise LibraryError(f"{member.name} reached the limit of {MAX_LOANS} books")
              book.available = False
              member.loans[isbn] = today + timedelta(days=LOAN_DAYS)
              return member.loans[isbn]

          def give_back(self, member_id, isbn, today):
              member = self.members[member_id]
              due = member.loans.pop(isbn)
              self.books[isbn].available = True
              late_days = max(0, (today - due).days)
              fee = late_days * DAILY_FEE
              member.fees += fee
              return late_days, fee

          def report(self):
              print("\\n📚 Books")
              for b in self.books.values():
                  status = "available" if b.available else "on loan"
                  print(f"  {b.title:<28}{b.author:<18}{status}")
              print("👥 Members")
              for m in self.members.values():
                  titles = [self.books[i].title for i in m.loans]
                  print(f"  #{m.member_id} {m.name:<8} loans: {titles or '-'}  fees: ${m.fees:.2f}")


      lib = Library()
      for b in [Book("111", "Clean Code", "Robert Martin"), Book("222", "Fluent Python", "Luciano Ramalho"),
                Book("333", "Automate the Boring Stuff", "Al Sweigart"), Book("444", "Python Crash Course", "Eric Matthes")]:
          lib.add_book(b)
      ali = lib.join("Ali")
      sara = lib.join(input("New member name: ").strip().title() or "Sara")

      start = date(2026, 3, 1)
      print("Ali borrows Clean Code, due", lib.borrow(ali.member_id, "111", start))
      print("Ali borrows Fluent Python, due", lib.borrow(ali.member_id, "222", start))
      for attempt in [(ali.member_id, "333"), (sara.member_id, "111"), (sara.member_id, "999")]:
          try:
              lib.borrow(*attempt, start)
          except LibraryError as e:
              print("⚠️ ", e)
      print(f"{sara.name} borrows Python Crash Course, due", lib.borrow(sara.member_id, "444", start))

      late, fee = lib.give_back(ali.member_id, "111", start + timedelta(days=20))
      print(f"Ali returned Clean Code {late} days late → fee ${fee:.2f}")
      lib.report()
      ''', stdin=["sara"]),

    # 13 ------------------------------------------------------------------
    P("bank_system", 13, "Banking System", "Accounts, transfers, interest and statements with inheritance",
      "project_13_bank_system", "Advanced", 60,
      """
      Model a small bank with an `Account` base class and `SavingsAccount` / `CheckingAccount` subclasses.
      You'll implement deposits, withdrawals with rules, transfers, monthly interest, overdraft limits,
      a transaction log and printable statements — a showcase of OOP done right.
      """,
      ["Base and derived account classes", "Deposits, withdrawals and transfers", "Overdraft limit for checking",
       "Monthly interest for savings", "Transaction history and statements", "Validation with exceptions"],
      ["inheritance", "polymorphism", "properties", "exceptions", "class attributes"],
      [
          STEP("Base Account", "Store the owner, balance and a list of transactions. Expose `balance` as a read-only property.", '''
              class Account:
                  def __init__(self, owner, balance=0):
                      self.owner = owner
                      self._balance = balance

                  @property
                  def balance(self):
                      return self._balance

              print(Account("Saif", 50).balance)
          '''),
          STEP("Rules with exceptions", "Raise `ValueError` for invalid amounts and insufficient funds."),
          STEP("Specialize", "Override `can_withdraw` in `CheckingAccount` to allow an overdraft."),
          STEP("Interest", "`SavingsAccount.add_interest()` adds the monthly rate to the balance."),
          STEP("Statement", "Print every transaction with a running balance."),
      ],
      '''
      class Account:
          bank_name = "PyBank"
          _next_number = 1000

          def __init__(self, owner, opening=0.0):
              Account._next_number += 1
              self.number = Account._next_number
              self.owner = owner
              self._balance = 0.0
              self.history = []
              if opening:
                  self.deposit(opening, "Opening deposit")

          @property
          def balance(self):
              return self._balance

          def _record(self, kind, amount, note):
              self.history.append((kind, amount, self._balance, note))

          def deposit(self, amount, note="Deposit"):
              if amount <= 0:
                  raise ValueError("Deposit must be positive")
              self._balance += amount
              self._record("IN", amount, note)

          def can_withdraw(self, amount):
              return amount <= self._balance

          def withdraw(self, amount, note="Withdrawal"):
              if amount <= 0:
                  raise ValueError("Amount must be positive")
              if not self.can_withdraw(amount):
                  raise ValueError(f"Insufficient funds in #{self.number}")
              self._balance -= amount
              self._record("OUT", amount, note)

          def transfer(self, other, amount):
              self.withdraw(amount, f"Transfer to #{other.number}")
              other.deposit(amount, f"Transfer from #{self.number}")

          def statement(self):
              print(f"\\n{self.bank_name} | {type(self).__name__} #{self.number} | {self.owner}")
              for kind, amount, after, note in self.history:
                  sign = "+" if kind == "IN" else "-"
                  print(f"  {note:<24}{sign}{amount:>9.2f}{after:>11.2f}")
              print(f"  {'Balance':<24}{'':>10}{self._balance:>11.2f}")


      class SavingsAccount(Account):
          def __init__(self, owner, opening=0.0, rate=0.04):
              super().__init__(owner, opening)
              self.rate = rate

          def add_interest(self):
              interest = round(self._balance * self.rate / 12, 2)
              self.deposit(interest, "Monthly interest")
              return interest


      class CheckingAccount(Account):
          def __init__(self, owner, opening=0.0, overdraft=200.0):
              super().__init__(owner, opening)
              self.overdraft = overdraft

          def can_withdraw(self, amount):
              return amount <= self._balance + self.overdraft


      owner = input("Customer name: ").strip().title()
      checking = CheckingAccount(owner, 500)
      savings = SavingsAccount(owner, 2000)

      actions = [
          ("Pay rent", lambda: checking.withdraw(650, "Rent")),
          ("Salary", lambda: checking.deposit(1800, "Salary")),
          ("Save money", lambda: checking.transfer(savings, 700)),
          ("Big purchase", lambda: checking.withdraw(5000, "Laptop")),
          ("Negative deposit", lambda: savings.deposit(-5)),
      ]
      for label, action in actions:
          try:
              action()
              print(f"✅ {label}")
          except ValueError as e:
              print(f"❌ {label}: {e}")

      print(f"💰 Interest earned: {savings.add_interest():.2f}")
      for acc in (checking, savings):
          acc.statement()
      total = sum(a.balance for a in (checking, savings))
      print(f"\\nTotal wealth for {owner}: {total:,.2f}")
      ''', stdin=["saif ahmed"]),

    # 14 ------------------------------------------------------------------
    P("student_grades", 14, "Student Grade Analyzer", "Statistics, rankings and report cards from class data",
      "project_14_student_grades", "Intermediate", 50,
      """
      Teachers and schools love automation. Load a class's marks from CSV text, compute averages, letter grades,
      rankings and subject statistics, find top performers and print beautiful report cards.
      """,
      ["Parse CSV data", "Average and letter grade per student", "Class ranking", "Per-subject statistics",
       "Distribution chart", "Individual report card lookup"],
      ["csv", "statistics", "dictionaries", "sorting", "functions", "f-strings"],
      [
          STEP("Read the data", "`csv.DictReader` turns each row into a dictionary keyed by the header.", '''
              import csv, io
              data = "name,math\\nAli,90\\nSara,75\\n"
              for row in csv.DictReader(io.StringIO(data)):
                  print(row["name"], int(row["math"]))
          '''),
          STEP("Grade function", "Convert an average into A–F using `if/elif`."),
          STEP("Statistics", "`statistics.mean`, `median` and `stdev` describe each subject."),
          STEP("Ranking", "Sort students by average with `sorted(..., key=..., reverse=True)`."),
          STEP("Report card", "Look up a student by name and print every subject with a bar."),
      ],
      '''
      import csv
      import io
      import statistics

      DATA = """name,math,physics,english,programming
      Ali,88,79,92,95
      Sara,95,91,85,98
      John,72,65,80,70
      Mia,60,58,75,66
      Omar,91,87,70,89
      Lina,45,52,61,58
      """

      SUBJECTS = ["math", "physics", "english", "programming"]


      def letter(avg):
          for limit, grade in [(90, "A"), (80, "B"), (70, "C"), (60, "D")]:
              if avg >= limit:
                  return grade
          return "F"


      students = []
      for row in csv.DictReader(io.StringIO(DATA)):
          marks = {s: int(row[s]) for s in SUBJECTS}
          avg = statistics.mean(marks.values())
          students.append({"name": row["name"], "marks": marks, "avg": avg, "grade": letter(avg)})

      ranking = sorted(students, key=lambda s: s["avg"], reverse=True)
      print("🏆 Class ranking")
      for pos, s in enumerate(ranking, 1):
          medal = {1: "🥇", 2: "🥈", 3: "🥉"}.get(pos, "  ")
          print(f"{medal} {pos}. {s['name']:<6} {s['avg']:6.2f}  {s['grade']}")

      print("\\n📊 Subject statistics")
      print(f"{'Subject':<13}{'Mean':>7}{'Median':>8}{'StDev':>7}{'Top':>6}")
      for subject in SUBJECTS:
          scores = [s["marks"][subject] for s in students]
          print(f"{subject:<13}{statistics.mean(scores):>7.1f}{statistics.median(scores):>8.1f}"
                f"{statistics.stdev(scores):>7.1f}{max(scores):>6}")

      print("\\n📈 Grade distribution")
      for g in "ABCDF":
          count = sum(s["grade"] == g for s in students)
          print(f"  {g}: {'█' * count} {count}")

      name = input("\\nReport card for: ").strip().title()
      student = next((s for s in students if s["name"] == name), None)
      if student:
          print(f"\\n— Report card: {student['name']} —")
          for subject, mark in student["marks"].items():
              print(f"  {subject:<12}{mark:>4}  {'▇' * (mark // 10)}")
          print(f"  Average {student['avg']:.1f}  →  Grade {student['grade']}")
      else:
          print("Student not found")
      ''', stdin=["sara"]),

    # 15 ------------------------------------------------------------------
    P("snake_game", 15, "Terminal Snake Game", "The classic Snake game on a text grid, controlled with W A S D",
      "project_15_snake_game", "Advanced", 70,
      """
      The grand finale! Build the classic Snake game on a text grid. The snake moves with W/A/S/D commands,
      eats food to grow, and the game ends when it hits a wall or itself. You'll combine data structures
      (deque), coordinates, game state, randomness and rendering — everything you've learned.
      """,
      ["Grid rendering with emoji", "Snake stored as a deque of coordinates", "W/A/S/D movement (several moves per turn)",
       "Food spawning and growing", "Wall and self-collision detection", "Score and game-over screen"],
      ["collections.deque", "tuples", "sets", "functions", "game loop", "random"],
      [
          STEP("Represent the snake", "A `deque` of `(row, col)` tuples — add a new head at the front, remove the tail at the back.", '''
              from collections import deque
              snake = deque([(2, 2), (2, 1), (2, 0)])
              snake.appendleft((2, 3))
              snake.pop()
              print(list(snake))
          '''),
          STEP("Draw the board", "Loop over rows and columns and choose a character for each cell.", '''
              def draw(snake, food, size=5):
                  for r in range(size):
                      print("".join("🟩" if (r, c) in snake else "🍎" if (r, c) == food else "⬛" for c in range(size)))
              draw([(1, 1), (1, 2)], (3, 3))
          '''),
          STEP("Move", "Map W/A/S/D to direction vectors and compute the next head position."),
          STEP("Collisions & food", "End the game on walls or self-collision. Grow when the head reaches the food."),
          STEP("Game loop", "Read a string like `ddds` each turn so you can make several moves at once, and type `q` to quit."),
      ],
      '''
      import random
      from collections import deque

      SIZE = 8
      MOVES = {"w": (-1, 0), "s": (1, 0), "a": (0, -1), "d": (0, 1)}
      rng = random.Random(11)


      def spawn_food(snake):
          free = [(r, c) for r in range(SIZE) for c in range(SIZE) if (r, c) not in snake]
          return rng.choice(free)


      def draw(snake, food, score):
          body = set(snake)
          print(f"\\nScore: {score}   Length: {len(snake)}")
          for r in range(SIZE):
              row = ""
              for c in range(SIZE):
                  if (r, c) == snake[0]:
                      row += "🐍"
                  elif (r, c) in body:
                      row += "🟩"
                  elif (r, c) == food:
                      row += "🍎"
                  else:
                      row += "⬛"
              print(row)


      snake = deque([(4, 2), (4, 1), (4, 0)])
      direction = MOVES["d"]
      food = spawn_food(snake)
      score = 0
      alive = True

      print("=== SNAKE ===  Move with w/a/s/d (e.g. 'ddw'), q to quit")
      draw(snake, food, score)
      while alive:
          keys = input("Moves: ").strip().lower()
          if keys == "q":
              break
          for key in keys or "x":
              if key in MOVES:
                  dr, dc = MOVES[key]
                  if (dr, dc) != (-direction[0], -direction[1]):
                      direction = (dr, dc)
              head = (snake[0][0] + direction[0], snake[0][1] + direction[1])
              hit_wall = not (0 <= head[0] < SIZE and 0 <= head[1] < SIZE)
              if hit_wall or head in list(snake)[:-1]:
                  alive = False
                  print("💥 Crash! " + ("You hit the wall." if hit_wall else "You bit yourself."))
                  break
              snake.appendleft(head)
              if head == food:
                  score += 10
                  food = spawn_food(snake)
                  print("😋 Yum! +10")
              else:
                  snake.pop()
          draw(snake, food, score)

      print(f"\\nGAME OVER — final score {score}, length {len(snake)}")
      ''', stdin=["dwd", "ssssa", "s"]),
]

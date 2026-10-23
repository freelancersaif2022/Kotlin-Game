from dsl import C, H, L, LI, M, Q, T, TIP

# ---------------------------------------------------------------------------
# Module 9 — File handling
# ---------------------------------------------------------------------------
m9 = M("m09", 9, "File Handling", "Read and write files, CSV, JSON and paths", "module_09_files", [
    L("m09l1", "Writing files",
      "Save data permanently by writing it to a file.", [
        T("`open(filename, mode)` opens a file. Mode `\"w\"` **writes** (creating or overwriting the file), "
          "`\"a\"` **appends** to the end."),
        C('''
            with open("notes.txt", "w") as f:
                f.write("Python notes\\n")
                f.write("1. Variables\\n")
                f.write("2. Loops\\n")
            print("File written!")
        '''),
        H("Appending"),
        C('''
            with open("notes.txt", "w") as f:
                f.write("Day 1: started Python\\n")
            with open("notes.txt", "a") as f:
                f.write("Day 2: learned loops\\n")
            with open("notes.txt") as f:
                print(f.read())
        '''),
        TIP("In the PyMaster terminal files are saved in a private sandbox folder inside the app."),
    ], keys=["`open(name, \"w\")` writes, `\"a\"` appends, `\"r\"` reads.",
             "`write()` doesn't add new lines — include `\\n` yourself."],
      practice="Write a file `todo.txt` with three tasks, one per line.", minutes=5),

    L("m09l2", "Reading files",
      "Load text back from a file in different ways.", [
        C('''
            with open("fruits.txt", "w") as f:
                f.write("apple\\nbanana\\ncherry\\n")

            with open("fruits.txt") as f:
                content = f.read()
            print(repr(content))

            with open("fruits.txt") as f:
                for line in f:
                    print("->", line.strip())

            with open("fruits.txt") as f:
                lines = f.readlines()
            print(lines)
        '''),
        LI("`read()` — the whole file as one string.",
           "`for line in f` — one line at a time (memory-friendly for big files).",
           "`readlines()` — a list of all lines."),
        H("Missing files"),
        C('''
            open("does_not_exist.txt")
        '''),
    ], keys=["`read()`, `readline()`, `readlines()`, or loop over the file.",
             "`strip()` removes the trailing `\\n`.",
             "A missing file raises FileNotFoundError."],
      practice="Write 5 numbers to a file, then read them back and print their sum.", minutes=5),

    L("m09l3", "The with statement",
      "Context managers close files automatically — even when errors happen.", [
        T("Files must be closed after use. `with` does it for you when the block ends."),
        C('''
            f = open("demo.txt", "w")
            f.write("manual")
            f.close()
            print("closed:", f.closed)

            with open("demo.txt") as f:
                print(f.read())
            print("closed:", f.closed)
        '''),
        H("Encoding"),
        T("Always specify `encoding=\"utf-8\"` when working with text that may contain non-English characters or emoji."),
        C('''
            with open("hello.txt", "w", encoding="utf-8") as f:
                f.write("Hello 👋 আসসালামু আলাইকুম")
            with open("hello.txt", encoding="utf-8") as f:
                print(f.read())
        '''),
        TIP("Always prefer `with open(...)` — it's the professional standard."),
    ], keys=["`with` closes files automatically.",
             "Use `encoding=\"utf-8\"` for international text."],
      practice="Use `with` to copy the contents of one file into another file.", minutes=4),

    L("m09l4", "CSV & JSON files",
      "Work with the two most common data formats.", [
        H("CSV — spreadsheet data"),
        C('''
            import csv

            rows = [["name", "score"], ["Ali", 88], ["Sara", 95]]
            with open("scores.csv", "w", newline="") as f:
                csv.writer(f).writerows(rows)

            with open("scores.csv") as f:
                for row in csv.DictReader(f):
                    print(row["name"], "scored", row["score"])
        '''),
        H("JSON — structured data"),
        C('''
            import json

            profile = {"name": "Saif", "skills": ["Python", "Kotlin"], "level": 3}
            with open("profile.json", "w") as f:
                json.dump(profile, f, indent=2)

            with open("profile.json") as f:
                loaded = json.load(f)
            print(loaded["skills"])
            print(json.dumps(loaded))
        '''),
        TIP("JSON is how most web APIs send data — learning it is essential."),
    ], keys=["`csv.writer` / `csv.DictReader` handle CSV files.",
             "`json.dump` / `json.load` save and load Python data.",
             "`json.dumps` converts to a JSON string."],
      practice="Save a list of 3 books (title, author) as JSON, then load and print them."),

    L("m09l5", "os & pathlib",
      "Work with folders, paths and file information.", [
        C('''
            from pathlib import Path

            folder = Path("projects")
            folder.mkdir(exist_ok=True)
            (folder / "app.py").write_text("print('hi')")
            (folder / "readme.md").write_text("# Readme")

            for item in sorted(folder.iterdir()):
                print(item.name, item.suffix, item.stat().st_size, "bytes")
            print((folder / "app.py").exists())
        '''),
        C('''
            import os

            os.makedirs("data/logs", exist_ok=True)
            print(os.path.join("data", "logs", "today.txt"))
            print(os.path.exists("data/logs"))
            print(os.path.splitext("photo.jpeg"))
        '''),
        TIP("`pathlib` is the modern, object-oriented way to handle paths. `/` joins path parts."),
    ], keys=["`Path` objects represent files and folders.",
             "`mkdir`, `iterdir`, `exists`, `suffix` are very useful.",
             "`os.path.join` builds paths safely."],
      practice="Create a folder with 3 text files, then list only the `.txt` files with `glob(\"*.txt\")`."),
], [
    Q("Which mode adds text to the end of an existing file?", ["`\"w\"`", "`\"r\"`", "`\"a\"`", "`\"x\"`"], 2, "`a` = append."),
    Q("What is the main benefit of `with open(...)`?", ["Faster reading", "Automatically closes the file", "Encrypts the file", "Creates folders"], 1,
      "The context manager closes the file for you."),
    Q("Which error is raised when opening a missing file for reading?", ["ValueError", "KeyError", "FileNotFoundError", "IOError only on Windows"], 2,
      "Python raises FileNotFoundError."),
    Q("Which function converts a Python dict into a JSON string?", ["`json.load`", "`json.dumps`", "`json.parse`", "`str()`"], 1,
      "`dumps` = dump to string."),
    Q("What does `Path(\"a\") / \"b.txt\"` create?", ["A division error", "The path `a/b.txt`", "A string `ab.txt`", "A new file"], 1,
      "pathlib overloads `/` to join paths."),
    Q("What does `f.readlines()` return?", ["A string", "A list of lines", "The first line", "A tuple"], 1, "One list item per line."),
])

# ---------------------------------------------------------------------------
# Module 10 — Errors & exceptions
# ---------------------------------------------------------------------------
m10 = M("m10", 10, "Errors & Exceptions", "Handle problems gracefully with try, except, finally and raise", "module_10_errors", [
    L("m10l1", "Common error types",
      "Know the errors you'll meet most often and what causes them.", [
        LI("`SyntaxError` — invalid Python grammar.",
           "`NameError` — unknown variable name.",
           "`TypeError` — wrong type, e.g. `\"2\" + 2`.",
           "`ValueError` — right type, wrong value, e.g. `int(\"abc\")`.",
           "`IndexError` — list index out of range.",
           "`KeyError` — missing dictionary key.",
           "`ZeroDivisionError` — dividing by zero.",
           "`AttributeError` — the object has no such attribute/method."),
        C('''
            numbers = [1, 2, 3]
            print(numbers[5])
        '''),
        C('''
            total = "Score: " + 100
        '''),
        TIP("Read the error type and message first — they point straight at the problem."),
    ], keys=["Each error type tells you what went wrong.",
             "Tracebacks show the line number."],
      practice="Trigger a KeyError on purpose using a dictionary, and read the message.", minutes=5),

    L("m10l2", "try & except",
      "Catch errors so your program keeps running.", [
        C('''
            try:
                age = int(input("Your age: "))
                print("In 10 years you'll be", age + 10)
            except ValueError:
                print("Please type a whole number!")
        ''', stdin=["twenty"]),
        H("Several except blocks"),
        C('''
            def divide(a, b):
                try:
                    return a / b
                except ZeroDivisionError:
                    return "Cannot divide by zero"
                except TypeError:
                    return "Numbers only, please"

            print(divide(10, 2))
            print(divide(5, 0))
            print(divide("5", 2))
        '''),
        H("Getting the error message"),
        C('''
            try:
                {"a": 1}["b"]
            except KeyError as e:
                print("Missing key:", e)
        '''),
        TIP("Catch specific exceptions. A bare `except:` hides bugs."),
    ], keys=["`try:` code that might fail, `except Type:` handles it.",
             "Use several except blocks for different errors.",
             "`except Error as e` gives you the error object."],
      practice="Keep asking for a number until the user enters a valid integer (use while + try/except)."),

    L("m10l3", "else & finally",
      "Run code only on success, or always — no matter what.", [
        C('''
            def read_number(text):
                try:
                    value = float(text)
                except ValueError:
                    print(f"'{text}' is not a number")
                else:
                    print(f"Got {value}")
                finally:
                    print("-- check done --")

            read_number("3.5")
            read_number("abc")
        '''),
        LI("`else` runs only if **no** exception happened.",
           "`finally` runs **always** — perfect for cleanup like closing connections."),
    ], keys=["`else` = success path.",
             "`finally` = always runs, even after errors or return."],
      practice="Write a function that opens a file and uses `finally` to print \"closing\".", minutes=4),

    L("m10l4", "Raising exceptions",
      "Signal problems in your own code with raise.", [
        C('''
            def withdraw(balance, amount):
                if amount <= 0:
                    raise ValueError("Amount must be positive")
                if amount > balance:
                    raise ValueError("Insufficient funds")
                return balance - amount

            print(withdraw(100, 30))
            try:
                withdraw(100, 500)
            except ValueError as err:
                print("Error:", err)
        '''),
        H("assert for sanity checks"),
        C('''
            def average(nums):
                assert len(nums) > 0, "list must not be empty"
                return sum(nums) / len(nums)

            print(average([2, 4, 6]))
            average([])
        '''),
        TIP("Raise exceptions early when input is invalid — it makes bugs easy to find."),
    ], keys=["`raise ValueError(\"msg\")` signals an error.",
             "Callers handle it with try/except.",
             "`assert` checks assumptions during development."],
      practice="Write `set_age(age)` that raises ValueError if age is negative or above 150.", minutes=5),

    L("m10l5", "Custom exceptions",
      "Create your own exception classes for clearer error handling.", [
        C('''
            class InsufficientFundsError(Exception):
                def __init__(self, needed):
                    super().__init__(f"You need {needed} more")
                    self.needed = needed

            def pay(balance, price):
                if price > balance:
                    raise InsufficientFundsError(price - balance)
                return balance - price

            try:
                pay(50, 80)
            except InsufficientFundsError as e:
                print("Payment failed:", e)
                print("Missing amount:", e.needed)
        '''),
        T("Custom exceptions inherit from `Exception`. They make your code self-documenting and let callers catch exactly what they need."),
        TIP("Name custom exceptions with the `Error` suffix, like `ValidationError`."),
    ], keys=["Inherit from `Exception` to make your own error type.",
             "Add extra data as attributes."],
      practice="Create `InvalidEmailError` and raise it from a `register(email)` function.", minutes=5),
], [
    Q("Which error does `int(\"hello\")` raise?", ["TypeError", "ValueError", "NameError", "SyntaxError"], 1,
      "Right type (str), but an invalid value for int."),
    Q("When does the `finally` block run?", ["Only on errors", "Only on success", "Always", "Never"], 2, "finally always runs."),
    Q("What is printed?", ["A", "B", "A then C", "B then C"], 3, "The ZeroDivisionError is caught, then finally runs.",
      code="try:\n    1 / 0\n    print(\"A\")\nexcept ZeroDivisionError:\n    print(\"B\")\nfinally:\n    print(\"C\")"),
    Q("Which keyword triggers an exception yourself?", ["`throw`", "`raise`", "`error`", "`except`"], 1, "Python uses `raise`."),
    Q("Custom exceptions should inherit from…", ["`object`", "`Error`", "`Exception`", "`BaseError`"], 2, "Subclass `Exception`."),
    Q("When does `else` in try/except run?", ["When an exception occurs", "When no exception occurs", "Always", "Before try"], 1,
      "else is the success path."),
])

# ---------------------------------------------------------------------------
# Module 11 — OOP
# ---------------------------------------------------------------------------
m11 = M("m11", 11, "Object-Oriented Programming", "Classes, objects, inheritance and dunder methods", "module_11_oop", [
    L("m11l1", "Classes & objects",
      "A class is a blueprint; objects are things built from it.", [
        C('''
            class Car:
                pass

            my_car = Car()
            my_car.brand = "Toyota"
            print(type(my_car).__name__, my_car.brand)
        '''),
        T("A **class** defines a new type. An **object** (instance) is created by calling the class like a function. "
          "Objects bundle **data** (attributes) and **behaviour** (methods)."),
        LI("Class: `Dog` — the idea of a dog.",
           "Objects: `rex`, `bella` — specific dogs with their own names and ages."),
        TIP("Class names use `CapWords` (PascalCase): `BankAccount`, `HttpClient`."),
    ], keys=["`class Name:` defines a blueprint.",
             "`Name()` creates an object.",
             "Objects hold attributes and methods."],
      practice="Create an empty class `Book`, make two objects and give each a `title` attribute.", minutes=4),

    L("m11l2", "__init__ & attributes",
      "Initialize every new object with its own data.", [
        C('''
            class Student:
                school = "PyMaster Academy"   # class attribute (shared)

                def __init__(self, name, grade):
                    self.name = name          # instance attributes
                    self.grade = grade

            a = Student("Ali", 90)
            b = Student("Sara", 95)
            print(a.name, a.grade, a.school)
            print(b.name, b.grade, b.school)
        '''),
        T("`__init__` runs automatically when an object is created. `self` refers to the object being built."),
        TIP("Instance attributes belong to one object; class attributes are shared by all objects."),
    ], keys=["`__init__(self, ...)` sets up new objects.",
             "`self.x = ...` creates instance attributes.",
             "Class attributes are shared."],
      practice="Create a `Rectangle` class with `width` and `height` set in `__init__`.", minutes=5),

    L("m11l3", "Methods & self",
      "Give objects behaviour with methods.", [
        C('''
            class BankAccount:
                def __init__(self, owner, balance=0):
                    self.owner = owner
                    self.balance = balance

                def deposit(self, amount):
                    self.balance += amount
                    return self.balance

                def withdraw(self, amount):
                    if amount > self.balance:
                        print("Insufficient funds")
                        return self.balance
                    self.balance -= amount
                    return self.balance

            acc = BankAccount("Saif", 100)
            acc.deposit(50)
            acc.withdraw(500)
            acc.withdraw(30)
            print(acc.owner, acc.balance)
        '''),
        T("Methods are functions defined inside a class. Their first parameter is always `self`, which Python passes automatically."),
        H("Class & static methods"),
        C('''
            class Temperature:
                def __init__(self, celsius):
                    self.celsius = celsius

                @classmethod
                def from_fahrenheit(cls, f):
                    return cls((f - 32) * 5 / 9)

                @staticmethod
                def is_freezing(c):
                    return c <= 0

            t = Temperature.from_fahrenheit(212)
            print(t.celsius, Temperature.is_freezing(-5))
        '''),
    ], keys=["Methods take `self` first.",
             "`@classmethod` gets the class (`cls`) — great for alternative constructors.",
             "`@staticmethod` is a plain function inside a class."],
      practice="Add an `area()` and `perimeter()` method to your Rectangle class."),

    L("m11l4", "Inheritance",
      "Create specialized classes that reuse code from a parent class.", [
        C('''
            class Animal:
                def __init__(self, name):
                    self.name = name

                def speak(self):
                    return "..."

                def describe(self):
                    return f"{self.name} says {self.speak()}"

            class Dog(Animal):
                def speak(self):
                    return "Woof!"

            class Cat(Animal):
                def speak(self):
                    return "Meow!"

            for pet in [Dog("Rex"), Cat("Luna"), Animal("Generic")]:
                print(pet.describe())
        '''),
        H("super()"),
        C('''
            class Employee:
                def __init__(self, name, salary):
                    self.name = name
                    self.salary = salary

            class Manager(Employee):
                def __init__(self, name, salary, team):
                    super().__init__(name, salary)
                    self.team = team

            m = Manager("Sara", 9000, ["Ali", "John"])
            print(m.name, m.salary, len(m.team))
            print(isinstance(m, Employee))
        '''),
        TIP("This is **polymorphism**: different classes respond to the same method call in their own way."),
    ], keys=["`class Child(Parent):` inherits.",
             "Override methods to change behaviour.",
             "`super()` calls the parent's version."],
      practice="Create `Vehicle` with a `wheels()` method, then `Bike` and `Car` subclasses that override it."),

    L("m11l5", "Encapsulation & properties",
      "Protect an object's data and control how it's accessed.", [
        C('''
            class Account:
                def __init__(self, balance):
                    self._balance = balance      # "protected" by convention

                @property
                def balance(self):
                    return self._balance

                @balance.setter
                def balance(self, value):
                    if value < 0:
                        raise ValueError("Balance can't be negative")
                    self._balance = value

            acc = Account(100)
            print(acc.balance)
            acc.balance = 250
            print(acc.balance)
            try:
                acc.balance = -5
            except ValueError as e:
                print("Blocked:", e)
        '''),
        LI("`_name` — internal by convention (\"please don't touch\").",
           "`__name` — name-mangled to make accidental access harder.",
           "`@property` — use a method like an attribute, with validation."),
    ], keys=["Prefix with `_` for internal attributes.",
             "`@property` adds getters, `@x.setter` adds validation."],
      practice="Add a `@property` called `age` to a `Person` class that rejects negative values.", minutes=5),

    L("m11l6", "Dunder methods",
      "Special __methods__ let your objects work with Python's built-in features.", [
        C('''
            class Vector:
                def __init__(self, x, y):
                    self.x, self.y = x, y

                def __repr__(self):
                    return f"Vector({self.x}, {self.y})"

                def __add__(self, other):
                    return Vector(self.x + other.x, self.y + other.y)

                def __eq__(self, other):
                    return (self.x, self.y) == (other.x, other.y)

                def __len__(self):
                    return 2

            a = Vector(1, 2)
            b = Vector(3, 4)
            print(a + b)
            print(a == Vector(1, 2))
            print(len(a))
        '''),
        LI("`__str__` / `__repr__` — how the object prints.",
           "`__add__`, `__sub__`, `__mul__` — operators.",
           "`__eq__`, `__lt__` — comparisons.",
           "`__len__`, `__getitem__` — behave like a collection."),
        TIP("Dunder = **d**ouble **under**score. They're the secret behind Python's elegance."),
    ], keys=["Dunder methods hook into Python syntax.",
             "`__repr__` makes debugging easier.",
             "Operator overloading uses `__add__` etc."],
      practice="Add a `__str__` method to your Rectangle class that prints `Rectangle 4x5`."),
], [
    Q("What is `self` in a method?", ["The class", "The current object", "A keyword for globals", "The parent class"], 1,
      "self is the instance the method was called on."),
    Q("Which method runs automatically when an object is created?", ["`__new__` only", "`__init__`", "`__start__`", "`__create__`"], 1,
      "`__init__` initializes new objects."),
    Q("What is printed?", ["...", "Woof", "An error", "None"], 1, "Dog overrides speak().",
      code="class Animal:\n    def speak(self):\n        return \"...\"\nclass Dog(Animal):\n    def speak(self):\n        return \"Woof\"\nprint(Dog().speak())"),
    Q("What does `super().__init__(...)` do?", ["Creates a new class", "Calls the parent's __init__", "Deletes the object", "Makes a copy"], 1,
      "It runs the parent initializer."),
    Q("Which decorator turns a method into a read-only attribute?", ["`@staticmethod`", "`@classmethod`", "`@property`", "`@attribute`"], 2,
      "@property."),
    Q("Which dunder method defines the behaviour of `+`?", ["`__plus__`", "`__add__`", "`__sum__`", "`__concat__`"], 1, "`__add__`."),
])

# ---------------------------------------------------------------------------
# Module 12 — Advanced Python
# ---------------------------------------------------------------------------
m12 = M("m12", 12, "Advanced Python", "Modules, generators, decorators, dataclasses and more", "module_12_advanced", [
    L("m12l1", "Modules & the standard library",
      "Import ready-made tools — Python comes with 'batteries included'.", [
        C('''
            import math
            import random
            from datetime import date, timedelta

            print(math.sqrt(144), math.pi)
            print(math.ceil(4.1), math.floor(4.9))
            random.seed(7)
            print(random.randint(1, 6), random.choice(["red", "green", "blue"]))
            launch = date(2026, 1, 1)
            print(launch + timedelta(days=100))
        '''),
        LI("`import module` — use as `module.name`.",
           "`from module import name` — use `name` directly.",
           "`import module as alias` — e.g. `import datetime as dt`."),
        H("A few gems"),
        C('''
            from collections import Counter
            import statistics
            import string

            print(Counter("mississippi").most_common(2))
            print(statistics.mean([4, 8, 6]), statistics.median([3, 1, 9]))
            print(string.ascii_uppercase[:5])
        '''),
        TIP("Outside this app, `pip install package` adds third-party libraries like requests or pandas."),
    ], keys=["`import` loads modules.",
             "The standard library covers maths, dates, random, files, JSON and more.",
             "pip installs external packages."],
      practice="Use `random.sample(range(1, 50), 6)` to generate lottery numbers and sort them."),

    L("m12l2", "Iterators & generators",
      "Produce values one at a time to save memory.", [
        C('''
            numbers = iter([10, 20, 30])
            print(next(numbers))
            print(next(numbers))
            print(next(numbers))
        '''),
        T("A **generator** function uses `yield` instead of `return`. It pauses after each value and resumes when the next value is requested."),
        C('''
            def countdown(n):
                while n > 0:
                    yield n
                    n -= 1

            for value in countdown(5):
                print(value, end=" ")
            print()

            gen = (x * x for x in range(1_000_000))
            print(next(gen), next(gen), next(gen))
        '''),
        TIP("Generator expressions `( ... )` handle millions of items using almost no memory."),
    ], keys=["`iter()` and `next()` step through iterables.",
             "`yield` creates a generator.",
             "Generators are lazy and memory-efficient."],
      practice="Write a generator `evens(limit)` that yields even numbers up to `limit`."),

    L("m12l3", "Decorators",
      "Wrap functions to add behaviour without changing their code.", [
        C('''
            def shout(func):
                def wrapper(*args, **kwargs):
                    result = func(*args, **kwargs)
                    return result.upper() + "!"
                return wrapper

            @shout
            def greet(name):
                return f"hello {name}"

            print(greet("saif"))
        '''),
        T("`@shout` is the same as writing `greet = shout(greet)`. Decorators are used everywhere — Flask routes, logging, timing, caching."),
        C('''
            import time
            from functools import wraps

            def timer(func):
                @wraps(func)
                def wrapper(*args, **kwargs):
                    start = time.perf_counter()
                    result = func(*args, **kwargs)
                    took = time.perf_counter() - start
                    print(f"{func.__name__} finished in {took:.3f}s")
                    return result
                return wrapper

            @timer
            def slow_sum(n):
                return sum(range(n))

            total = slow_sum(10)
            print(total)
        ''', output=None),
        TIP("`functools.lru_cache` is a built-in decorator that can make recursive functions dramatically faster."),
    ], keys=["A decorator takes a function and returns a new one.",
             "`@decorator` syntax applies it.",
             "Use `functools.wraps` to keep the original name."],
      practice="Write a decorator `log_call` that prints the function name before calling it."),

    L("m12l4", "Context managers",
      "Set up and clean up resources reliably with with.", [
        C('''
            from contextlib import contextmanager

            @contextmanager
            def section(title):
                print(f"=== {title} ===")
                yield
                print("=" * (len(title) + 8))

            with section("Report"):
                print("Sales: 120")
                print("Users: 45")
        '''),
        C('''
            class Timer:
                def __enter__(self):
                    print("Timer started")
                    return self

                def __exit__(self, exc_type, exc, tb):
                    print("Timer stopped")
                    return False

            with Timer():
                print("Doing work...")
        '''),
        TIP("Files, locks, database connections and network sockets all use context managers."),
    ], keys=["`with` calls `__enter__` and `__exit__`.",
             "`@contextmanager` builds one from a generator."],
      practice="Write a context manager that prints `start` and `end` around a block.", minutes=5),

    L("m12l5", "Type hints & dataclasses",
      "Write modern, self-documenting Python.", [
        C('''
            def total_price(price: float, qty: int = 1) -> float:
                return price * qty

            print(total_price(9.99, 3))
            print(total_price.__annotations__)
        '''),
        T("Type hints don't change how code runs, but editors and tools use them to catch bugs early."),
        C('''
            from dataclasses import dataclass, field

            @dataclass
            class Product:
                name: str
                price: float
                tags: list[str] = field(default_factory=list)

                def with_tax(self, rate: float = 0.15) -> float:
                    return round(self.price * (1 + rate), 2)

            p = Product("Keyboard", 49.0, ["tech"])
            print(p)
            print(p.with_tax())
            print(p == Product("Keyboard", 49.0, ["tech"]))
        '''),
        TIP("`@dataclass` writes `__init__`, `__repr__` and `__eq__` for you."),
    ], keys=["Type hints: `def f(x: int) -> str:`.",
             "`@dataclass` removes boilerplate.",
             "Use `field(default_factory=list)` for mutable defaults."],
      practice="Create a `@dataclass` for a `Movie` with title, year and rating."),

    L("m12l6", "Writing professional Python",
      "Habits that make you job-ready.", [
        LI("**`if __name__ == \"__main__\":`** — run code only when the file is executed directly.",
           "**Virtual environments** — `python -m venv .venv` keeps project dependencies separate.",
           "**pip & requirements.txt** — `pip install -r requirements.txt` sets up a project.",
           "**Testing** — `unittest` or `pytest` verify your code automatically.",
           "**Version control** — use Git and GitHub for every project.",
           "**Read the docs** — docs.python.org is your best friend."),
        C('''
            def add(a, b):
                return a + b

            def test_add():
                assert add(2, 3) == 5
                assert add(-1, 1) == 0
                print("All tests passed ✅")

            if __name__ == "__main__":
                test_add()
        '''),
        TIP("You now know the core of Python! Next stop: 15 real-world projects to put it all together."),
    ], keys=["Guard scripts with `if __name__ == \"__main__\":`.",
             "Use venv, pip, tests and Git in real projects."],
      practice="Write two functions and a test function that checks them with `assert`.", minutes=5),
], [
    Q("What does `from math import sqrt` allow?", ["Calling `math.sqrt` only", "Calling `sqrt` directly", "Installing math", "Nothing"], 1,
      "The name is imported into your file."),
    Q("Which keyword makes a function a generator?", ["`return`", "`yield`", "`async`", "`gen`"], 1, "`yield` produces values lazily."),
    Q("`@timer` above `def f():` is equivalent to…", ["`f = timer`", "`timer = f(timer)`", "`f = timer(f)`", "`f()`"], 2,
      "The decorator receives the function and returns a replacement."),
    Q("What does `@dataclass` generate automatically?", ["A database table", "`__init__`, `__repr__`, `__eq__`", "Unit tests", "Type checking at runtime"], 1,
      "It writes the boilerplate methods."),
    Q("What is printed?", ["1 2", "[1, 2]", "1", "An error"], 0, "next() pulls one value at a time.",
      code="g = (n for n in [1, 2, 3])\nprint(next(g), next(g))"),
    Q("Why use `if __name__ == \"__main__\":`?", ["To make code faster", "To run code only when the file is executed directly", "To declare the main class", "It's required in every file"], 1,
      "Importing the file won't run that block."),
])

MODULES_C = [m9, m10, m11, m12]

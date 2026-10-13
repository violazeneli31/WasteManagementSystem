# Waste Management System – GreenBin Albania

Software Modeling & Design project, Group 10, Epoka University (Spring 2025).

GreenBin Albania is a waste management company. Our system covers its whole workflow: smart bins with sensors and barcodes, collection schedules built from waste statistics, route optimization with live truck tracking, rewards and vouchers for citizens, community events, a recycling plant, and billing, fraud checks and budget forecasts.

There are five kinds of users: **company managers**, **garbage collection employees**, **citizens and institutions**, **recycling facility operators** and **finance officers**. The design has **35 functional requirements** (FR_01–FR_35) and four design patterns: **Singleton, Observer, Factory and Decorator**.

The project has three parts: the **design** (document and UML diagrams), a **MySQL database** built from the ERD, and a **Java desktop prototype** that implements every requirement. The database and the Java prototype were added after the course, to show the design working as real code.

---

## What's in each folder

| Folder / file | Contents |
|---|---|
| `docs/` | The full project document (PDF + Word): requirements, use cases, all diagrams, design patterns |
| `diagrams/` | Editable draw.io diagrams |
| `database/` | `schema.sql`, `sample_data.sql`, `queries.sql` |
| `src/main/java/` | The Java application (domain classes + visual interface) |
| `src/test/java/` | 59 JUnit 5 tests |
| `gui-screenshots/` | Screenshots of the Java app running |
| `meeting-reports/` | The 9 weekly meeting reports (March–May 2025) |
| `pom.xml` | Maven build file (Java 17, JUnit 5) |

---

## `diagrams/`

Open the `.drawio` files at [app.diagrams.net](https://app.diagrams.net) (*File → Open from → Device*), or in the draw.io desktop app or the VS Code *Draw.io Integration* extension.

- **`ClassDiagram.drawio`** and **`ERD.drawio`** cover the **whole project**: every class of the system and every database entity.
- **`viola-zeneli-part/`** contains **only Viola's part** of each diagram type, made for her five requirements (FR_01–FR_05):
  - `ActivityDiagrams.drawio`: AC_01–AC_05
  - `SequenceDiagrams.drawio`: SC_01–SC_05
  - `CollaborationDiagrams.drawio`: CC_01–CC_05
  - `DecoratorPatternDiagram.drawio`: the class diagram of the Decorator pattern (Viola's pattern)

The other members' diagrams (use case, state, activity, sequence and collaboration) are in the project document in `docs/`.

---

## `database/`

**How it was made:** each entity in the ERD became a table, and each relationship became a foreign key. The "is-a" entities became their own tables (`users` → `citizens` / `institutions`, `employees` → collectors / operators, `smart_containers` → `smart_bins`). Many-to-many relationships became join tables (`program_participants`, `vehicle_assignments`, `route_stops`). Rules from the requirements are enforced in the database itself:
- **checks**, for example "a shift starts before it ends" and "a bin is never filled above capacity";
- a **computed column** for the 20% tax on bills;
- **views** for the leaderboard, optimal pickups per area, employee schedules and bins that need emptying;
- **triggers** that reject overlapping shifts and shifts on an employee's day off, and that add reward points automatically.

| File | What it does | Run it |
|---|---|---|
| `schema.sql` | Creates the `waste_management` database with its 39 tables, views and triggers. It drops the old database first. | first |
| `sample_data.sql` | Fills the database with the same people and places as the Java demo. Dates are relative to today, and every password is `password` (stored as SHA-256). | second |
| `queries.sql` | Example queries, grouped by use case (UC_01–UC_05, leaderboard, bills, recycling...) | any time after |

**With XAMPP / phpMyAdmin:**
1. Start **Apache** and **MySQL** in XAMPP, then open `http://localhost/phpmyadmin`.
2. Go to **Import**, choose `schema.sql` and click **Go**.
3. Select the new `waste_management` database, then import `sample_data.sql` the same way.
4. Open the **SQL** tab and paste any query from `queries.sql` to run it.

**From the terminal (MySQL 8 or MariaDB):**
```
mysql -u root -p < database/schema.sql
mysql -u root -p waste_management < database/sample_data.sql
mysql -u root -p waste_management < database/queries.sql
```
Tested on MariaDB 10.11 (the XAMPP database); also works on MySQL 8.0.16+.

---

## `src/`: the Java application

### How we built it

- **The class diagram is the starting point.** Every class in it has a Java class with the same name, written in Java style (`Smart_Bin` → `SmartBin`). The classes are grouped into packages by area: `people`, `collection`, `planning`, `routing`, `notification`, `programs`, `bins`, `community`, `finance`, `recycling`, `weather`, `recognition` and `model` (the enums and `Coordinate`).
- **Every requirement has a method and a screen.** `system/GreenBinSystem` holds all the data in memory (like a small database) and has one method for each action in the requirements, grouped by FR number. The screens only call these methods.
- **These classes implement all 35 requirements, and each one can be seen working on screen.** The app is a **desktop GUI prototype (Java Swing), not a real website**. You log in with a role and click through its screens. You can see a city map with trucks moving live, bins filling up, charts, a week calendar, vouchers with QR codes and a notification inbox. Every screen shows the FR numbers it covers.
- **Tests:** 59 JUnit tests in `src/test/java` check every area of the system.

### Screenshots

Some screenshots of the app running are in [`gui-screenshots/`](gui-screenshots/).

### Design patterns in the code

| Pattern | Where | Why |
|---|---|---|
| **Singleton** | `routing/RouteOptimizationCenter.getInstance()` (also `GreenBinSystem`) | Only one route optimizer exists, so routes never conflict |
| **Observer** | `notification/NotificationCenter` (subject), `NotificationObserver` implemented by `User` and `Employee` | Everyone affected is notified when something changes; turned-off categories are skipped (FR_21) |
| **Factory Method** | `programs/ProgramFactory` → `EventFactory`, `EducationalProgramFactory` | Events and educational programs are created and checked in one place |
| **Decorator** | `notification/NotificationDecorator` → `RewardNotification`, `EventReminderNotification`, `UrgentNotification` | Adds rewards, reminders or an urgent mark to any notification without changing it |

The app has a **Design Patterns** screen with a button that runs each pattern live.

### Run it

You need **Java 17 or newer**. Log in with one of the demo accounts on the login screen (password `password`): `viola` (manager), `ardi` (collector), `ana` (citizen), `school21` (institution), `mira` (finance), `toni` (recycling operator). The manager's and finance officer's 2FA code is shown in the login window.

- **IntelliJ IDEA:** *File → Open* → choose this folder (it opens as a Maven project) → run `com.greenbin.wms.Main`. To run the tests, right-click `src/test/java` → *Run All Tests*.
- **Maven:** `mvn compile exec:java` starts the app, `mvn test` runs the tests, and `mvn package` builds a jar that opens with a double click.
- **Java only:**
  ```
  javac -d out $(find src/main/java -name "*.java")
  java -cp out com.greenbin.wms.Main
  ```

### Where each requirement is in the app

| Role | Screens (requirements) |
|---|---|
| Manager | Overview · Zone Map & Trucks (FR_01, 06, 11) · Statistics & Forecast (FR_02, 30) · Schedules & Weather (FR_02, 03, 05, 07) · Route Optimization (FR_04) · Smart Bins (FR_08, 09, 12, 14) · Programs & Events (FR_15, 16) · Reports (FR_20) · Community & Partners (FR_24–27) · Recycling Plant (FR_19) · Expenses, Budget, Disputes (FR_32–35) · Activity Log |
| Collector | My Tasks (FR_05, 06) · Pickup Requests (FR_23) · Bins: Place & Empty (FR_09, 11, 17) · Send a Report (FR_20) |
| Citizen / Institution | My Profile (FR_10, 21) · Throw Waste (FR_08, 12, 14) · Which Bin? photo AI (FR_22) · Request a Pickup (FR_13, 23) · Rewards & Leaderboard (FR_24–26) · Donate (FR_27) · My Bills (FR_29, 34) · Events (FR_15, 16) · Send a Report (FR_20) |
| Finance officer | Billing & Pricing (FR_13, 28, 29) · Fraud Detection (FR_31) · Disputes & Refunds (FR_34) · Expenses & Tax (FR_32, 33) · Budget Forecast (FR_35) |
| Recycling operator | Recycling Plant (FR_17, 18, 19) · Send a Report (FR_20) |

Every role also has **Notifications** and **Design Patterns**. **Reset demo** in the top bar restores the starting data.

---

## Viola's part

Viola Zeneli moderated all nine meetings, approved the requirements together with Sidrit, and made the class diagram of the whole system with him. Her five requirements are on waste collection and transportation: **FR_01** Working Zone Map, **FR_02** Statistics-Based Schedule Optimization, **FR_03** Schedule and Plan, **FR_04** Route Optimization and **FR_05** Notification for Collection. Her design pattern was the **Decorator**.

## Team

Viola Zeneli, Sidrit Rrustemi, Noel Zani, Nikol Dalipi, Suada Terolli, Alesia Dardha, Mirsad Amuli

## Lecturers

M.Sc. Ari Gjerazi, M.Sc. Edlira Cani

# 🏠 GORIB: Smart Budget & Expense Tracker

> **GORIB** (pronounced *go-reeb*, meaning **"Poor"** in Bengali) is a premium, AI-powered financial companion. It is built with a singular mission: helping you stay budget-conscious, save smartly, and build healthy financial habits so you are never left feeling *gorib*!

GORIB is a modern, high-precision Android application designed to streamline daily expense tracking, automate recurring bills, analyze spending trends, and leverage state-of-the-art AI to scan and translate paper grocery receipts instantly.

---

## 🚀 Core Features

GORIB comes packed with premium features to put you in complete control of your financial destiny:

### 1. 🏠 Home Overview (Central Command Center)
Your dashboard is a clean, modern gateway into your monthly finances.
* **Instant Financial Pulse:** View your total monthly budget, current spending, and exact remaining funds at a single glance.
* **Category Progress Visuals:** High-contrast, color-coded progress bars show you exactly which categories are healthy and which are near their limits.
* **Smart Actions:** Swift shortcuts to log expenses, check alerts, or perform quick updates.
* **Recent Activity Feed:** Scrollable history log of your latest transactions.

### 2. 📊 Smart Analytics & Interactive Charts
Gain deep, actionable insights into your spending habits without wading through spreadsheets.
* **Period-over-Period Compare:** Automatically computes spending trends (current month vs. last month) with percentage change indicators.
* **Category Breakdown:** Interactive charts categorizing your expenses (Food, Bills, Transport, etc.).
* **Daily Spending Bar Chart:** A responsive chart allowing you to tap any individual day to see a detailed transaction breakdown.
* **Top Spending Merchants:** Ranks your biggest spending destinations so you know exactly where the leaks are.

### 3. 🛒 Smart Groceries & Gemini AI OCR Scanner
Say goodbye to manual item entry! GORIB brings cutting-edge Artificial Intelligence to your grocery shopping.
* **Grocery Sessions:** Create and bundle related shopping lists and target limits for specific trips.
* **Advanced Gemini AI OCR:** Take a photo or upload an image of a receipt. The app employs `gemini-2.5-flash` vision models to process Malaysia's unique two-line receipt layouts (Econsave, Maslee, Lotus's, Jaya Grocer, TF Value Mart) in seconds.
* **Intelligent Translation:** If receipt items are printed in Malay (e.g., *Susu Segar*, *Ayam Potong*, *Bawang Merah*), Gemini translates them on the fly and appends the English translation in parentheses.
* **Precision Extraction:** Seamlessly extracts item names, weights/volumes (e.g., `550G`, `1KG`, `1.5L`), quantities, and final net prices.
* **Draft Editor:** Review and adjust the AI-extracted items in an interactive review sheet before committing them as categorized transactions.

### 4. 🏷️ Custom Budget Categories & Rules
Fine-tune GORIB to match your unique lifestyle.
* **Granular Limits:** Set specific monthly caps on individual categories.
* **Automated Keyword Routing:** Define custom keywords (e.g., "Grab" to "Transport", "Starbucks" to "Cafe") to automatically sort recognized receipt items into their designated folders.
* **Audit Trail:** Complete historical log of every item purchased within a specific category.

### 5. 📅 Rent & Bills Automation
A dedicated system to ensure you never incur a late payment fee.
* **Landlord Ledger:** Save your monthly rent amount, due date, and landlord bank details (account number, bank name) for quick copy-pasting.
* **Dynamic Payment Tracking:** Toggle your monthly rent payment status visually.
* **Utility Grouping:** Organize utilities into dedicated sub-groups (Electricity, Water, Internet, Sewerage).
* **Cost Visualizers:** Track utility costs over time and log individual billing line items.

### 6. 🔁 Recurring Expenses
Keep constant tabs on subscriptions and automated payments.
* **Subscription Management:** Log commitments like Netflix, Spotify, gym memberships, or insurance.
* **Custom Frequencies:** Define flexible recurring intervals (weekly, monthly, annually).
* **Due Alerts & Auto-Log:** View active countdowns to due dates with automatic database logging to maintain accurate account histories.

### 7. 🧙 Step-by-Step Onboarding Wizard
Getting started is quick, personal, and smooth.
* **Custom Profile:** Set up your nickname and preferred local currency symbol (e.g., `RM`, `$`, `€`).
* **Goal Setting:** Enter your estimated monthly income and designated savings goals.
* **Initial Commitments:** Configure your Rent and active Utilities during onboarding.
* **Safe-Budget calculation:** The wizard automatically computes a safe monthly spending budget based on your income, savings targets, and fixed commitments.

### 8. 🌙 Gorgeous Dark Theme Support
Beautiful design that is gentle on your eyes and your battery.
* **System-Wide Dark Mode:** Full, gorgeous system-wide dark mode interface.
* **Dynamic Adaptation:** Seamless transitions between light and dark modes based on your preferences.
* **Enhanced Contrast:** High-accessibility ratios to make sure all text and charts remain highly readable in any lighting conditions.

---

## 🛠️ Technical Stack & Architecture

GORIB is built using modern Android development practices, ensuring high performance, clean code, and solid offline capabilities:

* **Language:** Kotlin (JVM 17)
* **UI Framework:** Jetpack Compose (using Material Design 3)
* **Architecture:** Clean Architecture & MVVM (Model-View-ViewModel) pattern:
  * **UI Layer:** Compose Screen components, State flows, and Navigation controllers.
  * **Domain Layer:** Business models, Use Cases (e.g., `SaveRentSetupUseCase`), and Repository Interfaces.
  * **Data Layer:** Room database entities/DAOs, DataStore Preferences, and concrete Repository implementations.
* **Database & Persistence:**
  * **Room DB:** High-performance local SQL database with relational entities for sessions, receipts, and recurring bills.
  * **DataStore (Preferences):** Key-value storage for user settings, profile details, and system flags.
* **Dependency Injection:** Dagger Hilt (`hilt-android` + Hilt Navigation Compose) for clean modular component injection.
* **AI & Machine Learning:**
  * **Google Gemini SDK (`generativeai:0.9.0`):** Integrates the powerful `gemini-2.5-flash` model for high-precision receipt visual analysis.
  * **ML Kit Text Recognition:** Provides optional, low-latency client-side local OCR preprocessing.
* **Image Loading:** Coil (`coil-compose`) for asynchronous image decoding and caching.

---

## 📂 Project Structure

```
budgetapp/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/gorib/app/
│   │   │   │   ├── data/             # Database (Room), Preferences (DataStore), Repository Impls
│   │   │   │   ├── di/               # Dagger Hilt dependency modules
│   │   │   │   ├── domain/           # Business Models, GeminiReceiptParser, Use Cases
│   │   │   │   └── ui/               # Jetpack Compose Screens, Theme, Navigation, & Components
│   │   │   └── res/                  # Vector drawables, values, XMLs
│   │   └── build.gradle.kts          # Module-level Gradle config
│   └── build.gradle.kts              # Top-level Gradle config
└── settings.gradle.kts               # Project & dependency repositories
```

---

## ⚙️ Setup & Configuration

### Prerequisites
* **Android Studio:** Ladybug or newer is recommended.
* **JDK:** Version 17 or higher.
* **Android SDK:** Compile SDK 36, Min SDK 26.

### Gemini API Key Configuration
The AI Receipt Scanner requires a Google Gemini API Key. 

1. Obtain an API Key from the [Google AI Studio](https://aistudio.google.com/).
2. Add your API key to your local properties or key management store so it can be securely passed to `GeminiReceiptParser.parseReceipt(bitmap, apiKey)` during runtime.

### Building & Running
You can compile the project using standard Gradle wrapper commands or run it directly from Android Studio:

```powershell
# Build debug apk
./gradlew assembleDebug

# Run unit and instrumented tests
./gradlew test
./gradlew connectedAndroidTest
```

---

## 💡 Mission Statement
> *"Do not save what is left after spending; instead spend what is left after saving."*
> GORIB helps you respect every coin, recognize spending patterns, and make intentional financial decisions. Let's make smart savings second nature!

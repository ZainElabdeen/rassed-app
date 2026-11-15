**🌐 Language:** **English** · [العربية](README.ar.md)

# Raseed (رصيد)

**Raseed** is an Arabic-first Android app for mobile airtime (top-up) sellers in Sudan. It runs on a single phone with no account, no server and no internet connection. It records each credit sale in a few taps and keeps stock, customer debts and monthly totals accurate.

> The app is native Android (Kotlin, Jetpack Compose, Material 3, Room). It is not React Native or Expo.

**Just want the app?** [Download the APK](#download) and install it on any phone running Android 7.0 or newer.

## Download

[![Download APK](https://img.shields.io/badge/Download-APK%20v1.0-2ea44f?style=for-the-badge&logo=android&logoColor=white)](https://github.com/ZainElabdeen/rassed-app/raw/main/releases/raseed.apk)

**[⬇️ Download Raseed v1.0 (APK, 9.6 MB)](https://github.com/ZainElabdeen/rassed-app/raw/main/releases/raseed.apk)**

| | |
|---|---|
| Version | 1.0 (version code 1) |
| Requires | Android 7.0 (API 24) or newer |
| Size | 9.6 MB |
| SHA-256 | `7d6c5d35841ace74daf6d886100d9dd98296a87edbdd2d8c4cd543b385d48eb3` |

### Install on your phone

1. Open the download link above on the Android phone, or copy the APK to the phone.
2. Tap the downloaded `raseed.apk`.
3. If Android blocks it, allow **Install unknown apps** for your browser or file manager when prompted (Settings → Apps → *your browser* → Install unknown apps).
4. Tap **Install**, then open **رصيد** from the app drawer.

The app needs no account and no internet connection. To update later, install the newer APK over the old one; your data stays on the phone. Export a backup from **Settings** first, just in case.

## Features

| Area | What it does |
|---|---|
| **Home** | This month's sales, the total owed by customers, and a card per product showing remaining stock against what was available this month. A notice appears if no stock has been set for the current month. A large **New sale** button sits at the bottom. |
| **New sale** | Pick the product, enter the amount, and choose **Cash**, **Bankak** or **Pay later**. A customer is optional for cash and Bankak, required for pay later, and a new customer can be added from the same sheet. If the amount exceeds available stock you get a warning and can still continue. |
| **Sales** | History from newest to oldest, filtered by product and payment method. Pay-later sales are labelled Paid, Partly paid or Unpaid. Deleting a sale (after confirmation) restores the stock and the debt. |
| **Customers** | Add, edit, delete and view customers. A customer's screen shows the balance owed, the credit types they bought, and a dated history of sales and payments. A customer who still owes money cannot be deleted. |
| **Payments** | Record a later payment against a customer's account, with its own date and method (cash or Bankak). |
| **Products and stock** | Three default products: Zain, Sudani and MTN. Add a product with any name, and remove any product, defaults included. Set the month's opening stock, add or deduct stock with a reason, and view the dated stock log. |
| **Search** | One search across customers, products, sales, payments and the stock log, by name, phone, amount, payment method, note, reason or date. It ignores diacritics and Arabic alef variants, and accepts Arabic-Indic digits. |
| **Reports** | A monthly report and a custom date-range report on screen: total sales, sales by product and by payment method, credit given, payments received, total owed, and a sales table. Each report exports as a right-to-left Arabic PDF to share or print. |
| **Settings** | Export all data to one file, import it with validation, and switch between Arabic (default) and English. |

## How the numbers work

- Amounts are whole Sudanese pounds, stored as `Long` and shown with Latin digits (`1,500`). Dates are stored in milliseconds.
- A sale's amount is the airtime value sold. It is deducted from stock at the same value (1:1), so there is no purchase price and no profit figure.
- **Customer balance** = total pay-later sales − total payments.
- **Settlement:** a payment is applied to the oldest pay-later sales first (FIFO). That decides each sale's status: paid, partly paid or unpaid.
- **Stock** = this month's opening stock + additions − deductions − the product's sales since then.
- **Removing a product** only archives it. Old sales keep the product name from the time of sale, so records and reports stay intact. The default products are created once, when the database is created, so a removed default does not come back.
- **Deleting a customer** deletes only the customer. Their sales and payments stay in the history and reports, labelled "Deleted customer".

## Data storage

Everything is stored on the phone in a Room (SQLite) database. DataStore holds only the language choice. Nothing leaves the device unless you export a backup or share a PDF.

| Table | Fields |
|---|---|
| `Product` | id (stable: `zain`, `sudani`, `mtn`, or a UUID), name, sort order, archived, created at |
| `Customer` | id, name, phone (optional), created at |
| `Sale` | id, product id, product name (snapshot at sale time), amount, customer id (optional), created at, method (`CASH` / `BANKAK` / `CREDIT`) |
| `Payment` | id, customer id, amount, paid at, method (`CASH` / `BANKAK`), note (optional) |
| `StockEntry` | id, product id, kind (`OPENING` / `ADD` / `DEDUCT`), amount, reason, created at |

There are no cascading foreign keys, which is why removing a product or customer never corrupts old records.

## Backup and restore

- **Export:** writes one JSON file containing all five tables (archived products included). Its header holds `format` (value `raseed-backup`), `schemaVersion`, `exportedAt`, a row count per table and a SHA-256 checksum of the data. You save it through the system file picker or share it.
- **Import:** reads and validates the file **before touching any data**, and rejects it if:
  - it is not valid JSON or has the wrong format name;
  - it was written by a newer version of the app;
  - the checksum or row counts do not match;
  - an id is duplicated;
  - a sale or stock entry points to a product that does not exist;
  - an amount is out of range, a method or kind is unknown, or a pay-later sale has no customer.
- A valid file is shown for confirmation, because import **replaces all current data**. The replacement runs in a single database transaction, so if anything fails it is rolled back and the phone's data stays as it was.
- To move to a new phone: export on the old phone, copy the file across, and import it on the new one.

## PDF reports

Reports are drawn with Android's built-in `PdfDocument` and `StaticLayout`, so Arabic letters join correctly and text runs right to left, with no external PDF library. Pages are A4 and can run to several pages. Sharing goes through `FileProvider` and printing through `PrintManager`.

The PDF uses the Arabic font installed on the phone; the app ships no font file. To use a specific font, add a `.ttf` to `res/font` and use it in `pdf/ReportPdf.kt`.

## Language and direction

Arabic is the default and the interface is right to left. English is available in Settings, and the layout direction follows the chosen language. Strings live in `res/values/` (Arabic) and `res/values-en/` (English). The appearance is light in every system setting.

## Tech stack

- Kotlin 2.4 and Jetpack Compose with Material 3 (Compose BOM 2026.09.00)
- Room 2.8 with KSP for the SQLite database
- DataStore Preferences for the language
- Navigation Compose and Lifecycle ViewModel, with a bottom navigation bar
- kotlinx.serialization for the backup file
- Android Gradle Plugin 9.4, `minSdk` 24 (Android 7), `targetSdk` 36, Java 17

There is no image, network or PDF library. Versions are pinned in `android/gradle/libs.versions.toml`.

## Project structure

```
android/app/src/main/java/app/injaz/starter/
├── MainActivity.kt, RaseedApp.kt     entry point and application class
├── data/                             Room entities, DAOs, database, repository, settings store
├── domain/                           pure logic, no Android or Room types
│   ├── Balances.kt                   customer balance and FIFO settlement
│   ├── Inventory.kt                  stock calculation
│   ├── DateRanges.kt                 month and range boundaries
│   ├── ReportBuilder.kt              report totals and breakdowns
│   ├── Backup.kt                     backup format, checksum, validation
│   ├── Search.kt                     text normalisation and matching
│   └── Money.kt, Validation.kt       formatting and input checks
├── pdf/                              ReportPdf.kt (PDF drawing), PrintAdapter.kt
└── ui/
    ├── AppNav.kt                     tabs and routes
    ├── theme/, kit/, common/         theme, shared components, helpers
    └── home/ sales/ customers/ products/ reports/ settings/ more/ search/
                                      one folder per screen, with its ViewModel
android/app/src/test/java/.../domain/ unit tests for the logic
```

## Build and run

Requirements: Android Studio with JDK 17 and the Android SDK (compile SDK 37).

1. Open the **`android/`** folder in Android Studio and let Gradle finish syncing.
2. Choose a device or emulator running Android 7.0 (API 24) or newer, then press **Run**.
3. Run the unit tests with `./gradlew testDebugUnitTest` from the `android/` folder, or from Android Studio's test runner.
4. To build a file for installing or publishing, use **Build → Generate Signed App Bundle / APK** in Android Studio and sign it with your own key. The project has no signing configuration and contains no keys. Keep the keystore and its passwords out of the repository.
5. To publish a new version for download, raise `versionCode` and `versionName` in `android/app/build.gradle.kts`, build a signed release APK with the same key, replace `releases/raseed.apk`, and update the version, size and SHA-256 in the Download section of both READMEs (`sha256sum releases/raseed.apk`).

The unit tests cover balances, settlement, stock, date boundaries, money formatting, input validation, report totals, backup validation and search.

## Scope and limits

- One device, one user. No accounts, no sync between devices, no sharing data between people. The backup file is how data moves.
- **Bankak** is only a label the seller records by hand; there is no payment integration.
- Not included: multiple currencies, purchase prices and profit, notifications, multiple users.
- How the PDF looks depends on the Arabic font installed on the phone.

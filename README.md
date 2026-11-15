# رصيد (Raseed)

**رصيد** is an offline, Arabic-first Android app for people who resell mobile network credit (airtime / top-up) in Sudan. It runs on a single phone, with no account, no backend and no network access. It records every credit sale in a few taps and keeps stock, customer debts and monthly totals correct.

> **تطبيق لبائعي رصيد الاتصالات في السودان.** يعمل على هاتف واحد بلا إنترنت وبلا حسابات. يسجّل المبيعات والعملاء والديون ومخزون الرصيد لكل شركة، ويُصدر تقارير PDF عربية ونسخة احتياطية كاملة.

The app is native Android (Kotlin, Jetpack Compose, Material 3, Room). It is not React Native or Expo.

## Features

| Area | What it does |
|---|---|
| **Home** | This month's sales, the total owed by customers, and a card per product showing remaining stock against the stock available this month. A notice appears when no stock has been set for the current month. A large **New sale** button sits at the bottom. |
| **New sale** | Pick a product, enter an amount, choose **Cash**, **Bankak** or **Pay later**. A customer is optional for cash and Bankak, and required for pay later (a new customer can be added from the same sheet). A warning is shown if the amount exceeds available stock, and you can still continue. |
| **Sales** | History, newest first, filtered by product and payment method. Pay-later sales show a paid, partly paid or unpaid label. Deleting a sale (after confirmation) restores stock and debt. |
| **Customers** | Add, edit, delete and view customers. The detail screen shows the outstanding balance, the credit types bought, and a dated history of sales and payments. A customer who still owes money cannot be deleted. |
| **Payments** | Record a later payment against a customer, with its own date and method (cash or Bankak). |
| **Products and stock** | Three default products: Zain, Sudani and MTN. Add products with any name. Remove any product, including the defaults. Set the month's opening stock, add or deduct stock with a reason, and view the dated stock log. |
| **Search** | One search across customers, products, sales, payments and the stock log, by name, phone, amount, payment method, note, reason or date. It ignores Arabic diacritics and alef variants, and accepts Arabic-Indic digits. |
| **Reports** | Monthly and custom date-range reports on screen: total sales, sales by product and by payment method, credit given, payments received, the total owed, and a sales table. Each report exports as an Arabic right-to-left PDF to share or print. |
| **Settings** | Export all data to one file, import it back with validation, and switch between Arabic (default) and English. |

## How the numbers work

- Amounts are whole Sudanese pounds stored as `Long`, shown with Latin digits (`1,500`). Dates are stored as epoch milliseconds.
- A sale's amount is the credit value sold. It reduces stock one-to-one, so there is no cost price or profit.
- **Customer balance** = pay-later sales − payments.
- **Settlement.** A payment settles the customer's oldest pay-later sales first (FIFO). This decides whether each sale is paid, partly paid or unpaid.
- **Stock** = the current month's opening stock, plus additions, minus deductions, minus sales of that product since then.
- **Removing a product** only archives it. Past sales keep the product name they were made with, so old sales and reports stay correct. The default products are created once when the database is first created, so a deleted default does not come back.
- **Deleting a customer** removes only the customer. Their past sales and payments stay in history and reports, labelled as a deleted customer.

## Data storage

Everything is stored on the phone in a Room (SQLite) database. DataStore holds only the language choice. Nothing leaves the device unless you export a backup or share a PDF.

| Table | Fields |
|---|---|
| `Product` | id (stable: `zain`, `sudani`, `mtn`, or a UUID), name, sortOrder, archived, createdAt |
| `Customer` | id, name, phone?, createdAt |
| `Sale` | id, productId, productName (snapshot), amount, customerId?, createdAt, method (`CASH` / `BANKAK` / `CREDIT`) |
| `Payment` | id, customerId, amount, paidAt, method (`CASH` / `BANKAK`), note? |
| `StockEntry` | id, productId, kind (`OPENING` / `ADD` / `DEDUCT`), amount, reason, createdAt |

There are no cascading foreign keys. That is why removing a product or customer cannot corrupt old records.

## Backup and restore

- **Export** writes one JSON file containing all five tables (including archived products). It has a header with `format` (`raseed-backup`), `schemaVersion`, `exportedAt`, a row count per table, and a SHA-256 checksum of the data. You save it with the system file picker or share it.
- **Import** reads the file and validates it **before touching any data**. It rejects the file if any of these fail:
  - it is not valid JSON, or has the wrong format name;
  - it was written by a newer app version;
  - the checksum or the row counts do not match;
  - an id is duplicated;
  - a sale or stock entry points to a missing product;
  - an amount is out of range, a method or kind is not recognised, or a pay-later sale has no customer.
- A valid file is shown for confirmation, because import **replaces all current data**. The replacement runs in a single database transaction, so a failure rolls back and leaves the phone's data as it was.
- To move to a new phone: export on the old one, copy the file across, and import it on the new one.

## PDF reports

Reports are drawn with Android's built-in `PdfDocument` and `StaticLayout`. This gives correct Arabic letter shaping and right-to-left text without a PDF library. Pages are A4 and can run to several pages. Sharing goes through a `FileProvider`, and printing uses `PrintManager`.

The PDF uses the Arabic font installed on the phone. No font file is bundled. To use a specific font, add a `.ttf` to `res/font` and use it in `pdf/ReportPdf.kt`.

## Language and layout

Arabic is the default and the layout is right-to-left. English is available in Settings, and the layout direction follows the chosen language. Strings are in `res/values/` (Arabic) and `res/values-en/` (English). The app is light-themed in every system setting.

## Tech stack

- Kotlin 2.4, Jetpack Compose with Material 3 (Compose BOM 2026.09.00)
- Room 2.8 with KSP, for the SQLite database
- DataStore Preferences, for the language setting
- Navigation Compose and Lifecycle ViewModel, with a bottom navigation bar
- kotlinx.serialization, for the backup file
- Android Gradle Plugin 9.4, `minSdk` 24 (Android 7), `targetSdk` 36, Java 17

There is no image, network or PDF library. Versions are pinned in `android/gradle/libs.versions.toml`.

## Project structure

```
android/app/src/main/java/app/injaz/starter/
├── MainActivity.kt, RaseedApp.kt     entry point and application class
├── data/                             Room entities, DAOs, database, repository, settings store
├── domain/                           pure logic with no Android or Room types
│   ├── Balances.kt                   customer balance and FIFO settlement
│   ├── Inventory.kt                  stock calculation
│   ├── DateRanges.kt                 month and range boundaries
│   ├── ReportBuilder.kt              report totals and breakdowns
│   ├── Backup.kt                     backup format, checksum and validation
│   ├── Search.kt                     normalisation and matching
│   └── Money.kt, Validation.kt       formatting and input checks
├── pdf/                              ReportPdf.kt (PDF drawing), PrintAdapter.kt
└── ui/
    ├── AppNav.kt                     tabs and routes
    ├── theme/, kit/, common/         theme, shared components, helpers
    └── home/ sales/ customers/ products/ reports/ settings/ more/ search/
                                      one folder per screen, each with its ViewModel
android/app/src/test/java/.../domain/ unit tests for the domain logic
```

## Build and run

Requirements: Android Studio with JDK 17 and the Android SDK (compile SDK 37).

1. Open the **`android/`** folder in Android Studio and let Gradle sync.
2. Select a device or emulator running Android 7.0 (API 24) or newer, then press **Run**.
3. Run the unit tests with `./gradlew testDebugUnitTest` from `android/`, or from Android Studio's test runner.
4. For a build to install or publish, use **Build → Generate Signed App Bundle / APK** in Android Studio and sign it with your own keystore. The project has no signing configuration and does not contain keys. Keep your keystore and its passwords outside the repository.

The unit tests cover balances and settlement, stock, date ranges, money formatting and validation, report totals, backup validation and search.

## Scope and limits

- Single device, single user. There are no accounts, no sync between devices and no data shared between people. A backup file is the way to move data.
- **Bankak** is only a label that the seller records by hand. There is no payment integration.
- Not included: several currencies, purchase prices and profit, notifications, and multiple users.
- The PDF uses the phone's own Arabic font. How it looks depends on the fonts installed on the phone.

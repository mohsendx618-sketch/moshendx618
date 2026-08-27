# Hamyar CafeNet — همیار کافی‌نت

An installable native Persian Android handbook for café-net operators. Version 1.1
bundles 61 text guides (37 new tax guides), full-text Persian search, categories, favorites, recent
guides and per-guide checklists. No website hosting, account or first-run download
is needed.

## Boundaries and provenance

- Training text is bundled in app/src/main/assets/guides.json and taxes.json.
- The tax shortcut opens 37 guides: business groups, partnerships, Article 100,
  companies, VAT, rental, inheritance, incidental income, liquidation, payroll,
  quarterly reports, electronic books, invoices, notices and follow-up services.
- Official sources, review date, source scope and per-guide caveats are shown in
  the app. Portal entry points are not presented as tested live transactions.
- The route label marks general procedural checklists whose live form could not
  be inspected. The official label marks published official instructions, not an
  authenticated end-to-end transaction test. No customer payments were tested.
- Portals can change. Prices, processing times and universal acceptance of
  certificates are intentionally not promised.
- Training is offline; performing online services still needs Internet.
  Desktop tools referenced in the guides are not bundled into the Android app.
- This is an independent handbook, not an official government application.
- All tax guides are general operational checklists marked route. Current live
  tax forms were inaccessible; archival manuals and reprints are disclosed.
  These are not verified current click-by-click instructions, individualized
  advice, or a complete account of every exception. Rates, thresholds, exemptions
  and deadlines must be checked against the applicable official rules by a
  qualified accountant or tax adviser. Never fabricate data to bypass a form.

## Privacy and security

- No Internet, SMS, contacts or storage permissions are requested by app source.
- No WebView, embedded login, remote content, advertising or telemetry.
- Customer credentials, documents and card data are not collected.
- Only guide IDs, favorites, ticks and font preference are persisted locally.
  Search text can survive activity recreation but is not written to persistent
  preferences; users should search topics, never customer information.
- Cloud backups and device-transfer extraction are explicitly disabled.
  Fixed external HTTPS links require confirmation and open
  in the user's browser. Their hosts are allowlisted.
- Starting a new customer resets only the current guide's ticks, on confirmation.

## Build

JDK 17, Gradle 8.13, Android SDK 36 and Android Gradle Plugin 8.13.2:

    python3 cafenet-android/scripts/validate_content.py
    gradle -p cafenet-android testDebugUnitTest lintDebug assembleDebug --no-daemon

The workflow verifies the APK signature and manifest and runs functional offline
tests on an Android 16 emulator. Min SDK is 26 (Android 8). CI uses its default
debug signing certificate with debugging disabled. The tested APK is locally
re-signed for delivery with a stable private release key, then its signature and
unchanged payload are verified. The key and password are never committed or
sent to public CI. This is a sideloaded application, not a Play Store release.

The ephemeral CI key used for version 1.0 is unavailable, so version 1.1 cannot
update that installation in place. The user must first uninstall version 1.0,
which loses favorites and checklist marks. No automatic uninstall is performed.
Future releases must reuse the private release key to support in-place updates.

## Tests

- Unit tests cover Persian normalization, joined/spaced words, aliases, ranking,
  multi-word matching, contextual result tabs, tax terminology and URL policy.
- Content checks cover guide count, IDs, section detail and source references;
  they do not certify the current portal UI.
- Emulator: network disabled before launch; search, navigation, troubleshooting,
  source notice, favorites, persisted checklist, reset, empty state, filters,
  the tax hub, VAT troubleshooting, and major tax-return search shortcuts.

Changes are isolated on branch codex/cafenet-offline-guide. The previous
monitoring app and the default branch are not modified.

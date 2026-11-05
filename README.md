# Moje Naprawy — Android (Java)

Natywna aplikacja Android, dane lokalnie w SQLite, bez internetu, konta i reklam.

## Funkcje
- Kalendarz miesięczny z wyróżnieniem dni z naprawami
- Klienci (nazwa i telefon)
- Dodawanie, edycja i usuwanie napraw
- Data, godzina rozpoczęcia, czas w minutach, opis, urządzenie
- Cena ręczna w PLN
- Status zapłacone/nieopłacone z szybkim przełączaniem
- Podsumowanie miesiąca: suma, zapłacone, zaległe

## Budowanie APK
1. Otwórz katalog `MojeNaprawy` w Android Studio (JDK 17, Android SDK 35).
2. Poczekaj na synchronizację Gradle. Projekt korzysta z Android Gradle Plugin 8.7.3.
3. Wybierz Build > Build App Bundle(s) / APK(s) > Build APK(s).
4. Plik pojawi się w `app/build/outputs/apk/debug/app-debug.apk`.
5. Przenieś APK na telefon i zainstaluj (może być potrzebne zezwolenie na instalowanie aplikacji z tego źródła).

## Uwagi
- To pierwsza wersja projektu źródłowego. Nie jest dołączony gotowy plik APK.
- Baza danych znajduje się w prywatnym katalogu aplikacji. Odinstalowanie aplikacji może usunąć dane; przed użyciem do ważnych zapisów warto dodać eksport/import kopii zapasowej.
- Brak zewnętrznych zależności aplikacji.


## Wersja 1.2 — części do napraw
W widoku dnia przy każdej naprawie przycisk „Lista części”. Można dodać, edytować i usunąć część, wpisać kwotę należną od klienta oraz niezależnie oznaczyć płatność. Statusy części są uwzględnione w kolorze dnia. Migracja bazy SQLite 1→2 zachowuje klientów i naprawy.

## Wersja 1.3 — kopie zapasowe
W kalendarzu wybierz **Kopia zapasowa — eksport / import**. Eksport tworzy plik JSON zawierający klientów, naprawy, części oraz statusy płatności. Import zastępuje zawartość lokalnej bazy; najpierw sprawdza format i spójność referencji, a zapis wykonuje w transakcji SQLite. Używa systemowego wyboru pliku Androida (SAF), nie wymaga uprawnień do pamięci. Plik zawiera dane klientów w postaci niezaszyfrowanej — przechowuj go prywatnie.


## Wersja 1.4 — przycisk Wstecz i ikona
Przycisk systemowy Wstecz z ekranu części wraca do kalendarza z listą napraw, z listy klientów do kalendarza. Na ekranie głównym pokazuje okno potwierdzenia zamknięcia. Nowa ikona aplikacji znajduje się w `res/mipmap-*`. Zwiększono `versionCode` do 5.

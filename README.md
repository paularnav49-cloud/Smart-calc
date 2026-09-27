# SmartCalc

All-in-one Android calculator app (Kotlin):

- Basic calculator (+ - x / % parentheses)
- Percentage calculator (X% of Y, X is what % of Y, % change)
- Currency converter (live rates from open.er-api.com)
- Unit converter (length, weight, volume, temperature)
- HCF / LCM calculator (comma-separated numbers)
- Voice calculator (speak a calculation, e.g. "25 plus 17 into 3")
- AI chat assistant powered by Groq (llama-3.3-70b-versatile)

## Local build

1. Open in Android Studio (or run gradle).
2. Create/append to local.properties:
   groqApiKey=YOUR_GROQ_API_KEY
3. Build: gradle assembleDebug

## APK via GitHub Actions

1. Push to main (or run the workflow manually).
2. Add a repository secret named GROQ_API_KEY (Settings > Secrets and variables > Actions).
3. When the run finishes, download the SmartCalc-debug-apk artifact from the run page.

The voice calculator uses Android's built-in speech recognition (Google app must be installed).

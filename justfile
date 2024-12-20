build: gomobile
    ./gradlew installDebug

gomobile:
    #!/usr/bin/env fish
    set gofile (ls -t (ag -l --go backend) backend/go.mod | head -n 1)
    if test "$gofile" -nt app/libs/backend.aar
        echo "binding gomobile..."
        cd backend
        gomobile bind -tags=debug -androidapi 26 -target=android -o ../app/libs/backend.aar
    end

build_release: gomobile
    ./gradlew assembleRelease
    ./gradlew installRelease

emulator: gomobile
    ./gradlew assembleDebug
    adb install app/build/outputs/apk/debug/app-arm64-v8a-debug.apk

emulator_release: gomobile
    ./gradlew assembleRelease
    adb install app/build/outputs/apk/release/app-arm64-v8a-release.apk

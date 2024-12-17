build: gomobile
    ./gradlew installDebug

gomobile:
    #!/usr/bin/env fish
    set gofile (ls -t (ag -l --go backend) | head -n 1)
    if test "$gofile" -nt app/libs/backend.aar
        echo "binding gomobile..."
        cd backend
        gomobile bind -androidapi 26 -target=android -o ../app/libs/backend.aar
    end

build_release: gomobile
    ./gradlew assembleRelease
    ./gradlew installRelease
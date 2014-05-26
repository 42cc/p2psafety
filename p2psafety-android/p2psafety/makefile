all:
	((~/gradle-1.9/bin/gradle clean test && \
	~/gradle-1.9/bin/gradle clean assembleDebug && \
	echo "Debug apk file is in attachments." | mutt -s "[barista][p2psafety] New apk file created" praca.taras@gmail.com -a build/apk/p2psafety-defaultFlavor-debug-unaligned.apk) || \
	((zip -9 -r test-results.zip build/test-report/ && \
    echo "Test results are in attachments." | mutt -s "[barista][aw-android] New test results" praca.taras@gmail.com -a test-results.zip && \
    rm -f test-results.zip) && should_generate_error)) && \
    cd /tmp && \
    rm -r android-*/

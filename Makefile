build:
	mvn clean compile

test:
	mvn test

run:
	mvn clean compile exec:java -Dexec.mainClass=progr3.mail.client.app.Launcher

clean:
	mvn clean

.PHONY: build test run clean

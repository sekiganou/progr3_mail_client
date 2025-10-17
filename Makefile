NUM_INSTANCES ?= 2
MAIN_CLASS = progr3.mail.client.app.Launcher

build:
	mvn clean compile

test: build
	mvn test

run: build
	@echo "Starting $(MAIN_CLASS)..."
	@mkdir -p logs
	mvn exec:java -Dexec.mainClass=$(MAIN_CLASS) \
		> logs/app.log 2>&1 &
	@echo "$(MAIN_CLASS) started. Logs are being written to logs/app.log"

run-multi: build
	@echo "Starting $(NUM_INSTANCES) instances of $(MAIN_CLASS)..."
	@mkdir -p logs
	@for i in $$(seq 1 $(NUM_INSTANCES)); do \
		echo "Starting instance $$i..."; \
		mvn exec:java -Dexec.mainClass=$(MAIN_CLASS) \
			> logs/instance_$$i.log 2>&1 & \
	done; \
	exit 0
	@echo "All $(NUM_INSTANCES) instances started."

clean:
	mvn clean
	rm -rf logs

.PHONY: build test run run-multi clean

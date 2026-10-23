.PHONY: clean test run
.DEFAULT_GOAL := help

VERSION := '0.1.0'

help: ## List the make targets
	@printf 'Usage: make \033[36m<target>\033[0m\n\n'
	@awk 'BEGIN{FS=":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

clean: # Clean solution
	./gradlew clean

build: clean # Build the plugin
	./gradlew buildPlugin

test: clean # Test the plugin wihth gradle
	./gradlew test

run: # Run in a sandbox IDE with the plugin
	./gradlew runIde

clean-run: # Clean & Run
	./gradlew clean runIde

package:
	./gradlew buildPlugin -Pversion=$(VERSION)

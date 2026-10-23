.PHONY: clean test run
.DEFAULT_GOAL := help

help: ## List the make targets
	@printf 'Usage: make \033[36m<target>\033[0m\n\n'
	@awk 'BEGIN{FS=":.*## "} /^[a-zA-Z_-]+:.*## / {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

clean: # Clean solution
	./gradlew clean

test: clean # Test the plugin wihth gradle
	./gradlew test buildPlugin

run: # Run in a sandbox IDE with the plugin
	./gradlew runIde

clean-run: clean # Clean & Run
	./gradlew runIde

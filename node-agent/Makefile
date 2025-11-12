
VERSION ?= $(shell git describe --tags --always --dirty 2>/dev/null || echo v0.0.0)
OUT ?= dist

.PHONY: build deploy clean checksums

build:
	@OUT=$(OUT) VERSION=$(VERSION) scripts/build.sh $(VERSION)

checksums:
	@cd $(OUT) && rm -f SHA256SUMS && shasum -a 256 * > SHA256SUMS && echo "Wrote $(OUT)/SHA256SUMS"

deploy:
	scripts/deploy.sh

clean:
	rm -rf $(OUT)

PATH := node_modules/.bin:$(PATH)

node_modules/.bin/lessc:
	npm install

node_modules/.bin/less-watch-compiler:
	npm install

less: node_modules/.bin/less-watch-compiler
	less-watch-compiler less/ resources/public/css/ site.less

less-prod: node_modules/.bin/lessc
	lessc -x less/site.less resources/public/css/site.css

.PHONY: less less-prod

node_modules/.bin/lessc:
	npm install

node_modules/.bin/chokidar:
	npm install

less: node_modules/.bin/chokidar
	npx chokidar "less/**/*.less" -c "npx lessc less/site.less resources/public/css/site.css" --initial

less-prod: node_modules/.bin/lessc
	npx lessc -x less/site.less resources/public/css/site.css

.PHONY: less less-prod

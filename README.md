

## Configuration

To setup a new job just create a new folder under the `./data/jobs` folder, for instance `./data/jobs/my-job`. And then create a file named `job.json` with the configuration of the modules.

### Crawler module
This is the controller module, 

#### Priorities
Priorities are used to make the crawler to process some urls first than others, the bigger the number the faster it will be processed. Everytime a new url is detected, the crawler will evaluate the url filter of the defined priorities, the first match will assign the priority value, so the order is important here. If none of the priority filters evaluates to true, then the priority will be set to zero.

```json
"crawler": {
    "threadCount": 10, /*Number of concurrent requests*/
    "seeds": ["https://www.google.com/search?q=java"], /*the array of seeds, this will not be fitered*/
    "urlFilter" : "startsWith(host, 'www.google.')", /*A filter expression*/
    "priorities": [
       {"urlFilter": "matches(path, '^\/(MCO|mco)-[0-9]+.*')", "priority": 100}
    ]
}
```

### RocketScrape module
This module modifies the urls to use the RocketStcrape service

```json
"rocketScrape": {
    "apiKey": "xxxx-xxxxx-xxxx", /*Your rocketscrape url*/
    "urlFilter": "matches(protocol, '^https?$')" /*The expression used to filter urls that will be modified to use rocketscrape*/
}

```

### Task Killer Module
This module will kill the crawiling process after a condition is met. The conditions are based in the `urlFilter` configuration. When the filter returns a `false` result, it means that the page is *rejected*. If the result is `true` then the page is *accepted*.


* Max Rejected Page Count: Counts how may pages were accepted in a row. If the page is *accepted* this counter gets cleared.
* Timeout: Uses the last time a was accepted to calculate a timeout. This value can accept sufixes like `1s`, `32m`, `1h15m`.

```json
/*Sample module config*/
  "taskKiller": {
    "timeout": "1h20m",
    "urlFilter": "matches(path, '^\/(MCO|mco)-[0-9]+.*')"
  }
```

## Runing

Use the following command

```sh
mvn spring-boot:run -Dspring-boot.run.arguments="--crawler.jobId=my-job"
```

## Resuming a Job


## Expressions 

To write expressions use the functions described here: (https://github.com/ridencww/expression-evaluator)

MATCHES function was added to check if a component matches with a regex.
Example:  `matches(protocol, '^https?$')`

isResource will check if the given string ends with a common resource extension (png, jpeg, pdf,..).
Example: `isResource(path)`


## Logging

Loging can be configured in the `./conf/logback.xml` some loggers are already registered, default configuration is reloaded every 30 seconds, so it can be modified while running.

## Known Bugs

After first start, if there is no urls to add in the first iteration, it would never finish

## To do
* Add a mechanism to kill the task if there is no running tasks and relevant urls.
* Create a page validation mechanism to refetch pages if they are invalid.
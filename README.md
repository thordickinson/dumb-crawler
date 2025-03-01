## Configuration

Create a `config.json` file with the name of the job to run under the folder `~/.apricoot/crawler/jobs/{jobId}`. 

For instance `~/.apricoot/crawler/jobs/meli/config.json`.

This is a sample configuration file. 

```json
{
  "seeds": [
    "https://motos.mercadolibre.com.co/"
  ],
  "idExtractorPattern": ".*\/(?<id>MCO-[0-9]{6,}).*",
  "threadCount": 4,
  "tagger": {
    "internal": "matches(host, 'motos?.mercadolibre.com.co')",
    "item": "matches(path, '^\/(MCO|mco)-[0-9]+.*')",
    "listing": "matches(host, 'motos\\.mercadolibre\\.com\\.co') and not isResource(path)"
  },
  "linkFilter": {
    "whitelist": ["internal"],
    "blacklist": ["other"]
  },
  "priorities": {
    "seed": 1000,
    "item": 900,
    "listing": 100,
    "other": 10
  },
  "renderer": {
    "proxify": ["all"]
  },
  "validationSelectors": {
    "item": "div#price"
  },
  "storage": {
    "includedTags": ["item"]
  },
  "taskKiller": {
    "timeout": "5m"
  }
}
```

### Tagging
The configuration file contains a `tagger` configuration, each property of this object will be used
to add tags to the url, these tags are used later to set priorities, select elements to be stored and
other configurations.

### Priorities
Since some pages need to be rendered first, the priority allows to process first the ones with higher
priorities.


* `idExtractorPattern`: Is a regex applied to the entire url to extract the id of the publication, instead of applying a
  has over the entire url. This is a standard java regex and must contain an extractor group called `id`. If the url, if
  the url does not match the pattern, then a hashing algorithm will be used to calculate the url's id. You can use a tool
  like [this](https://regex101.com/) to test your regex.
* `threadCount`: Indicates the max request to be run concurrently, bear in mind that `Rocetscrape` can limit the max
  amount of requests in a second, so a value of about 4 to 5 should be good.
* `tagger`: This is an important component, it adds tags to the urls according to the given [expression](#expression-engine).
  These tags are later used by other components, so make sure your urls are getting well tagged.



## Rocketscrape

To setup the rocketscrape renderer use the 



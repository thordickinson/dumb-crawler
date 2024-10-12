## Configuration

* Create a file with the name of the job to run under the folder `conf/jobs`. For instance `finacaraiz.json`.


* `idExtractorPattern`: Is a regex applied to the entire url to extract the id of the publication, instead of applying a 
has over the entire url. This is a standard java regex and must contain an extractor group called `id`. If the url, if
the url does not match the pattern, then a hashing algorithm will be used to calculate the url's id. You can use a tool
like [this](https://regex101.com/) to test your regex.
* `threadCount`: Indicates the max request to be run concurrently, bear in mind that `Rocetscrape` can limit the max 
amount of requests in a second, so a value of about 4 to 5 should be good.
* `tagger`: This is an important component, it adds tags to the urls according to the given [expression](#expression-engine).
These tags are later used by other components, so make sure your urls are getting well tagged.



## Expression Engine


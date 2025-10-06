# Dumb Crawler Configuration Guide

## Overview

Dumb Crawler is a configurable web crawler that uses a JSON configuration file to define crawling behavior. The configuration file should be placed at `~/.apricoot/crawler/{jobId}/config.json`.

For example: `~/.apricoot/crawler/eltiempo/config.json`

## Complete Configuration Reference

### Sample Configuration

```json
{
  "seeds": ["https://eltiempo.com/"],
  "idExtractorPattern": ".*-(?<id>[0-9]+)(#[0-9]*)?$",
  "threadCount": 5,
  "maxAttemptCount": 5,
  "tagger": {
    "internal": "matches(host, '(\\w+.)?eltiempo\\.com')",
    "article": "matches(path, '.*-(?<id>[0-9]+)(#[0-9]*)?$')",
    "resource": "matches(path, '.*\\.(jpeg|jpg|png|tiff|webm|wav|mp3|mp4|avi|gif)$')"
  },
  "linkFilter": {
    "whitelist": ["internal"],
    "blacklist": ["other", "resource"]
  },
  "priorities": {
    "seed": 1000,
    "article": 900,
    "other": 10
  },
  "renderer": {
    "proxify": ["all"]
  },
  "validationSelectors": {
    "article": "div.header__principal"
  },
  "storage": {
    "includedTags": ["article"],
    "maxFileSize": 50
  },
  "taskKiller": {
    "timeout": "5m"
  }
}
```

---

## Configuration Properties

### `seeds` (Required)
**Type:** Array of strings  
**Description:** Starting URLs for the crawler. The crawler begins by visiting these URLs and following links found on them.

**Example:**
```json
"seeds": [
  "https://example.com/",
  "https://example.com/category/"
]
```

---

### `idExtractorPattern` (Optional)
**Type:** String (Java regex pattern)  
**Default:** Uses MD5 hash of the URL  
**Description:** A regular expression pattern to extract a unique ID from URLs. The pattern **must** contain a named capture group called `id` using the syntax `(?<id>...)`. If a URL doesn't match the pattern, the crawler falls back to using an MD5 hash of the URL as the ID.

**Use Case:** Useful for extracting article IDs, product codes, or other unique identifiers from URLs to avoid duplicate processing.

**Example:**
```json
"idExtractorPattern": ".*-(?<id>[0-9]+)(#[0-9]*)?$"
```
This extracts numeric IDs from URLs like:
- `https://example.com/article-12345` → ID: `12345`
- `https://example.com/news-67890#comment` → ID: `67890`

**Testing:** Use [regex101.com](https://regex101.com/) to test your patterns.

---

### `threadCount` (Optional)
**Type:** Integer  
**Default:** `3`  
**Description:** Maximum number of concurrent HTTP requests. Controls how many pages are fetched simultaneously.

**Recommendations:**
- For simple HTTP requests: 5-10
- When using Rocketscrape or similar rendering services: 3-5 (to respect rate limits)
- Consider the target server's capacity and your bandwidth

**Example:**
```json
"threadCount": 5
```

---

### `maxAttemptCount` (Optional)
**Type:** Integer  
**Default:** `5`  
**Description:** Maximum number of retry attempts for failed requests before marking a URL as permanently failed.

**Example:**
```json
"maxAttemptCount": 3
```

---

### `tagger` (Required)
**Type:** Object (key-value pairs)  
**Description:** Defines rules for tagging URLs based on expressions. Tags are used throughout the system for filtering, prioritization, validation, and storage decisions.

**Important Notes:**
- Each key becomes a tag name
- Each value is an [expression](#expression-language) that evaluates to true/false
- URLs matching an expression receive that tag
- URLs that don't match any expression receive the `other` tag
- The `other` tag is reserved and cannot be defined in the tagger

**Example:**
```json
"tagger": {
  "internal": "matches(host, '(\\w+.)?example\\.com')",
  "article": "matches(path, '/articles/.*')",
  "listing": "matches(path, '/category/.*')",
  "resource": "isResource(path)"
}
```

**Common Patterns:**
```json
{
  "internal": "matches(host, 'yourdomain\\.com')",
  "article": "matches(path, '.*-[0-9]+$')",
  "image": "matches(path, '.*\\.(jpg|png|gif)$')",
  "api": "matches(path, '^/api/')"
}
```

---

### `linkFilter` (Optional)
**Type:** Object  
**Description:** Controls which URLs are crawled based on their tags.

**Properties:**
- **`whitelist`** (Array of strings): Only URLs with these tags will be crawled
- **`blacklist`** (Array of strings): URLs with these tags will be ignored
- **`allowByDefault`** (Boolean, default: `false`): If true, URLs not matching whitelist/blacklist are allowed

**Logic:**
1. If URL has a blacklisted tag → rejected
2. If URL has a whitelisted tag → accepted
3. Otherwise → use `allowByDefault` value

**Example:**
```json
"linkFilter": {
  "whitelist": ["internal", "article"],
  "blacklist": ["resource", "external"]
}
```

**Use Cases:**
- **Stay on domain:** `"whitelist": ["internal"]`
- **Avoid media files:** `"blacklist": ["resource"]`
- **Only specific content:** `"whitelist": ["article", "product"]`

---

### `priorities` (Optional)
**Type:** Object (tag → integer)  
**Default:** All priorities are `0`  
**Description:** Assigns priority values to tags. URLs are processed in order of priority (highest first). If a URL has multiple tags, the highest priority among them is used.

**Example:**
```json
"priorities": {
  "seed": 1000,
  "article": 900,
  "listing": 100,
  "other": 10
}
```

**Use Cases:**
- Prioritize seed URLs to start crawling quickly
- Process important content (articles, products) before auxiliary pages
- Deprioritize low-value pages (archives, tags)

---

### `renderer` (Optional)
**Type:** Object  
**Description:** Configuration for the content renderer. Currently supports proxifying requests through Rocketscrape.

**Properties:**
- **`proxify`** (Array of strings): Tags of URLs that should be rendered through Rocketscrape

**Example:**
```json
"renderer": {
  "proxify": ["all"]
}
```

**Rocketscrape Setup:**
1. Set the `ROCKETSCRAPE_API_KEY` environment variable
2. Configure which tags should use the renderer
3. The crawler will automatically route those requests through Rocketscrape

**Use Cases:**
- JavaScript-heavy sites that require browser rendering
- Sites with anti-bot protection
- Dynamic content loaded via AJAX

---

### `validationSelectors` (Optional)
**Type:** Object (tag → CSS selector)  
**Description:** Defines CSS selectors that must be present in the HTML for URLs with specific tags. If the selector is not found, the page is marked as invalid and a `CONTENT_VALIDATION_ERROR_{tag}` counter is incremented.

**Example:**
```json
"validationSelectors": {
  "article": "div.article-content",
  "product": "div.product-price"
}
```

**Use Cases:**
- Verify that article pages contain the expected content structure
- Detect when a page layout has changed
- Ensure product pages have pricing information
- Identify pages that failed to load correctly

---

### `storage` (Required)
**Type:** Object  
**Description:** Controls which crawled pages are saved to disk.

**Properties:**
- **`includedTags`** (Array of strings): Only URLs with these tags will be saved
- **`maxFileSize`** (Integer, default: `50`): Maximum size of each WARC file in megabytes

**Example:**
```json
"storage": {
  "includedTags": ["article", "product"],
  "maxFileSize": 100
}
```

**Storage Format:**
- Pages are stored in WARC (Web ARChive) format
- Files are located in `~/.apricoot/crawler/{jobId}/sessions/{sessionId}/crawl/`
- When a WARC file reaches `maxFileSize`, a new file is created
- The crawler maintains an index to track which URLs are in which files

**Use Cases:**
- Save only valuable content: `"includedTags": ["article"]`
- Ignore navigation pages: Don't include `listing` or `other` tags
- Control disk usage with `maxFileSize`

---

### `taskKiller` (Optional)
**Type:** Object  
**Description:** Defines conditions for automatically stopping the crawler.

**Properties:**
- **`timeout`** (String, default: `"10m"`): Stop crawling if no new pages are saved within this time period

**Time Format:**
- `s` = seconds
- `m` = minutes  
- `h` = hours
- Examples: `"30s"`, `"5m"`, `"2h"`

**Example:**
```json
"taskKiller": {
  "timeout": "10m"
}
```

**Use Cases:**
- Prevent infinite crawling when all content is exhausted
- Automatically stop when the crawler gets stuck
- Set time limits for crawling sessions

---

## Expression Language

The `tagger` configuration uses a custom expression language to evaluate URLs. Expressions have access to URL components and can use built-in functions.

### Available Variables

When evaluating expressions, the following variables are available:

| Variable | Type | Description | Example |
|----------|------|-------------|---------|
| `url` | String | Complete URL | `https://example.com/path?q=1` |
| `protocol` | String | URL scheme | `https` |
| `host` | String | Domain name | `example.com` |
| `path` | String | URL path | `/articles/123` |
| `port` | Number | Port number | `443` |
| `query` | String | Query string | `q=1&page=2` |
| `fragment` | String | URL fragment | `section-1` |
| `contentType` | String | MIME type (when available) | `text/html` |

### Built-in Functions

#### `matches(string, regex)`
Tests if a string matches a regular expression pattern.

**Parameters:**
- `string`: The text to test
- `regex`: Java regular expression pattern

**Returns:** Boolean

**Examples:**
```javascript
// Match domain
matches(host, 'example\\.com')

// Match path pattern
matches(path, '/articles/[0-9]+')

// Match with optional subdomain
matches(host, '(\\w+\\.)?example\\.com')

// Match file extensions
matches(path, '.*\\.(jpg|png|gif)$')
```

#### `isResource(path)`
Checks if the path appears to be a static resource file.

**Parameters:**
- `path`: URL path to check

**Returns:** Boolean

**Detected Extensions:**
`css`, `js`, `sass`, `less`, `ico`, `jpeg`, `jpg`, `png`, `webp`, `pdf`, `mpeg`, `mpg`, `mp3`, `mp4`, `avi`, `ogg`, `wav`, `iso`

**Example:**
```javascript
isResource(path)  // true for "/images/photo.jpg"
```

#### `extract(string, regex)`
Extracts a value from a string using a regex with a named capture group called `value`.

**Parameters:**
- `string`: The text to extract from
- `regex`: Pattern with `(?<value>...)` capture group

**Returns:** String or null

**Example:**
```javascript
extract(path, '/articles/(?<value>[0-9]+)')
// Returns "123" for path "/articles/123"
```

### Expression Operators

You can combine expressions using logical operators:

- **`and`**: Both conditions must be true
- **`or`**: At least one condition must be true
- **`not`**: Negates the condition

**Examples:**
```javascript
// Internal domain AND not a resource
matches(host, 'example\\.com') and not isResource(path)

// Article OR product page
matches(path, '/articles/.*') or matches(path, '/products/.*')

// Not an external domain
not matches(host, 'external\\.com')
```

### Common Expression Patterns

```javascript
// Internal pages only
"internal": "matches(host, '(www\\.)?yourdomain\\.com')"

// Articles with numeric IDs
"article": "matches(path, '/articles/[0-9]+')"

// Exclude resources
"content": "not isResource(path)"

// Specific sections
"news": "matches(path, '^/news/')"

// Multiple domains
"internal": "matches(host, '(domain1|domain2)\\.com')"

// Query parameter check
"search": "matches(query, 'q=')"

// Protocol check
"secure": "matches(protocol, 'https')"
```

---

## Session Management

### Directory Structure

```
~/.apricoot/crawler/{jobId}/
├── config.json                    # Configuration file
└── sessions/
    └── {sessionId}/               # Format: YYYYMMDD_HHMMSS
        ├── crawl/                 # WARC files
        │   ├── crawl_*.warc
        │   └── ...
        ├── file_index.db          # SQLite index
        ├── session.db             # Session state
        ├── logs/                  # Log files
        └── terminated.marker      # Completion marker
```

### Session Resumption

The crawler automatically resumes incomplete sessions:
- If no `terminated.marker` file exists, the session is resumed
- Otherwise, a new session is created with a new timestamp
- All state (visited URLs, counters, etc.) is preserved

### Counters and Statistics

The crawler tracks various metrics during execution:

| Counter | Description |
|---------|-------------|
| `PROCESSED_URLS` | Successfully crawled pages |
| `SAVED_PAGES` | Pages written to WARC files |
| `NEW_SAVED_PAGES` | Pages saved for the first time |
| `UPDATED_PAGES` | Pages re-crawled and updated |
| `UNSAVED_PAGES` | Pages not saved (filtered by tags) |
| `ALLOWED_LINKS` | Links that passed the filter |
| `IGNORED_LINKS` | Links rejected by the filter |
| `ERROR_*` | Various error types |
| `EXCEPTION_*` | Exception types encountered |
| `CONTENT_VALIDATION_ERROR_*` | Validation failures by tag |

---

## Complete Example Configurations

### Example 1: News Site Crawler

```json
{
  "seeds": ["https://news.example.com/"],
  "idExtractorPattern": ".*-(?<id>[0-9]+)$",
  "threadCount": 5,
  "tagger": {
    "internal": "matches(host, '(\\w+\\.)?news\\.example\\.com')",
    "article": "matches(path, '/articles/.*-[0-9]+$')",
    "category": "matches(path, '/category/.*')",
    "resource": "isResource(path)"
  },
  "linkFilter": {
    "whitelist": ["internal"],
    "blacklist": ["resource"]
  },
  "priorities": {
    "seed": 1000,
    "article": 900,
    "category": 500,
    "other": 100
  },
  "validationSelectors": {
    "article": "article.content"
  },
  "storage": {
    "includedTags": ["article"],
    "maxFileSize": 100
  },
  "taskKiller": {
    "timeout": "15m"
  }
}
```

### Example 2: E-commerce Product Crawler

```json
{
  "seeds": ["https://shop.example.com/products"],
  "idExtractorPattern": "/products/(?<id>[A-Z0-9-]+)",
  "threadCount": 3,
  "tagger": {
    "internal": "matches(host, 'shop\\.example\\.com')",
    "product": "matches(path, '/products/[A-Z0-9-]+')",
    "listing": "matches(path, '/category/.*')",
    "image": "matches(path, '.*\\.(jpg|png|webp)$')"
  },
  "linkFilter": {
    "whitelist": ["internal"],
    "blacklist": ["image"]
  },
  "priorities": {
    "seed": 1000,
    "product": 900,
    "listing": 700,
    "other": 100
  },
  "renderer": {
    "proxify": ["product"]
  },
  "validationSelectors": {
    "product": "div.product-price"
  },
  "storage": {
    "includedTags": ["product"],
    "maxFileSize": 50
  },
  "taskKiller": {
    "timeout": "30m"
  }
}
```

### Example 3: Documentation Site Crawler

```json
{
  "seeds": ["https://docs.example.com/"],
  "threadCount": 8,
  "tagger": {
    "internal": "matches(host, 'docs\\.example\\.com')",
    "doc": "matches(path, '/docs/.*')",
    "api": "matches(path, '/api/.*')"
  },
  "linkFilter": {
    "whitelist": ["internal"]
  },
  "priorities": {
    "seed": 1000,
    "doc": 800,
    "api": 800,
    "other": 500
  },
  "storage": {
    "includedTags": ["doc", "api"]
  },
  "taskKiller": {
    "timeout": "20m"
  }
}
```

---

## Rocketscrape Integration

To use Rocketscrape for JavaScript rendering:

1. **Set Environment Variable:**
   ```bash
   export ROCKETSCRAPE_API_KEY="your-api-key-here"
   ```

2. **Configure Renderer:**
   ```json
   "renderer": {
     "proxify": ["article", "product"]
   }
   ```

3. **Tag URLs to Render:**
   Ensure your tagger assigns the appropriate tags to URLs that need rendering.

**Note:** Rocketscrape may have rate limits. Adjust `threadCount` accordingly (typically 3-5 concurrent requests).

---

## Tips and Best Practices

### 1. Start Small
Begin with a limited seed set and test your configuration before scaling up.

### 2. Monitor Counters
Check the crawler logs to ensure:
- URLs are being tagged correctly
- The link filter is working as expected
- Pages are being saved (check `SAVED_PAGES` counter)

### 3. Use Validation Selectors
Add validation selectors for important content types to detect:
- Layout changes
- Failed page loads
- Anti-bot measures

### 4. Tune Priorities
Set priorities to ensure important content is crawled first:
- Seeds: 1000
- High-value content: 800-900
- Supporting pages: 500-700
- Low-value pages: 100-400

### 5. Control Storage
Only save what you need using `storage.includedTags` to:
- Reduce disk usage
- Speed up crawling
- Focus on valuable content

### 6. Set Appropriate Timeouts
Configure `taskKiller.timeout` based on:
- Site size
- Crawl speed
- Expected completion time

### 7. Test Regular Expressions
Always test your regex patterns:
- Use [regex101.com](https://regex101.com/)
- Select "Java" flavor
- Test with real URLs from your target site

### 8. Handle Rate Limits
If crawling a site with rate limits:
- Reduce `threadCount`
- Consider using Rocketscrape
- Add delays if needed

---

## Troubleshooting

### No Pages Being Saved
**Check:**
- `storage.includedTags` matches your tagger tags
- URLs are being tagged correctly (check logs)
- Validation selectors aren't too strict

### Crawler Stops Too Early
**Check:**
- `taskKiller.timeout` is long enough
- Link filter isn't too restrictive
- Seeds are correct

### Too Many URLs Being Crawled
**Check:**
- Link filter whitelist/blacklist
- Tagger expressions are specific enough
- Consider adding more blacklist tags

### Memory Issues
**Check:**
- Reduce `threadCount`
- Reduce `storage.maxFileSize`
- Check for memory leaks in logs

---

## Running the Crawler

```bash
# Set job ID
java -jar dumb-crawler.jar --job=myjob

# With Rocketscrape
export ROCKETSCRAPE_API_KEY="your-key"
java -jar dumb-crawler.jar --job=myjob
```

The crawler will:
1. Load configuration from `~/.apricoot/crawler/myjob/config.json`
2. Resume existing session or create new one
3. Start crawling from seeds
4. Save results to `~/.apricoot/crawler/myjob/sessions/{sessionId}/`
5. Stop when timeout is reached or no more URLs to crawl 



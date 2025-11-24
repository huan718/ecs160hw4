# HW2 README


---
### 0. Assumptions

> - Redis database is pre-populated (from HW1)
> - We assume Redis contains:
Repository metadata (Url, CreatedAt, AuthorName)
Issue descriptions stored under keys like iss-curl-1, iss-curl-2, etc.

> - selected_repo.dat
```
curl
src/tool_operate.c
src/tool_urlglob.c
lib/file.c
lib/transfer.c
```
---
---

### 1. LLM Setup (Microservices A, B, C)

This program requires you to run a separate instance of deepcoder:1.5b via Ollama.

If the model is not installed, Part D will fail with:
- timeouts
- empty summaries
- or invalid JSON

To install the required model:

```bash
ollama pull deepcoder:1.5b
```
and start the server
```bash
ollama serve
```
---

### 2. Redis Setup


We assumeed redis is already setup on testing environment

---

### 3. Running the Program


```
# Run the script
bash script.sh
```

> Full execution may take **~15 minutes**

### 4. Viewing Results

The program prints:
1. Repo metadata from Redis

2. 20 issue summaries from Microservice A

3. Bug report JSON from Microservice B

4. Common issues (Microservice C)

```
=== Repo: curl ===
Issues: 20

--- Issue iss-curl-1 ---
LLM summary:
{"title": "...", "body":"..."}

=== Analyzing C Files ===
src/tool_operate.c -> [ ... JSON issues ... ]

=== Common Issues ===
{"commonIssues":[]}
```


---

### 5. Unit Tests

Unit tests validate statistics computation without invoking the GitHub API. Run tests with:

```bash
mvn test
```

---

### 6. GitHub Actions

- Every push triggers GitHub Actions to run unit tests automatically.  
- Ensures correctness of statistics computation while keeping API calls separate.

---


